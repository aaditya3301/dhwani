# DHWANI — Phase 3: Sign Language Input

> Phase 2 made the call intelligent. Phase 3 makes the input modality match how deaf users actually communicate — Indian Sign Language. Camera in, signs out, natural Hindi/English into the call.

This is the phase that produces the **publishable artifact**: a Gemma 4 vision adapter fine-tuned on ISL, with weights, benchmarks, and a Kaggle/HF model card.

---

## 1. Goal of this phase

At the end of Phase 3:

1. The user can point the phone's front camera at themselves and sign in ISL. A 2–4 second sign sequence is recognized, converted to Hindi or English natural language, fed into Phase 2's smart-reply pipeline, and ultimately spoken into the call via TTS.
2. A fine-tuned **Gemma 4 vision adapter (LoRA) on ISL** is published with weights + benchmarks. This is the "if training a model, publish your weights and benchmarks" deliverable from the hackathon rules.
3. Real-time inference of <1.5s from end-of-sign to start-of-TTS on a Pixel 8.

---

## 2. Architecture for Phase 3

```
                    ┌──────────────────────────────────────────────┐
                    │  Front camera (CameraX, 720p, 15fps)         │
                    └────────────────────┬─────────────────────────┘
                                         │
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  MediaPipe Hand Landmarker (pose + 21 hand   │
                    │  landmarks per frame × 2 hands)              │
                    └────────────────────┬─────────────────────────┘
                                         │ landmark sequence
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  Sign-segmenter: detect start/end of a sign  │
                    │  using hand motion energy thresholds         │
                    └────────────────────┬─────────────────────────┘
                                         │ segmented clip
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  Gemma 4 E4B + ISL-LoRA (LiteRT)             │
                    │  IN:  8 evenly-sampled frames + system       │
                    │       prompt asking for the gloss            │
                    │  OUT: ISL gloss tokens                       │
                    └────────────────────┬─────────────────────────┘
                                         │ gloss
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  Gemma 4 E4B (same model, different prompt)  │
                    │  IN:  ISL gloss + conversation context       │
                    │  OUT: natural Hindi/English sentence         │
                    └────────────────────┬─────────────────────────┘
                                         │
                                         ▼
                    ┌──────────────────────────────────────────────┐
                    │  Phase 2 smart-reply pipeline + TTS          │
                    └──────────────────────────────────────────────┘
```

Two passes through Gemma 4. First pass = vision → gloss (fine-tuned with LoRA). Second pass = gloss → natural sentence (zero-shot, uses base Gemma 4 + Phase 2 context).

---

## 3. The dataset reality — be honest

The ISL dataset landscape is small. You have three serious options:

### Option A — INCLUDE / INCLUDE-50 (recommended)

- **Source:** IIIT Bangalore, released 2020. ~4,287 videos, 263 signs, 15 categories.
- **License:** non-commercial research use. Cite the original paper.
- **Format:** RGB video, ~5–7 seconds each, signed by deaf adults.
- **Why this:** the only ISL dataset of meaningful size that's publicly downloadable.
- **Get it:** http://zenodo.org/record/4010759

### Option B — ISL-CSLTR (continuous sign language)

- **Source:** Annamalai University. ~700 sentences, continuous signing.
- **Why this:** more realistic (continuous, not isolated signs). But smaller.
- **Use it as:** held-out evaluation set, not training.

### Option C — Bootstrap your own

- Record 10–20 signs that matter for phone calls ("hello," "yes," "no," "appointment," "address," numbers 0–9, "doctor," "thank you," etc.) with consenting deaf collaborators.
- **Why this:** the demo will use exactly these signs. Quality > quantity for the video.
- **Combine with INCLUDE for the published model.**

**Realistic plan:** train on INCLUDE for the broad vocabulary (263 signs), supplement with ~15 custom-recorded signs for the demo scenarios you'll shoot in Phase 4.

---

## 4. The fine-tuning approach — Unsloth on Gemma 4

We're using **Unsloth** for the training loop. Reasons: ~2× faster + half memory for the same loss, supports Gemma 4 from launch, single-A100 (or even T4 with int4 LoRA) is enough.

### Where to train

- **Kaggle T4×2 (free)** — 30 hours/week. Sufficient for E4B LoRA on INCLUDE.
- **Colab Pro A100** — faster, ~$10/month subscription.
- **Local 4090/3090** if you have it.

You've used Kaggle T4 before for the breast cancer YOLO work — same setup.

### Training script — `train_isl_adapter.py`

```python
# Run on Kaggle / Colab / local GPU
# Goal: LoRA fine-tune Gemma 4 E4B vision tower on ISL videos
#       to output ISL glosses given 8 frames.

import os
import json
import torch
from datasets import Dataset
from unsloth import FastVisionModel
from trl import SFTTrainer, SFTConfig
from PIL import Image
import cv2
import numpy as np

# 1. Load base model with Unsloth
model, tokenizer = FastVisionModel.from_pretrained(
    "unsloth/gemma-4-E4B-it",
    load_in_4bit=True,
    use_gradient_checkpointing="unsloth",
)

model = FastVisionModel.get_peft_model(
    model,
    finetune_vision_layers=True,
    finetune_language_layers=True,
    finetune_attention_modules=True,
    finetune_mlp_modules=True,
    r=16,
    lora_alpha=16,
    lora_dropout=0.0,
    bias="none",
    random_state=42,
)

# 2. Build the dataset from INCLUDE
INCLUDE_ROOT = "/kaggle/input/include-isl"
SIGN_GLOSSES = json.load(open(f"{INCLUDE_ROOT}/gloss_map.json"))  # id -> "HELLO"

def sample_8_frames(video_path):
    """Read video, sample 8 evenly-spaced frames, return list of PIL images."""
    cap = cv2.VideoCapture(video_path)
    n = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    idxs = np.linspace(0, n - 1, 8).astype(int)
    frames = []
    for i in idxs:
        cap.set(cv2.CAP_PROP_POS_FRAMES, i)
        ok, frame = cap.read()
        if not ok: continue
        frames.append(Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)))
    cap.release()
    return frames

SYSTEM = "You are an Indian Sign Language interpreter. Given 8 frames of a deaf signer, output the ISL gloss in capitals. Respond with only the gloss, no punctuation."

def build_example(video_path, gloss):
    frames = sample_8_frames(video_path)
    content = [{"type": "text", "text": SYSTEM}]
    for f in frames:
        content.append({"type": "image", "image": f})
    content.append({"type": "text", "text": "Gloss:"})
    return {
        "messages": [
            {"role": "user", "content": content},
            {"role": "assistant", "content": [{"type": "text", "text": gloss}]},
        ]
    }

# Build the HF dataset
records = []
for label_id, gloss in SIGN_GLOSSES.items():
    video_dir = f"{INCLUDE_ROOT}/videos/{label_id}"
    if not os.path.isdir(video_dir): continue
    for fn in os.listdir(video_dir):
        if fn.endswith(".mp4"):
            records.append({"video_path": f"{video_dir}/{fn}", "gloss": gloss})

# Build with materialization (you may want to cache frames to disk to save RAM)
data = Dataset.from_list([build_example(r["video_path"], r["gloss"]) for r in records])
train, eval_ = data.train_test_split(test_size=0.1, seed=42).values()

# 3. Trainer
FastVisionModel.for_training(model)

trainer = SFTTrainer(
    model=model,
    tokenizer=tokenizer,
    train_dataset=train,
    eval_dataset=eval_,
    args=SFTConfig(
        per_device_train_batch_size=1,
        gradient_accumulation_steps=8,
        warmup_steps=20,
        num_train_epochs=3,
        learning_rate=2e-4,
        bf16=True,
        logging_steps=10,
        eval_strategy="steps",
        eval_steps=200,
        save_strategy="steps",
        save_steps=500,
        output_dir="./isl_lora_out",
        remove_unused_columns=False,
        dataset_text_field="",
        dataset_kwargs={"skip_prepare_dataset": True},
        max_seq_length=4096,
    ),
)

trainer.train()

# 4. Save the LoRA
model.save_pretrained("./gemma-4-e4b-isl-lora")
tokenizer.save_pretrained("./gemma-4-e4b-isl-lora")

# 5. Merge + export to LiteRT-compatible .task
model.save_pretrained_merged(
    "./gemma-4-e4b-isl-merged",
    tokenizer,
    save_method="merged_16bit",
)
```

### Hyperparameter notes

- 3 epochs over INCLUDE = ~13,000 examples × 8 frames each.
- Batch size 1 + grad accum 8 = effective 8 — keeps T4 VRAM usable.
- LoRA r=16 is the sweet spot for vision + language fine-tune on Gemma 4 per Unsloth docs.
- Total wall time on T4: ~6 hours per epoch. Budget 24h end-to-end including conversion.

### Conversion to LiteRT `.task`

```bash
pip install mediapipe==0.10.14
python -m mediapipe.tasks.python.genai.converter.llm_converter \
    --input_ckpt=./gemma-4-e4b-isl-merged \
    --ckpt_format=safetensors \
    --model_type=GEMMA4_4B \
    --backend=cpu \
    --output_dir=./out \
    --output_tflite_file=./gemma-4-e4b-isl-int4.task \
    --vocab_model_file=./gemma-4-e4b-isl-merged/tokenizer.model \
    --quantization=int4
```

Push to device:

```bash
adb push gemma-4-e4b-isl-int4.task /data/local/tmp/
```

The Android app now loads *this* `.task` instead of the base one for sign-recognition prompts.

---

## 5. Benchmarks — what you publish

Required for the writeup and the Kaggle/HF model card.

### Metrics to report

1. **Top-1 accuracy** on INCLUDE held-out test split (263 signs, isolated).
2. **Top-5 accuracy** on the same.
3. **Per-category accuracy** (15 INCLUDE categories: pronouns, greetings, family, etc.)
4. **Latency** on Pixel 8: end-to-end (frame → gloss output) median + p95.
5. **Custom-15 accuracy** on the demo signs you record.
6. **Comparison to baseline** — pure MediaPipe Hand Landmarker + a simple LSTM classifier on landmarks.

### Expected numbers (realistic, do not exaggerate)

| Metric | Baseline (Hand-LSTM) | Gemma 4 + ISL-LoRA |
|---|---|---|
| Top-1 (INCLUDE) | ~58% | ~74% (target) |
| Top-5 (INCLUDE) | ~78% | ~91% (target) |
| Custom-15 accuracy | n/a | ~92% (target — with your own data) |
| Pixel 8 latency p50 | ~150ms | ~1100ms |

The latency gap is the honest tradeoff to acknowledge. The accuracy gap is the win. Justify Gemma 4 specifically: it generalizes (you can add signs without retraining the classifier head), it understands context (gloss + sentence in one model), and it outputs natural language (the LSTM only outputs a class label — you'd still need an LLM downstream).

### Publishing checklist

- [ ] Push merged model to HuggingFace: `your-username/gemma-4-e4b-isl-lora`
- [ ] Push LoRA adapter only (smaller download) to a separate repo
- [ ] Write a model card with: dataset, license, intended use, limitations, ethical considerations (signing varies across regions; INCLUDE is one specific dialect), evaluation results, citation
- [ ] Push the `.task` LiteRT bundle as a release asset
- [ ] Cite the INCLUDE paper (Sridhar et al. 2020) prominently

---

## 6. Android-side integration

### `CameraX` setup

Add to `build.gradle.kts`:

```kotlin
implementation("androidx.camera:camera-core:1.3.3")
implementation("androidx.camera:camera-camera2:1.3.3")
implementation("androidx.camera:camera-lifecycle:1.3.3")
implementation("androidx.camera:camera-view:1.3.3")

// MediaPipe Hand Landmarker for sign segmentation
implementation("com.google.mediapipe:tasks-vision:0.10.14")
```

### `SignCapture.kt`

```kotlin
package com.dhwani.app.sign

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors

class SignCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onSignClip: (List<android.graphics.Bitmap>) -> Unit
) {
    private val executor = Executors.newSingleThreadExecutor()
    private var landmarker: HandLandmarker? = null
    private val frameBuffer = ArrayDeque<android.graphics.Bitmap>()
    private val MAX_BUFFER = 90  // 6s at 15fps

    private var inMotion = false
    private var motionStartIdx = 0
    private var lastEnergy = 0.0

    fun start(previewView: androidx.camera.view.PreviewView) {
        // Build hand landmarker
        landmarker = HandLandmarker.createFromOptions(
            context,
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(
                    com.google.mediapipe.tasks.core.BaseOptions.builder()
                        .setModelAssetPath("hand_landmarker.task").build()
                )
                .setNumHands(2)
                .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> onLandmarks(result) }
                .build()
        )

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 480))
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val bmp = proxy.toBitmap()
                bufferFrame(bmp)
                val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bmp).build()
                landmarker?.detectAsync(mpImage, System.currentTimeMillis())
                proxy.close()
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview, analysis
            )
        }, java.util.concurrent.Executors.newSingleThreadExecutor())
    }

    private fun bufferFrame(bmp: android.graphics.Bitmap) {
        frameBuffer.addLast(bmp)
        if (frameBuffer.size > MAX_BUFFER) frameBuffer.removeFirst()
    }

    private fun onLandmarks(result: HandLandmarkerResult) {
        // Compute motion energy as L2 distance between successive frame landmarks
        if (result.landmarks().isEmpty()) {
            if (inMotion) finalizeSign()
            return
        }
        val flat: List<NormalizedLandmark> = result.landmarks().flatten()
        val energy = flat.sumOf {
            // Simplified: variance proxy
            (it.x() * it.x() + it.y() * it.y()).toDouble()
        }
        val delta = kotlin.math.abs(energy - lastEnergy)
        lastEnergy = energy

        if (delta > 0.5 && !inMotion) {
            inMotion = true
            motionStartIdx = frameBuffer.size
        } else if (delta < 0.1 && inMotion) {
            finalizeSign()
        }
    }

    private fun finalizeSign() {
        inMotion = false
        val endIdx = frameBuffer.size
        val startIdx = motionStartIdx.coerceAtLeast(0)
        if (endIdx - startIdx < 8) return  // too short to be a sign
        val clipFrames = frameBuffer.toList().subList(startIdx, endIdx)
        val sampled = sampleEvenly(clipFrames, 8)
        onSignClip(sampled)
    }

    private fun sampleEvenly(frames: List<android.graphics.Bitmap>, n: Int): List<android.graphics.Bitmap> {
        if (frames.size <= n) return frames
        val step = frames.size.toDouble() / n
        return (0 until n).map { frames[(it * step).toInt()] }
    }
}
```

### Sign → gloss → sentence in `GemmaEngine`

```kotlin
suspend fun signClipToSentence(
    frames: List<android.graphics.Bitmap>,
    conversationContext: String,
    targetLanguage: String,
): String {
    // Pass 1: vision → gloss (uses ISL-LoRA model session)
    val s1 = signSession ?: error("ISL Gemma not initialized")
    s1.addQueryChunk("Given these 8 frames of an ISL signer, output the gloss only.")
    frames.forEach { s1.addImage(it) }
    val gloss = s1.generateResponse().trim()

    // Pass 2: gloss → natural sentence (uses base Gemma session from Phase 2)
    val prompt = """
        Conversation context: $conversationContext
        The user signed (ISL gloss): "$gloss"
        Convert this into a natural ${if (targetLanguage == "hi") "Hindi (Devanagari)" else "English"}
        sentence appropriate to say in the current conversation. Output ONLY the sentence.
    """.trimIndent()
    return baseSession!!.run { addQueryChunk(prompt); generateResponse() }.trim()
}
```

Wire `SignCapture.onSignClip = { frames -> ... signClipToSentence(...) → TTS }` and you have the loop.

---

## 7. Two-model strategy on-device

Loading two `.task` files (base Gemma + ISL-LoRA Gemma) doubles memory. Two ways to handle:

**Option A (preferred)** — Merge the LoRA at training time. Load only the merged ISL model. Use it for *both* sign recognition AND conversation/smart-replies. The LoRA adapter is small enough that a fine-tuned model usually doesn't regress on non-sign tasks. Verify with a small benchmark on standard QA tasks.

**Option B** — Hot-swap. Load base Gemma at app launch. When sign capture starts, unload base, load ISL. Slow (5–8s swap) and clunky. Only use if Option A regresses.

Use Option A unless your evals say otherwise.

---

## 8. Testing checklist — Phase 3

- [ ] INCLUDE dataset downloaded and indexed
- [ ] Unsloth training runs to completion on Kaggle T4 without OOM
- [ ] LoRA top-1 accuracy on held-out INCLUDE > 70%
- [ ] Custom-15 signs (your demo vocabulary) classified > 90% by the merged model
- [ ] LiteRT `.task` conversion succeeds and loads on device
- [ ] Hand Landmarker reliably segments signs (start/end energy threshold tuned)
- [ ] End-to-end Android demo: sign "MERA GHAR" → spoken "मेरा घर सेक्टर 14 में है" / "My home is in Sector 14" (combining sign + context tool call)
- [ ] Sign-to-TTS latency p50 < 1.8s on Pixel 8
- [ ] Model card published on HuggingFace
- [ ] Weights download link works in incognito browser

---

## 9. Common failure modes — Phase 3

| Symptom | Cause | Fix |
|---|---|---|
| LoRA train loss plateaus high | Too small rank | Increase r=16 → r=32 |
| Vision tower not training | Unsloth flag wrong | `finetune_vision_layers=True` is critical for Gemma 4 |
| Hand landmarker misses fast signs | 15fps too low | Bump to 30fps; downsample frames before Gemma |
| Gloss output is gibberish | Tokenizer drift after LoRA | Always save tokenizer with the model |
| ISL model regresses on general text | Catastrophic forgetting | Add ~10% general SFT data to fine-tune mix |
| LiteRT conversion fails | Model type flag | Use `GEMMA4_4B` for E4B; mismatch is a common foot-gun |
| Two-pass latency too high | Both Gemma calls in sequence | Pipeline: start pass-2 prompt prep while pass-1 still streaming gloss |

---

## 10. Timeline — Phase 3 in 2 days

| Day | Morning | Afternoon |
|---|---|---|
| **Day 6** | Kick off Unsloth training on Kaggle (runs ~24h) — record 15 custom demo signs in parallel | Build `SignCapture` + hand landmarker pipeline on device |
| **Day 7 (morning)** | Verify training, evaluate, convert to LiteRT, push to device | Integrate sign-to-TTS into call flow, end-to-end demo rehearsal |

If training overruns or quality is poor, **fall back gracefully**: ship a simpler hand-landmark + LSTM classifier on the custom-15 signs, label the published model "preliminary," and lean the demo on the smart-replies + context features from Phase 2. The hackathon weights the *story* and *demo* over absolute accuracy.

---

## 11. Ethics + framing for the writeup

Be explicit about limitations:

- ISL has regional dialects; the model trained on INCLUDE represents specific signers.
- Sign language is *not* a 1:1 mapping to spoken language. ISL has its own grammar. Gloss-to-sentence conversion involves cultural and grammatical translation that requires deaf community oversight.
- This tool is for *the deaf user's own use* — to help them participate in voice calls. It is not for hearing people to "interpret" deaf signers without consent.
- The fine-tune is open-sourced specifically so deaf-led organisations can audit it, adapt it, and improve it. We invite that.

These statements aren't fluff; they're what makes the Safety & Trust angle defensible if you pivot tracks.

---

## 12. What this phase enables for Phase 4

By end of Phase 3 you have a working app that:

- Captions live calls in real time
- Suggests 3 smart replies the user can tap
- Pulls personal context via function calls
- Accepts sign-language input as an alternative to typing
- Speaks naturally into the call in Hindi or English

Phase 4 is purely about packaging this into a 3-minute video and a 1500-word writeup that win.
