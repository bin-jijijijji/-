import json
import sys

from .config import DB_CONFIG


def check_imports():
    import cv2  # noqa: F401
    import numpy  # noqa: F401
    import onnxruntime  # noqa: F401
    from insightface.app import FaceAnalysis  # noqa: F401


def check_db():
    import mysql.connector

    conn = mysql.connector.connect(use_pure=True, **DB_CONFIG)
    try:
        cur = conn.cursor()
        cur.execute("SELECT 1")
        cur.fetchone()
        cur.close()
    finally:
        conn.close()


def main():
    report = {"ok": True, "checks": {}}
    try:
        check_imports()
        report["checks"]["imports"] = "ok"
    except Exception as ex:
        report["ok"] = False
        report["checks"]["imports"] = f"failed: {ex}"

    try:
        check_db()
        report["checks"]["db"] = "ok"
    except Exception as ex:
        report["ok"] = False
        report["checks"]["db"] = f"failed: {ex}"

    print(json.dumps(report, ensure_ascii=False))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

