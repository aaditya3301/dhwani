"""Train a Gemma 4 vision LoRA for isolated ISL gloss recognition.

The input is a CSV manifest with these columns:
    video,gloss,split,category

Video paths may be absolute or relative to the manifest. Split must be train,
validation, or test. Test rows are intentionally not loaded by this trainer.
"""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from torch.utils.data import Dataset
from trl import SFTConfig, SFTTrainer
from unsloth import FastVisionModel, is_bf16_supported
from unsloth.trainer import UnslothVisionDataCollator


SYSTEM_PROMPT = (
    "You are an Indian Sign Language recognizer. The images are ordered frames "
    "from one short isolated sign. Output only the ISL gloss in uppercase."
)
VALID_SPLITS = {"train", "validation", "test"}


@dataclass(frozen=True)
class ManifestRow:
    video: Path
    gloss: str
    split: str
    category: str


def sample_frames(
    video_path: Path,
    count: int = 8,
    max_side: int = 384,
) -> list[Image.Image]:
    cap = cv2.VideoCapture(str(video_path))
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    if frame_count <= 0:
        cap.release()
        raise ValueError(f"Could not read frames from {video_path}")

    frames: list[Image.Image] = []
    for frame_index in np.linspace(0, frame_count - 1, count).astype(int):
        cap.set(cv2.CAP_PROP_POS_FRAMES, int(frame_index))
        ok, frame = cap.read()
        if not ok:
            continue
        image = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        image.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
        frames.append(image)
    cap.release()

    if len(frames) != count:
        raise ValueError(
            f"Expected {count} sampled frames from {video_path}, got {len(frames)}"
        )
    return frames


def build_example(row: ManifestRow, frame_count: int, max_side: int) -> dict:
    content = [
        {"type": "image", "image": frame}
        for frame in sample_frames(row.video, frame_count, max_side)
    ]
    content.append({"type": "text", "text": SYSTEM_PROMPT})
    return {
        "messages": [
            {"role": "user", "content": content},
            {
                "role": "assistant",
                "content": [{"type": "text", "text": row.gloss}],
            },
        ],
    }


class IslManifestDataset(Dataset):
    """Decodes videos lazily so the full frame set is never held in RAM."""

    def __init__(self, rows: list[ManifestRow], frame_count: int, max_side: int):
        self.rows = rows
        self.frame_count = frame_count
        self.max_side = max_side

    def __len__(self) -> int:
        return len(self.rows)

    def __getitem__(self, index: int) -> dict:
        return build_example(self.rows[index], self.frame_count, self.max_side)


def read_manifest(path: Path) -> list[ManifestRow]:
    if not path.is_file():
        raise FileNotFoundError(f"Manifest not found: {path}")

    rows: list[ManifestRow] = []
    with path.open(newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        required = {"video", "gloss", "split"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Manifest is missing columns: {sorted(missing)}")

        for line_number, item in enumerate(reader, start=2):
            raw_video = item["video"].strip()
            gloss = " ".join(item["gloss"].upper().split())
            split = item["split"].strip().lower()
            category = item.get("category", "unknown").strip() or "unknown"
            video = Path(raw_video)
            if not video.is_absolute():
                video = (path.parent / video).resolve()

            if not raw_video or not gloss:
                raise ValueError(f"Blank video or gloss at manifest line {line_number}")
            if split not in VALID_SPLITS:
                raise ValueError(
                    f"Invalid split '{split}' at line {line_number}; use {sorted(VALID_SPLITS)}"
                )
            if not video.is_file():
                raise FileNotFoundError(f"Video at line {line_number} does not exist: {video}")
            rows.append(ManifestRow(video, gloss, split, category))

    if not rows:
        raise ValueError("Manifest contains no examples")
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--base-model", default="unsloth/gemma-4-E4B-it")
    parser.add_argument("--output-dir", default="isl_lora_out", type=Path)
    parser.add_argument("--epochs", default=3, type=int)
    parser.add_argument("--frames", default=8, type=int)
    parser.add_argument("--max-image-side", default=384, type=int)
    parser.add_argument("--learning-rate", default=8e-5, type=float)
    parser.add_argument("--lora-rank", default=16, type=int)
    parser.add_argument("--seed", default=42, type=int)
    parser.add_argument(
        "--freeze-vision",
        action="store_true",
        help="Train language/attention/MLP adapters only for a cheaper baseline.",
    )
    parser.add_argument(
        "--merge",
        action="store_true",
        help="Also write merged 16-bit weights; this needs substantial RAM/disk.",
    )
    args = parser.parse_args()

    rows = read_manifest(args.manifest.resolve())
    train_rows = [row for row in rows if row.split == "train"]
    validation_rows = [row for row in rows if row.split == "validation"]
    if not train_rows or not validation_rows:
        raise ValueError("Manifest must contain both train and validation rows")

    model, tokenizer = FastVisionModel.from_pretrained(
        args.base_model,
        load_in_4bit=True,
        use_gradient_checkpointing="unsloth",
    )
    model = FastVisionModel.get_peft_model(
        model,
        finetune_vision_layers=not args.freeze_vision,
        finetune_language_layers=True,
        finetune_attention_modules=True,
        finetune_mlp_modules=True,
        r=args.lora_rank,
        lora_alpha=args.lora_rank,
        lora_dropout=0.0,
        bias="none",
        random_state=args.seed,
    )

    train_dataset = IslManifestDataset(train_rows, args.frames, args.max_image_side)
    validation_dataset = IslManifestDataset(
        validation_rows,
        args.frames,
        args.max_image_side,
    )

    FastVisionModel.for_training(model)
    trainer = SFTTrainer(
        model=model,
        tokenizer=tokenizer,
        data_collator=UnslothVisionDataCollator(model, tokenizer),
        train_dataset=train_dataset,
        eval_dataset=validation_dataset,
        args=SFTConfig(
            per_device_train_batch_size=1,
            gradient_accumulation_steps=8,
            warmup_ratio=0.05,
            num_train_epochs=args.epochs,
            learning_rate=args.learning_rate,
            fp16=not is_bf16_supported(),
            bf16=is_bf16_supported(),
            logging_steps=10,
            eval_strategy="steps",
            eval_steps=100,
            save_strategy="steps",
            save_steps=100,
            save_total_limit=2,
            optim="adamw_8bit",
            weight_decay=0.01,
            lr_scheduler_type="linear",
            output_dir=str(args.output_dir),
            report_to="none",
            seed=args.seed,
            remove_unused_columns=False,
            dataset_text_field="",
            dataset_kwargs={"skip_prepare_dataset": True},
            dataloader_num_workers=0,
            max_length=4096,
        ),
    )
    trainer.train()

    adapter_dir = args.output_dir / "gemma-4-e4b-isl-lora"
    model.save_pretrained(str(adapter_dir))
    tokenizer.save_pretrained(str(adapter_dir))

    if args.merge:
        merged_dir = args.output_dir / "gemma-4-e4b-isl-merged"
        model.save_pretrained_merged(
            str(merged_dir),
            tokenizer,
            save_method="merged_16bit",
        )


if __name__ == "__main__":
    main()
