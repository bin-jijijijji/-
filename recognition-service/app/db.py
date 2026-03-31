import json
import threading
from typing import Optional

import mysql.connector
from mysql.connector import pooling

from .config import DB_CONFIG

_pool_lock = threading.Lock()
_pool: Optional[pooling.MySQLConnectionPool] = None


def get_conn():
    global _pool
    if _pool is None:
        with _pool_lock:
            if _pool is None:
                _pool = pooling.MySQLConnectionPool(
                    pool_name="thesis_recog_pool",
                    pool_size=5,
                    use_pure=True,
                    **DB_CONFIG,
                )
    return _pool.get_connection()


def fetch_job(job_id: int):
    sql = """
        SELECT
            j.id,
            j.threshold,
            j.status,
            v.file_path,
            r.x1, r.y1, r.x2, r.y2
        FROM recognition_jobs j
        JOIN video_assets v ON v.id = j.video_asset_id
        JOIN roi_rectangles r ON r.id = j.roi_rectangle_id
        WHERE j.id = %s
    """
    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql, (job_id,))
        row = cur.fetchone()
        cur.close()
        return row


def update_job_status(job_id: int, status: str, started_at=None, finished_at=None):
    if started_at is not None and finished_at is not None:
        sql = """
            UPDATE recognition_jobs
            SET status=%s, started_at=%s, finished_at=%s
            WHERE id=%s
        """
        params = (status, started_at, finished_at, job_id)
    elif started_at is not None:
        sql = """
            UPDATE recognition_jobs
            SET status=%s, started_at=%s
            WHERE id=%s
        """
        params = (status, started_at, job_id)
    elif finished_at is not None:
        sql = """
            UPDATE recognition_jobs
            SET status=%s, finished_at=%s
            WHERE id=%s
        """
        params = (status, finished_at, job_id)
    else:
        sql = """
            UPDATE recognition_jobs
            SET status=%s
            WHERE id=%s
        """
        params = (status, job_id)

    with get_conn() as conn:
        cur = conn.cursor()
        cur.execute(sql, params)
        conn.commit()
        cur.close()


def fetch_all_face_embeddings():
    """
    返回列表：
    [
      {"identityId":..., "name":..., "listType":"blacklist/whitelist", "embedding":[...512...]}
    ]
    """
    sql = """
        SELECT
            fi.id AS identity_id,
            fi.name AS identity_name,
            fi.list_type AS list_type,
            fe.embedding_vector AS embedding_vector_json
        FROM face_embeddings fe
        JOIN face_identities fi ON fi.id = fe.face_identity_id
        WHERE fe.embedding_vector IS NOT NULL
    """
    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql)
        rows = cur.fetchall()
        cur.close()

    result = []
    for r in rows:
        emb = None
        try:
            emb = json.loads(r["embedding_vector_json"])
        except Exception:
            pass
        if isinstance(emb, list) and len(emb) > 0:
            # insightface embedding typically is 512-d float array
            result.append({
                "identityId": r["identity_id"],
                "name": r["identity_name"],
                "listType": r["list_type"],
                "embedding": emb
            })
    return result


def ingest_identity_and_embeddings(name: str, list_type: str, image_paths: list[str], face_engine):
    """
    写入 face_identities / face_embeddings
    """
    with get_conn() as conn:
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO face_identities (name, list_type, created_at) VALUES (%s, %s, NOW())",
            (name, list_type)
        )
        face_identity_id = cur.lastrowid

        inserted = 0
        for p in image_paths:
            vec = face_engine.extract_embedding_from_image_path(p)
            if vec is None:
                continue
            # store as json array text
            vec_json = json.dumps(vec.tolist())
            cur.execute(
                "INSERT INTO face_embeddings (face_identity_id, embedding_vector, created_at) VALUES (%s, %s, NOW())",
                (face_identity_id, vec_json)
            )
            inserted += 1
        conn.commit()
        cur.close()
    return face_identity_id, inserted

