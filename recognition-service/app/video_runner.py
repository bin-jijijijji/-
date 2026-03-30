import json
import os
import time
import uuid
from typing import Dict, Tuple, Any
from datetime import datetime

import cv2
import numpy as np
import requests

from .config import (
    BACKEND_EVENT_ENDPOINT,
    RECOGNITION_TOKEN,
    SNAPSHOT_DIR,
    TARGET_FPS,
    EVENT_COOLDOWN_SEC,
)
from .db import fetch_job, fetch_all_face_embeddings, update_job_status
from .face_engine import FaceEngine


def _roi_contains_bbox_center(roi: Dict[str, float], bbox_xyxy: list[float], frame_w: int, frame_h: int) -> bool:
    x1 = roi["x1"] * frame_w
    y1 = roi["y1"] * frame_h
    x2 = roi["x2"] * frame_w
    y2 = roi["y2"] * frame_h

    bx1, by1, bx2, by2 = bbox_xyxy
    cx = (bx1 + bx2) / 2.0
    cy = (by1 + by2) / 2.0
    return (cx >= x1 and cx <= x2 and cy >= y1 and cy <= y2)


def _safe_crop(frame_bgr: np.ndarray, bbox_xyxy: list[float], margin_ratio: float = 0.15):
    h, w = frame_bgr.shape[:2]
    x1, y1, x2, y2 = bbox_xyxy
    bw = x2 - x1
    bh = y2 - y1
    x1n = max(0, int(x1 - bw * margin_ratio))
    y1n = max(0, int(y1 - bh * margin_ratio))
    x2n = min(w, int(x2 + bw * margin_ratio))
    y2n = min(h, int(y2 + bh * margin_ratio))
    if x2n <= x1n or y2n <= y1n:
        return None
    return frame_bgr[y1n:y2n, x1n:x2n]


class VideoJobRunner:
    def __init__(self):
        self.face_engine = FaceEngine()

    def run(self, job_id: int):
        os.makedirs(SNAPSHOT_DIR, exist_ok=True)
        job = fetch_job(job_id)
        if not job:
            update_job_status(job_id, "FAILED", finished_at=datetime.now())
            return

        video_path = job["file_path"]
        threshold = float(job["threshold"])

        roi = {"x1": float(job["x1"]), "y1": float(job["y1"]), "x2": float(job["x2"]), "y2": float(job["y2"])}

        update_job_status(job_id, "RUNNING", started_at=datetime.now())

        # load all embeddings once per job
        face_entries = fetch_all_face_embeddings()
        if not face_entries:
            update_job_status(job_id, "FAILED", finished_at=datetime.now())
            return

        embedding_matrix = []
        metas = []
        for e in face_entries:
            emb = np.array(e["embedding"], dtype=np.float32)
            if emb.shape[0] != 512:
                continue
            # normalize just in case
            norm = np.linalg.norm(emb) + 1e-6
            emb = emb / norm
            embedding_matrix.append(emb)
            metas.append({
                "identityId": e["identityId"],
                "name": e["name"],
                "listType": e["listType"],
            })

        if not embedding_matrix:
            update_job_status(job_id, "FAILED", finished_at=datetime.now())
            return

        embedding_matrix = np.stack(embedding_matrix, axis=0)  # (N,512)

        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            update_job_status(job_id, "FAILED", finished_at=time.strftime("%Y-%m-%d %H:%M:%S"))
            return

        video_fps = cap.get(cv2.CAP_PROP_FPS)
        if video_fps is None or video_fps <= 0:
            video_fps = 25.0
        frame_skip = max(1, int(round(video_fps / max(0.1, TARGET_FPS))))

        latest_job_status = None
        cooldown: Dict[Tuple[int, str], float] = {}
        frame_index = 0
        processed = 0

        try:
            while True:
                ret, frame_bgr = cap.read()
                if not ret:
                    break

                if frame_index % frame_skip != 0:
                    frame_index += 1
                    continue

                processed += 1

                # compute timestamp before matching
                timestamp_ms = int(cap.get(cv2.CAP_PROP_POS_MSEC) or 0)
                frame_h, frame_w = frame_bgr.shape[:2]

                faces = self.face_engine.detect_and_embed(frame_bgr)
                if faces:
                    for face in faces:
                        if not _roi_contains_bbox_center(roi, face.bbox_xyxy, frame_w, frame_h):
                            continue

                        q = face.embedding.astype(np.float32)
                        q = q / (np.linalg.norm(q) + 1e-6)
                        scores = embedding_matrix @ q  # cosine similarity
                        best_idx = int(np.argmax(scores))
                        best_score = float(scores[best_idx])

                        if best_score < threshold:
                            continue

                        meta = metas[best_idx]
                        identity_id = int(meta["identityId"])
                        list_type = meta["listType"]

                        event_type = "blacklist_match" if list_type == "blacklist" else "whitelist_match"
                        cooldown_key = (identity_id, event_type)
                        now = time.time()
                        if now - cooldown.get(cooldown_key, 0.0) < EVENT_COOLDOWN_SEC:
                            continue
                        cooldown[cooldown_key] = now

                        crop = _safe_crop(frame_bgr, face.bbox_xyxy)
                        snapshot_filename = None
                        if crop is not None and crop.size > 0:
                            snapshot_filename = f"job_{job_id}_face_{identity_id}_{uuid.uuid4().hex}.jpg"
                            snapshot_path = os.path.join(SNAPSHOT_DIR, snapshot_filename)
                            cv2.imwrite(snapshot_path, crop, [cv2.IMWRITE_JPEG_QUALITY, 85])

                        payload = {
                            "jobId": job_id,
                            "eventType": event_type,
                            "faceIdentityId": identity_id,
                            "matchedName": meta["name"],
                            "score": best_score,
                            "frameIndex": frame_index,
                            "timestampMs": timestamp_ms,
                            "snapshotPath": snapshot_filename
                        }

                        headers = {"Recognition-Token": RECOGNITION_TOKEN}
                        try:
                            requests.post(
                                BACKEND_EVENT_ENDPOINT,
                                headers=headers,
                                json=payload,
                                timeout=10
                            )
                        except Exception:
                            # if backend is down, still keep processing for thesis demo
                            pass

                frame_index += 1

        finally:
            cap.release()

        update_job_status(job_id, "FINISHED", finished_at=datetime.now())

