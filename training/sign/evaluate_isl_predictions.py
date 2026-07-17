"""Compute sign-recognition metrics from a prediction CSV.

Required columns:
    expected,predicted

Optional columns:
    top5       Pipe-separated ranked glosses, for example YES|NO|THANK YOU
    category   Dataset category used for per-category top-1 accuracy
"""

from __future__ import annotations

import argparse
import csv
from collections import Counter, defaultdict


def normalize(value: str) -> str:
    return " ".join(value.upper().strip().split())


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--predictions", required=True)
    args = parser.parse_args()

    total = 0
    correct = 0
    top5_total = 0
    top5_correct = 0
    by_category = defaultdict(Counter)

    with open(args.predictions, newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        required = {"expected", "predicted"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Prediction CSV is missing columns: {sorted(missing)}")

        for row in reader:
            expected = normalize(row["expected"])
            predicted = normalize(row["predicted"])
            category = row.get("category", "unknown") or "unknown"
            total += 1
            hit = expected == predicted
            correct += int(hit)
            by_category[category]["total"] += 1
            by_category[category]["correct"] += int(hit)

            ranked = [
                normalize(value)
                for value in row.get("top5", "").split("|")
                if value.strip()
            ][:5]
            if ranked:
                top5_total += 1
                top5_correct += int(expected in ranked)

    print(f"examples={total}")
    print(f"top1_accuracy={correct / total:.4f}" if total else "top1_accuracy=0.0000")
    if top5_total:
        print(f"top5_accuracy={top5_correct / top5_total:.4f}")
        print(f"top5_examples={top5_total}")
    for category, counts in sorted(by_category.items()):
        total_for_category = counts["total"]
        score = counts["correct"] / total_for_category if total_for_category else 0.0
        print(f"category_accuracy[{category}]={score:.4f}")


if __name__ == "__main__":
    main()
