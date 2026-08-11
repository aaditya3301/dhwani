"""Evaluate videos with the same INCLUDE preprocessing used by the Android app."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import cv2
import mediapipe as mp
import numpy as np
import onnxruntime as ort


TARGET_FRAMES = 169
FEATURES = 134
POSE_POINTS = 25
HAND_POINTS = 21


def _landmarks_array(landmarks, count: int) -> np.ndarray:
    result = np.full((count, 2), np.nan, dtype=np.float32)
    if landmarks is None:
        return result
    for index, point in enumerate(landmarks.landmark[:count]):
        result[index] = (point.x, point.y)
    return result


def _assign_hands(hand_result, pose: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    empty = np.full((HAND_POINTS, 2), np.nan, dtype=np.float32)
    detected = list(hand_result.multi_hand_landmarks or [])
    if not detected:
        return empty.copy(), empty.copy()

    first = _landmarks_array(detected[0], HAND_POINTS)
    if len(detected) > 1:
        return first, _landmarks_array(detected[1], HAND_POINTS)

    if np.isnan(pose[[15, 16]]).any():
        return first, empty.copy()
    left_distance = np.square(pose[15] - first[0]).sum()
    right_distance = np.square(pose[16] - first[0]).sum()
    if right_distance < left_distance:
        return empty.copy(), first
    return first, empty.copy()


def extract_frames(video_path: Path) -> list[np.ndarray]:
    frames: list[np.ndarray] = []
    capture = cv2.VideoCapture(str(video_path))
    with mp.solutions.hands.Hands(
        max_num_hands=2,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    ) as hands, mp.solutions.pose.Pose(
        model_complexity=1,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    ) as pose_detector:
        while capture.isOpened():
            success, image = capture.read()
            if not success:
                break
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            hand_result = hands.process(image)
            pose_result = pose_detector.process(image)
            pose = _landmarks_array(pose_result.pose_landmarks, POSE_POINTS)
            left_hand, right_hand = _assign_hands(hand_result, pose)
            frames.append(np.concatenate((pose, left_hand, right_hand)).reshape(FEATURES))
    capture.release()
    return frames[:TARGET_FRAMES]


def prepare(frames: list[np.ndarray]) -> np.ndarray:
    if not frames:
        raise ValueError("No video frames were decoded")
    source = np.stack(frames).astype(np.float32)
    output = np.zeros((TARGET_FRAMES, FEATURES), dtype=np.float32)
    indexes = np.arange(source.shape[0])
    for feature in range(FEATURES):
        values = source[:, feature]
        valid = np.flatnonzero(np.isfinite(values))
        if valid.size:
            output[: source.shape[0], feature] = np.interp(indexes, valid, values[valid])
    output[:, 0::2] *= 1920.0
    output[:, 1::2] *= 1080.0
    return output[np.newaxis]


def temporal_views(frames: list[np.ndarray]) -> list[list[np.ndarray]]:
    if len(frames) < 9:
        return [frames]
    trim = min(max(len(frames) // 8, 1), len(frames) // 3)
    return [frames, frames[trim:], frames[:-trim]]


def densify(frames: list[np.ndarray], factor: int) -> list[np.ndarray]:
    if factor <= 1 or len(frames) < 2:
        return frames
    result: list[np.ndarray] = []
    for start, end in zip(frames, frames[1:]):
        result.append(start)
        finite = np.isfinite(start) & np.isfinite(end)
        for step in range(1, factor):
            between = np.full(FEATURES, np.nan, dtype=np.float32)
            fraction = step / factor
            between[finite] = start[finite] + (end[finite] - start[finite]) * fraction
            result.append(between)
    result.append(frames[-1])
    return result[:TARGET_FRAMES]


def softmax(logits: np.ndarray) -> np.ndarray:
    shifted = logits - logits.max()
    values = np.exp(shifted)
    return values / values.sum()


def evaluate_frames(
    name: str,
    frames: list[np.ndarray],
    session: ort.InferenceSession,
    labels: list[str],
) -> None:
    predictions = []
    for view in temporal_views(frames):
        logits = session.run(["logits"], {"landmarks": prepare(view)})[0][0]
        predictions.append(softmax(logits))

    averages = np.stack(predictions).mean(axis=0)
    winners = [int(values.argmax()) for values in predictions]
    vote_counts = {winner: winners.count(winner) for winner in set(winners)}
    winner = max(vote_counts, key=lambda index: (vote_counts[index], averages[index]))
    ranked = [winner] + [
        int(index)
        for index in np.argsort(averages)[::-1]
        if int(index) != winner
    ][:2]
    result = [(labels[index], float(averages[index])) for index in ranked]
    view_results = [
        (labels[int(values.argmax())], float(values.max())) for values in predictions
    ]
    agreement = vote_counts[winner] / len(predictions)
    hand_frames = sum(np.isfinite(frame[POSE_POINTS * 2 :]).any() for frame in frames)
    print(
        f"{name}: frames={len(frames)} hand_frames={hand_frames} "
        f"agreement={agreement:.3f} views={view_results} top={result}"
    )


def evaluate(
    video_path: Path,
    session: ort.InferenceSession,
    labels: list[str],
    simulate_phone: bool,
) -> None:
    frames = extract_frames(video_path)
    evaluate_frames(video_path.name, frames, session, labels)
    if simulate_phone:
        sparse = frames[::3]
        evaluate_frames(f"{video_path.name} sparse", sparse, session, labels)
        evaluate_frames(
            f"{video_path.name} sparse+dense",
            densify(sparse, factor=3),
            session,
            labels,
        )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("videos", nargs="+", type=Path)
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--labels", required=True, type=Path)
    parser.add_argument("--simulate-phone", action="store_true")
    args = parser.parse_args()

    labels = json.loads(args.labels.read_text(encoding="utf-8"))["display_labels"]
    session = ort.InferenceSession(str(args.model), providers=["CPUExecutionProvider"])
    for video in args.videos:
        evaluate(video, session, labels, args.simulate_phone)


if __name__ == "__main__":
    main()
