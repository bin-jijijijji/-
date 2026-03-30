import os
from dotenv import load_dotenv

load_dotenv()


def _get(name: str, default: str):
    return os.getenv(name, default)


DB_CONFIG = {
    "host": _get("DB_HOST", "127.0.0.1"),
    "port": int(_get("DB_PORT", "3306")),
    "user": _get("DB_USER", "root"),
    "password": _get("DB_PASSWORD", "123456"),
    "database": _get("DB_NAME", "intelligent_security"),
}

BACKEND_BASE_URL = _get("BACKEND_BASE_URL", "http://127.0.0.1:8080")
BACKEND_EVENT_ENDPOINT = os.getenv("BACKEND_EVENT_ENDPOINT", f"{BACKEND_BASE_URL}/api/recognition/events")

# 与后端 application.yml 的 app.recognition.service-token 保持一致
RECOGNITION_TOKEN = _get("RECOGNITION_TOKEN", "recognition-secret")

SNAPSHOT_DIR = _get("SNAPSHOT_DIR", "d:/intelligent_security_thesis/shared/snapshots")

# 视频处理
TARGET_FPS = float(_get("TARGET_FPS", "6"))
MIN_FACE_SCORE_THRESHOLD = float(_get("MIN_FACE_SCORE_THRESHOLD", "0.0"))
EVENT_COOLDOWN_SEC = float(_get("EVENT_COOLDOWN_SEC", "30"))

