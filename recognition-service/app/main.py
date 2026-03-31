import logging
from typing import List, Optional

from fastapi import FastAPI, BackgroundTasks, Header, HTTPException
from pydantic import BaseModel

from .config import RECOGNITION_TOKEN
from .db import ingest_identity_and_embeddings
from .video_runner import VideoJobRunner

app = FastAPI(title="recognition-service")
logger = logging.getLogger(__name__)

_runner: Optional[VideoJobRunner] = None


def get_runner() -> VideoJobRunner:
    global _runner
    if _runner is None:
        _runner = VideoJobRunner()
    return _runner


class IdentityIngestModel(BaseModel):
    name: str
    listType: str  # whitelist / blacklist
    imagePaths: List[str]


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/api/identities/ingest")
def ingest_identity(
    payload: IdentityIngestModel,
    recognition_token: Optional[str] = Header(default=None, alias="Recognition-Token"),
):
    if recognition_token is None or recognition_token != RECOGNITION_TOKEN:
        raise HTTPException(status_code=403, detail="Recognition-Token invalid")

    safe_name = payload.name.strip()
    if safe_name == "":
        raise HTTPException(status_code=400, detail="name不能为空")

    safe_type = payload.listType.strip().lower()
    if safe_type not in ["whitelist", "blacklist"]:
        raise HTTPException(status_code=400, detail="listType必须是whitelist或blacklist")

    if not payload.imagePaths or len(payload.imagePaths) == 0:
        raise HTTPException(status_code=400, detail="imagePaths不能为空")

    runner = get_runner()
    face_identity_id, inserted = ingest_identity_and_embeddings(
        name=safe_name,
        list_type=safe_type,
        image_paths=payload.imagePaths,
        face_engine=runner.face_engine,
    )

    return {"ok": True, "faceIdentityId": face_identity_id, "inserted": inserted}


@app.post("/api/jobs/{job_id}/run")
def run_job(
    job_id: int,
    background_tasks: BackgroundTasks,
    recognition_token: Optional[str] = Header(default=None, alias="Recognition-Token"),
):
    if recognition_token is None or recognition_token != RECOGNITION_TOKEN:
        raise HTTPException(status_code=403, detail="Recognition-Token invalid")

    runner = get_runner()
    # 后台跑（避免HTTP超时）
    background_tasks.add_task(runner.run, job_id)
    return {"ok": True, "jobId": job_id}

