# Dhwani

Dhwani is an Android accessibility prototype that helps deaf users take part in live phone calls.

The app runs alongside a normal phone call. The caller's speech is captured from speakerphone audio and displayed as live captions. The user can type or tap a suggested reply, which Dhwani speaks aloud through Android Text-to-Speech so the caller can hear it.

---

## Overview

Dhwani is an on-device phone-call assistant. The goal is to combine live captions, smart replies, personal context, and call summaries — with optional sign-language input in the future — while keeping all sensitive call data on the user's device.

---

## Current Status

**Implemented**

- Android app in Kotlin + Jetpack Compose
- Runtime permission screen
- Foreground microphone service
- Speakerphone capture using `VOICE_COMMUNICATION`
- Acoustic echo cancellation and noise suppression
- Offline speech recognition via Vosk
- Android Text-to-Speech output
- On-device LLM inference via MediaPipe (local Gemma `.task` model)
- Personal context form
- Gemma-generated smart replies and pre-call briefing
- Local call summaries via SharedPreferences

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
cd gemma
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

Dhwani runs without models, but captions and Gemma features require them.

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

**Gemma model** — needed for smart replies, briefing, summaries, and the Test Gemma button. Expected file: `gemma-4-e4b-it-int4.task`.

Place it in either location:

```
app/src/main/assets/models/gemma-4-e4b-it-int4.task   # bundled into debug APK (recommended for dev)
```

or in the app's private files directory on the phone:

```
filesDir/gemma-4-e4b-it-int4.task
```

> Note: the Gemma model can be several GB. Low-RAM devices may fail to load it.

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

Permissions used: microphone, phone state, notifications, foreground microphone service, audio settings.

---

## Testing

**Basic app test**

1. Open Dhwani and grant permissions.
2. Fill the personal context form and tap Save.
3. Tap Test Gemma.

Expected: if the Gemma model is present and loadable, the status shows a Gemma response; otherwise a "model unavailable" message appears.

**Caption test**

1. Ensure a Vosk model is present.
2. Tap Start and speak near the phone (or play speech from another device).

Expected: the caption box updates with recognized speech. A prompt to add a Vosk model means `vosk-en` / `vosk-hi` was not found.

**Text-to-Speech test**

1. Type a message in *Type to speak*.
2. Tap Speak.

Expected: the phone speaks the text aloud.

**Real call test**

1. Start or receive a call and put it on speaker.
2. Open Dhwani and tap Start.
3. Have the caller speak, then type a reply and tap Speak.

Expected: caller speech appears as captions, and typed replies are spoken over the call.

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
│   └── UserContext.kt
├── llm/
│   ├── GemmaEngine.kt
│   └── CallAssistant.kt
└── ui/
    ├── CallScreen.kt
    ├── CallViewModel.kt
    ├── PermissionGate.kt
    └── theme/Theme.kt
```

Status documents: `docs/stage_1_STATUS.md` through `docs/stage_4_STATUS.md`.

---

## Troubleshooting

**Gradle sync fails**

```bash
./gradlew --stop
./gradlew clean
./gradlew assembleDebug
```

Also verify: JDK is set to 17, the Android SDK is installed, `local.properties` points to the correct SDK path, and SDK 34+ is downloaded.

**Gemma not loaded** — Confirm `gemma-4-e4b-it-int4.task` exists in `app/src/main/assets/models/` or in the app's files directory on the phone, and that the device has enough RAM.

**Captions not working** — Confirm a Vosk model exists at `vosk-en/` or `vosk-hi/` and contains real model files (not just `.gitkeep`). Rebuild and reinstall.

**TTS too quiet in a call** — Ensure the call is on speakerphone. The prototype relies on speakerphone loopback.

**Crash when loading Gemma** — Likely insufficient RAM, a corrupt or incomplete model file, the wrong `.task` format, or an unsupported device ABI. Test without Gemma first, then add the model once captions and TTS work.

---

## Development Notes

- Do not commit large model files or `local.properties`.
- Test on a real phone, not only an emulator.
- The app uses speakerphone loopback, not direct call-audio capture.
- Echo cancellation depends on the device hardware and Android audio stack.
- Gemma generation is currently blocking, not streaming.
