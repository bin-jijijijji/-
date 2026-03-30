import argparse
import time

import cv2

from .face_engine import FaceEngine


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", required=True)
    parser.add_argument("--frames", type=int, default=120)
    args = parser.parse_args()

    engine = FaceEngine()
    cap = cv2.VideoCapture(args.video)
    if not cap.isOpened():
        raise RuntimeError("无法打开视频：" + args.video)

    processed = 0
    t0 = time.time()

    while processed < args.frames:
        ret, frame = cap.read()
        if not ret:
            break
        ts = time.time()
        _ = engine.detect_and_embed(frame)
        processed += 1
        _ = time.time() - ts

    cap.release()
    total = time.time() - t0
    if processed == 0:
        print("No frames processed.")
        return

    fps = processed / total
    ms_per_frame = (total / processed) * 1000.0
    print(f"Processed={processed}, TotalSec={total:.3f}, FPS={fps:.2f}, ms/frame={ms_per_frame:.2f}")


if __name__ == "__main__":
    main()

