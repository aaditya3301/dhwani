# Dhwani

Dhwani is an Android accessibility prototype that helps deaf users take part in live phone calls.

The app runs alongside a normal phone call. The caller's speech is captured from speakerphone audio and displayed as live captions. The user can type or tap a suggested reply, which Dhwani speaks aloud through Android Text-to-Speech so the caller can hear it.

---

## Overview

Dhwani is an on-device phone-call assistant. The goal is to combine live captions, smart replies, personal context, sign input, and call summaries while keeping all sensitive call data on the user's device.

---

## Current Status

**Implemented**

- Android app in Kotlin + Jetpack Compose
- Minimal three-section UI for live calls, outgoing calls, and personal details
- Microphone permission at startup and camera permission only for sign reply
- Foreground microphone service
- Speakerphone capture using `VOICE_COMMUNICATION`
- Acoustic echo cancellation and noise suppression
- Offline speech recognition via Vosk
- Android Text-to-Speech output
- On-device LLM inference via MediaPipe (local Gemma `.task` model)
- Personal context form
- Structured encrypted local personal context
- Gemma-generated smart replies and pre-call briefing
- Local tool dispatch for address/contact/medical/payment/recent-call context
- Automatic on-device recognition for 263 isolated INCLUDE signs
- Manual 15-phrase call vocabulary for meanings absent from the public model
- Encrypted local call summaries

---

## Requirements

Install the following on the development machine:

- Android Studio (recent stable release)
- JDK 17
- Git
- Android SDK, API 34 or newer
- A physical Android phone for testing (Android 8.0+, arm64, 6 GB+ RAM recommended — e.g. Pixel 7/8/9)

**Project configuration**

| Setting | Value |
|---|---|
| Kotlin | 2.0.0 |
| Android Gradle Plugin | 8.13.2 |
| Gradle wrapper | 9.0-milestone-1 |
| Min SDK | 26 |
| Target SDK | 34 |
| Compile SDK | 36 |
| ABI | arm64-v8a |

---

## Setup

### 1. Clone and open

```bash
git clone <your-repo-url>
cd dhwani
```

Open the project root in Android Studio and let Gradle sync finish.

### 2. Local files (not committed)

The following are intentionally excluded from version control:

- `local.properties`
- Gemma `.task` model files
- Vosk model folders
- APK / AAB outputs
- Gradle and Android Studio build folders

Android Studio usually generates `local.properties` automatically. It should point to your SDK:

```
sdk.dir=C\:\\Users\\<your-name>\\AppData\\Local\\Android\\Sdk
```

Do not commit `local.properties`.

### 3. Model setup

Dhwani's stage 3 sign models are already bundled. Captions and Gemma features
use the additional models below.

**Vosk speech models** — needed for live captions. Add at least one model:

| Language | Model | Path |
|---|---|---|
| English (India) | `vosk-model-small-en-in-0.4` | `app/src/main/assets/vosk-en/` |
| Hindi | `vosk-model-small-hi-0.22` | `app/src/main/assets/vosk-hi/` |

After unzipping, copy the model contents into the matching folder. Keep the folder name exactly `vosk-en` or `vosk-hi`. Example layout:

```
app/src/main/assets/vosk-en/
├── am/
├── conf/
├── graph/
└── ivector/
```

**Gemma model** — needed for smart replies, briefing, summaries, and sign-sentence generation. Download the Android MediaPipe-compatible `gemma3-1b-it-int4.task` (about 555 MB) from [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT). Hugging Face requires a free account and acceptance of the Gemma license before downloading.

Place it in either location:

```
app/src/main/assets/models/gemma3-1b-it-int4.task   # bundled into the debug APK
```

or in the app's private files directory on the phone:

```
filesDir/gemma3-1b-it-int4.task
```

> Note: Google recommends testing MediaPipe LLM Inference on a high-end physical Android phone (for example, Pixel 8 or Samsung S23 and newer). Emulators and low-RAM devices may fail to load it.

**Sign recognition models** - no setup is required. The app includes the
MediaPipe Holistic landmark model, converted OpenHands INCLUDE BiLSTM, and its
263-label vocabulary under `app/src/main/assets/models/sign/`. stage 3 does not
require training, a dataset, or a Hugging Face login.

---

## Build

**From Android Studio**

1. Open the project and wait for Gradle sync.
2. Select the `app` run configuration.
3. Connect a phone with USB debugging enabled.
4. Click Run.

**From the terminal**

```bash
./gradlew assembleDebug          # macOS / Linux
.\gradlew.bat assembleDebug      # Windows PowerShell
```

The debug APK is generated at:

```
app/build/outputs/apk/debug/
```

---

## Phone Setup

On the test phone:

1. Enable Developer Options.
2. Enable USB Debugging.
3. Connect to the computer and accept the debugging prompt.
4. Install and run the app from Android Studio.
5. Grant permissions when prompted.

Permissions used: microphone, optional camera, notifications, foreground microphone service, and audio settings.

---

## Testing

**Basic app test**

1. Open Dhwani and allow microphone access.
2. Open **You**, add a name and any useful details, then tap **Save details**.
3. Open **Live** and tap **Start captions**.

Expected: the live caption area enters listening mode. Smart features load when first needed.

**Caption test**

1. Ensure a Vosk model is present.
2. Tap Start and speak near the phone (or play speech from another device).

Expected: the caption box updates with recognized speech. A prompt to add a Vosk model means `vosk-en` / `vosk-hi` was not found.

**Text-to-Speech test**

1. Type a message in *Type to speak*.
2. Tap Speak.

Expected: the phone speaks the text aloud.

**Sign input test**

1. Open **Live** and tap **Sign reply**.
2. Allow camera access and frame your upper body and both hands in good light.
3. Tap **Recognize my sign** and perform one isolated sign for about three seconds.
4. Confirm one of the model's top results, or choose a manual call phrase such as `Reschedule`.
5. Tap **Edit first** or **Speak now**.

Expected: the on-device model predicts an INCLUDE gloss, Dhwani converts it into
a natural reply, and the normal draft/TTS path is reused. Try `Hello`, `Doctor`,
`Medicine`, `Hospital`, or `Thankyou` for meanings present in both workflows.

**Real call test**

1. Start or receive a call and put it on speaker.
2. Open Dhwani and tap Start.
3. Have the caller speak, then type a reply or tap a smart reply.
4. Tap Stop after the call to save a summary.

Expected: caller speech appears as captions, and typed replies are spoken over the call.

**Outgoing briefing test**

1. Enter a call goal in *Pre-call briefing*.
2. Tap Brief.
3. Confirm the phone number field, then tap Place call.

Expected: Dhwani opens the Android call/dial screen and starts the call pipe.

---

## Project Structure

```
app/src/main/java/com/dhwani/app/
├── DhwaniApp.kt
├── MainActivity.kt
├── audio/
│   ├── SpeakerphoneRecorder.kt
│   ├── SpeechToText.kt
│   ├── TextToSpeechEngine.kt
│   └── VoskModelManager.kt
├── call/
│   └── CallService.kt
├── data/
│   ├── CallLogStore.kt
│   ├── SecureJsonStore.kt
│   └── UserContext.kt
├── llm/
│   ├── GemmaEngine.kt
│   ├── CallAssistant.kt
│   └── AssistantToolDispatcher.kt
├── sign/
│   ├── OpenHandsSignRecognizer.kt
│   ├── SignFrameAnalyzer.kt
│   └── SignVocabulary.kt
└── ui/
    ├── CallScreen.kt
    ├── CallViewModel.kt
    ├── PermissionGate.kt
    └── theme/Theme.kt
```

Status documents: `docs/stage_1_STATUS.md` through `docs/stage_4_STATUS.md`.

stage 3 model provenance and the optional conversion workflow are documented in
`app/src/main/assets/models/sign/README.md` and `docs/stage_3_TRAINING.md`.

---

## Troubleshooting

**Gradle sync fails**

```bash
./gradlew --stop
./gradlew clean
./gradlew assembleDebug
```

Also verify: JDK is set to 17, the Android SDK is installed, `local.properties` points to the correct SDK path, and SDK 34+ is downloaded.

**Gemma not loaded** — Confirm `gemma3-1b-it-int4.task` exists in `app/src/main/assets/models/` or in the app's files directory on the phone, and that the device has enough RAM.

**Captions not working** — Confirm a Vosk model exists at `vosk-en/` or `vosk-hi/` and contains real model files (not just `.gitkeep`). Rebuild and reinstall.

**TTS too quiet in a call** — Ensure the call is on speakerphone. The prototype relies on speakerphone loopback.

**Crash when loading Gemma** — Likely insufficient RAM, a corrupt or incomplete model file, the wrong `.task` format, or an unsupported device ABI. Test without Gemma first, then add the model once captions and TTS work.

**Sign recognition asks to retry** - Keep your shoulders, upper body, and both
hands visible for the full capture. Use even front lighting and avoid moving the
phone while signing. The model recognizes one sign per capture.

---

## Development Notes

- Do not commit downloaded Gemma/Vosk models or `local.properties`; the small
  stage 3 mobile assets are intentionally bundled for reproducible offline use.
- Test on a real phone, not only an emulator.
- The app uses speakerphone loopback, not direct call-audio capture.
- Echo cancellation depends on the device hardware and Android audio stack.
- Gemma generation is currently blocking, not streaming.
- stage 3 uses a pretrained 263-class isolated-sign model. It does not claim
  continuous ISL sentence translation or automatic support for phrases outside
  the INCLUDE vocabulary.
