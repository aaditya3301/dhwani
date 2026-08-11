"""Convert AI4Bharat's official INCLUDE small Transformer to ONNX.

The checkpoint comes from https://github.com/AI4Bharat/INCLUDE and was trained
on 169 frames of MediaPipe pose and hand landmarks. This script converts the
published weights only; it does not train or alter them.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch
from torch import nn
from torch.nn import functional as functional


FRAMES = 169
FEATURES = 134
HIDDEN_SIZE = 256
CLASSES = 263

DISPLAY_LABELS = {
    "biglarge": "BIG/LARGE",
    "exmonsoon": "EX-MONSOON",
    "goodafternoon": "GOOD AFTERNOON",
    "goodevening": "GOOD EVENING",
    "goodmorning": "GOOD MORNING",
    "goodnight": "GOOD NIGHT",
    "howareyou": "HOW ARE YOU",
    "raceethnicity": "RACE/ETHNICITY",
    "secondnumber": "SECOND (NUMBER)",
    "smalllittle": "SMALL/LITTLE",
    "storeorshop": "STORE/SHOP",
    "streetorroad": "STREET/ROAD",
    "thankyou": "THANK YOU",
    "trainstation": "TRAIN STATION",
    "trainticket": "TRAIN TICKET",
    "tshirt": "T-SHIRT",
    "youplural": "YOU (PLURAL)",
}


class PositionEmbedding(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.position_embeddings = nn.Embedding(256, HIDDEN_SIZE)
        self.LayerNorm = nn.LayerNorm(HIDDEN_SIZE, eps=1e-12)
        self.dropout = nn.Dropout(0.1)
        self.register_buffer(
            "position_ids",
            torch.arange(256).expand((1, -1)),
        )

    def forward(self, landmarks: torch.Tensor) -> torch.Tensor:
        sequence_length = landmarks.size(1)
        positions = self.position_embeddings(self.position_ids[:, :sequence_length])
        return self.dropout(self.LayerNorm(landmarks + positions))


class BertSelfAttention(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.query = nn.Linear(HIDDEN_SIZE, HIDDEN_SIZE)
        self.key = nn.Linear(HIDDEN_SIZE, HIDDEN_SIZE)
        self.value = nn.Linear(HIDDEN_SIZE, HIDDEN_SIZE)
        self.dropout = nn.Dropout(0.1)

    def forward(self, hidden: torch.Tensor) -> torch.Tensor:
        batch_size, frames, _ = hidden.shape

        def split_heads(values: torch.Tensor) -> torch.Tensor:
            return values.reshape(batch_size, frames, 4, 64).permute(0, 2, 1, 3)

        query = split_heads(self.query(hidden))
        key = split_heads(self.key(hidden))
        value = split_heads(self.value(hidden))
        scores = torch.matmul(query, key.transpose(-1, -2)) / 8.0
        probabilities = self.dropout(torch.softmax(scores, dim=-1))
        context = torch.matmul(probabilities, value)
        return context.permute(0, 2, 1, 3).reshape(batch_size, frames, HIDDEN_SIZE)


class BertSelfOutput(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.dense = nn.Linear(HIDDEN_SIZE, HIDDEN_SIZE)
        self.LayerNorm = nn.LayerNorm(HIDDEN_SIZE, eps=1e-12)
        self.dropout = nn.Dropout(0.1)

    def forward(self, hidden: torch.Tensor, residual: torch.Tensor) -> torch.Tensor:
        return self.LayerNorm(self.dropout(self.dense(hidden)) + residual)


class BertAttention(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.self = BertSelfAttention()
        self.output = BertSelfOutput()

    def forward(self, hidden: torch.Tensor) -> torch.Tensor:
        return self.output(self.self(hidden), hidden)


class BertIntermediate(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.dense = nn.Linear(HIDDEN_SIZE, 3072)

    def forward(self, hidden: torch.Tensor) -> torch.Tensor:
        return functional.gelu(self.dense(hidden))


class BertOutput(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.dense = nn.Linear(3072, HIDDEN_SIZE)
        self.LayerNorm = nn.LayerNorm(HIDDEN_SIZE, eps=1e-12)
        self.dropout = nn.Dropout(0.1)

    def forward(self, hidden: torch.Tensor, residual: torch.Tensor) -> torch.Tensor:
        return self.LayerNorm(self.dropout(self.dense(hidden)) + residual)


class BertLayer(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.attention = BertAttention()
        self.intermediate = BertIntermediate()
        self.output = BertOutput()

    def forward(self, hidden: torch.Tensor) -> torch.Tensor:
        attention = self.attention(hidden)
        return self.output(self.intermediate(attention), attention)


class IncludeTransformer(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.l1 = nn.Linear(FEATURES, HIDDEN_SIZE)
        self.embedding = PositionEmbedding()
        self.layers = nn.ModuleList([BertLayer() for _ in range(2)])
        self.l2 = nn.Linear(HIDDEN_SIZE, CLASSES)

    def forward(self, landmarks: torch.Tensor) -> torch.Tensor:
        hidden = self.embedding(self.l1(landmarks))
        for layer in self.layers:
            hidden = layer(hidden)
        return self.l2(torch.max(hidden, dim=1).values)


def read_labels(path: Path) -> list[str]:
    label_to_index = json.loads(path.read_text(encoding="utf-8"))
    if len(label_to_index) != CLASSES:
        raise ValueError(f"Expected {CLASSES} INCLUDE labels")
    labels = [""] * CLASSES
    for label, index in label_to_index.items():
        labels[index] = DISPLAY_LABELS.get(label, label.upper())
    if any(not label for label in labels):
        raise ValueError("INCLUDE label map is not contiguous")
    return labels


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--label-map", type=Path, required=True)
    parser.add_argument("--output-model", type=Path, required=True)
    parser.add_argument("--output-labels", type=Path, required=True)
    args = parser.parse_args()

    checkpoint = torch.load(args.checkpoint, map_location="cpu", weights_only=False)
    model = IncludeTransformer()
    model.load_state_dict(checkpoint["model"], strict=True)
    model.eval()

    sample = torch.randn(1, FRAMES, FEATURES, dtype=torch.float32)
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
            "source": "AI4Bharat/INCLUDE small no-CNN Transformer checkpoint",
            "license": "MIT",
            "dataset": "INCLUDE (263 isolated Indian Sign Language signs)",
            "validation_score": str(checkpoint.get("score", "unknown")),
            "input": "1x169x134 MediaPipe pose and hand coordinates",
        },
    )
    onnx.save(onnx_model, args.output_model)

    with torch.no_grad():
        expected = model(sample).numpy()
    session = ort.InferenceSession(
        str(args.output_model),
        providers=["CPUExecutionProvider"],
    )
    actual = session.run(["logits"], {"landmarks": sample.numpy()})[0]
    np.testing.assert_allclose(actual, expected, rtol=1e-4, atol=2e-5)

    labels = read_labels(args.label_map)
    args.output_labels.write_text(
        json.dumps(
            {
                "display_labels": labels,
                "source": "AI4Bharat/INCLUDE label_map_include.json",
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"Validation score: {checkpoint.get('score', 'unknown')}")
    print(f"Wrote {args.output_model} ({args.output_model.stat().st_size} bytes)")
    print(f"Wrote {args.output_labels} ({len(labels)} labels)")


if __name__ == "__main__":
    main()
