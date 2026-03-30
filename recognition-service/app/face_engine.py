import logging
from dataclasses import dataclass
import cv2
import numpy as np

from insightface.app import FaceAnalysis

logger = logging.getLogger(__name__)


@dataclass
class FaceResult:
    bbox_xyxy: list[float]  # [x1,y1,x2,y2] in pixel coords
    embedding: np.ndarray  # normed 512-d vector


class FaceEngine:
    def __init__(self):
        # buffalo_sc: 轻量化人脸识别模型（512-d embedding）
        self.app = FaceAnalysis(name="buffalo_sc", allowed_modules=["detection", "recognition"])
        # ctx_id=-1 表示CPU
        self.app.prepare(ctx_id=-1, det_size=(640, 640))

    def _to_rgb(self, img_bgr: np.ndarray) -> np.ndarray:
        return cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

    def detect_and_embed(self, frame_bgr: np.ndarray) -> list[FaceResult]:
        frame_rgb = self._to_rgb(frame_bgr)
        faces = self.app.get(frame_rgb)
        results: list[FaceResult] = []
        if not faces:
            return results
        for f in faces:
            bbox = [float(f.bbox[0]), float(f.bbox[1]), float(f.bbox[2]), float(f.bbox[3])]
            emb = f.normed_embedding
            if emb is None:
                continue
            results.append(FaceResult(bbox_xyxy=bbox, embedding=np.array(emb, dtype=np.float32)))
        return results

    def extract_embedding_from_image_path(self, image_path: str):
        img = cv2.imread(image_path)
        if img is None:
            return None
        faces = self.detect_and_embed(img)
        if not faces:
            return None
        # pick the biggest face bbox
        faces.sort(key=lambda r: (r.bbox_xyxy[2]-r.bbox_xyxy[0]) * (r.bbox_xyxy[3]-r.bbox_xyxy[1]), reverse=True)
        return faces[0].embedding

