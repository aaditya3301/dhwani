"""Convert AI4Bharat OpenHands INCLUDE BiLSTM checkpoint to ONNX.

The released OpenHands model expects MediaPipe Holistic pose data reduced to
27 keypoints and normalized by the mean shoulder center/distance. This script
only converts published weights; it does not train or alter them.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch
from torch import nn


class AttentionBlock(nn.Module):
    def __init__(self, hidden_size: int) -> None:
        super().__init__()
        self.fc1 = nn.Linear(hidden_size, hidden_size, bias=False)
        self.fc2 = nn.Linear(hidden_size * 2, hidden_size, bias=False)

    def forward(self, hidden_states: torch.Tensor) -> torch.Tensor:
        score_first_part = self.fc1(hidden_states)
        h_t = hidden_states[:, -1, :]
        score = torch.bmm(score_first_part, h_t.unsqueeze(2)).squeeze(2)
        attention_weights = torch.softmax(score, dim=1)
        context_vector = torch.bmm(
            hidden_states.permute(0, 2, 1), attention_weights.unsqueeze(2)
        ).squeeze(2)
        return torch.tanh(self.fc2(torch.cat((context_vector, h_t), dim=1)))


class IncludeBiLstm(nn.Module):
    """Exact inference graph used by the released OpenHands checkpoint."""

    def __init__(self, num_classes: int) -> None:
        super().__init__()
        # OpenHands 0.1 used the PyTorch default batch_first=False while passing
        # B,T,F tensors. Preserve that behavior so this is a true conversion.
        self.rnn = nn.LSTM(
            input_size=54,
            hidden_size=128,
            num_layers=4,
            bidirectional=True,
        )
        self.attn_block = AttentionBlock(hidden_size=256)
        self.fc = nn.Linear(256, num_classes)

    def forward(self, landmarks: torch.Tensor) -> torch.Tensor:
        output, _ = self.rnn(landmarks)
        return self.fc(self.attn_block(output))


def read_labels(split_csv: Path) -> tuple[list[str], list[str]]:
    with split_csv.open(newline="", encoding="utf-8-sig") as handle:
        source_labels = sorted({row["Word"].strip() for row in csv.DictReader(handle)})
    display_labels = [
        re.sub(r"^\d+\.\s*", "", label).replace("_", " ").strip().upper()
        for label in source_labels
    ]
    if len(source_labels) != 263 or len(set(display_labels)) != 263:
        raise ValueError("Expected 263 unique INCLUDE labels")
    return source_labels, display_labels


def load_model(checkpoint_path: Path, num_classes: int) -> IncludeBiLstm:
    checkpoint = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
    state = {
        key.removeprefix("model.decoder."): value
        for key, value in checkpoint["state_dict"].items()
    }
    model = IncludeBiLstm(num_classes)
    model.load_state_dict(state, strict=True)
    model.eval()
    return model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--split-csv", type=Path, required=True)
    parser.add_argument("--output-model", type=Path, required=True)
    parser.add_argument("--output-labels", type=Path, required=True)
    args = parser.parse_args()

    source_labels, display_labels = read_labels(args.split_csv)
    model = load_model(args.checkpoint, len(source_labels))
    sample = torch.randn(1, 32, 54, dtype=torch.float32)

    args.output_model.parent.mkdir(parents=True, exist_ok=True)
    args.output_labels.parent.mkdir(parents=True, exist_ok=True)
    torch.onnx.export(
        model,
        sample,
        args.output_model,
        input_names=["landmarks"],
        output_names=["logits"],
        opset_version=17,
        do_constant_folding=True,
        dynamo=False,
    )

    onnx_model = onnx.load(args.output_model)
    onnx.checker.check_model(onnx_model)
    onnx.helper.set_model_props(
        onnx_model,
        {
            "source": "AI4Bharat/OpenHands include_lstm checkpoint",
            "license": "Apache-2.0",
            "dataset": "INCLUDE (263 isolated Indian Sign Language signs)",
            "input": "1x32x54 normalized x/y coordinates for 27 keypoints",
        },
    )
    onnx.save(onnx_model, args.output_model)

    with torch.no_grad():
        expected = model(sample).numpy()
    session = ort.InferenceSession(str(args.output_model), providers=["CPUExecutionProvider"])
    actual = session.run(["logits"], {"landmarks": sample.numpy()})[0]
    np.testing.assert_allclose(actual, expected, rtol=1e-4, atol=1e-5)

    args.output_labels.write_text(
        json.dumps(
            {"source_labels": source_labels, "display_labels": display_labels},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {args.output_model} ({args.output_model.stat().st_size} bytes)")
    print(f"Wrote {args.output_labels} ({len(display_labels)} labels)")


if __name__ == "__main__":
    main()
