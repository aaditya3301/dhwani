# Dhwani

Dhwani is an Android accessibility prototype that helps deaf users participate in live phone calls.

The app runs beside a normal phone call. The caller's speech is captured from speakerphone audio and shown as live captions. The deaf user can type or tap a suggested reply, and Dhwani speaks it aloud through Android Text-to-Speech so the caller can hear it.

## Current State

Implemented in this repo:

- Android app in Kotlin + Jetpack Compose.
- Runtime permission screen.
- Foreground microphone service.
- Speakerphone microphone capture using `VOICE_COMMUNICATION`.
- Acoustic echo cancellation and noise suppression setup.
- Offline speech recognition wrapper using Vosk.
- Android Text-to-Speech output.
- MediaPipe LLM Inference wrapper for a local Gemma `.task` model.
- Personal context form.
- Gemma-generated smart replies.
- Gemma-generated pre-call briefing.
- Local recent call summaries using SharedPreferences.

Not implemented yet:

- Camera/sign-language input.
- Room database or encrypted structured storage.
- Native Gemma tool/function calling.
- Automatic phone-call state detection.
- Final production/demo polish.

## Requirements

Install these on the new development device:

- Android Studio, recent stable version.
- JDK 17.
- Git.
- Android SDK with API 34 or newer installed.
- A real Android phone for testing.
- Android phone with Android 8.0+.
- Recommended phone: Pixel 7/8/9 or another arm64 phone with 6GB+ RAM.

The app is configured for:

- Kotlin `2.0.0`
- Android Gradle Plugin `8.13.2`
- Gradle wrapper `9.0-milestone-1`
- Min SDK `26`
- Target SDK `34`
- Compile SDK `36`
- ABI: `arm64-v8a`

## Clone And Open

```bash
git clone <your-repo-url>
cd gemma
```

Open the project root in Android Studio.

If Android Studio asks to trust the project, trust it. Then let Gradle sync finish.

## Local Files Not Committed

These files are intentionally not committed:

- `local.properties`
- Gemma `.task` model files
- Vosk model folders
- APK/AAB outputs
- Gradle/Android Studio build folders

On a new device, Android Studio usually creates `local.properties` automatically. It should look like this:

```properties
sdk.dir=C\:\\Users\\<your-name>\\AppData\\Local\\Android\\Sdk
```

Do not commit `local.properties`.

## Model Setup

Dhwani can run without the models, but captions and Gemma features need model files.

### 1. Vosk Speech Models

For live captions, add at least one Vosk model.

Expected model locations:

```text
app/src/main/assets/vosk-en/
app/src/main/assets/vosk-hi/
```

Recommended downloads:

- English India: `vosk-model-small-en-in-0.4`
- Hindi: `vosk-model-small-hi-0.22`

After downloading and unzipping, copy the model contents into one of these folders.

Example final layout:

```text
app/src/main/assets/vosk-en/am/
app/src/main/assets/vosk-en/conf/
app/src/main/assets/vosk-en/graph/
app/src/main/assets/vosk-en/ivector/
```

or:

```text
app/src/main/assets/vosk-hi/am/
app/src/main/assets/vosk-hi/conf/
app/src/main/assets/vosk-hi/graph/
```

Keep the folder name exactly `vosk-en` or `vosk-hi`.

These folders are ignored by Git because they are large.

### 2. Gemma Model

For smart replies, briefing, summaries, and the `Test Gemma` button, the app expects this file:

```text
gemma-4-e4b-it-int4.task
```

Put it here if you want it bundled into the debug APK:

```text
app/src/main/assets/models/gemma-4-e4b-it-int4.task
```

Alternative: push/copy it into the app private files directory on the phone. The app checks:

```text
filesDir/gemma-4-e4b-it-int4.task
```

For easiest setup while developing, use the assets folder:

```text
app/src/main/assets/models/
```

The `.task` file is ignored by Git.

Important: the Gemma model can be several GB. A low-RAM phone may fail to load it.

## Build

From Android Studio:

1. Open the project.
2. Wait for Gradle sync.
3. Select the `app` run configuration.
4. Connect your Android phone with USB debugging enabled.
5. Click Run.

From terminal:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK will be generated under:

```text
app/build/outputs/apk/debug/
```

## Phone Setup

On the test phone:

1. Enable Developer Options.
2. Enable USB Debugging.
3. Connect the phone to the computer.
4. Accept the USB debugging prompt.
5. Install/run the app from Android Studio.
6. Grant permissions when Dhwani asks.

Permissions used:

- Microphone
- Phone state
- Notifications
- Foreground microphone service
- Audio settings

## How To Test The App

### Basic App Test

1. Open Dhwani.
2. Grant permissions.
3. Fill the personal context form.
4. Tap `Save`.
5. Tap `Test Gemma`.

Expected:

- If the Gemma model exists and the phone can load it, the status changes to a Gemma response.
- If not, the app shows a model unavailable message.

### Caption Test

1. Make sure a Vosk model is present.
2. Tap `Start`.
3. Speak near the phone or play speech from another device.

Expected:

- The live caption box updates with recognized speech.

If it says to add a Vosk model, the app did not find `vosk-en` or `vosk-hi`.

### TTS Test

1. Type a message in `Type to speak`.
2. Tap `Speak`.

Expected:

- The phone speaks the typed text aloud.

### Real Call Test

1. Start or receive a normal phone call.
2. Put the phone on speaker.
3. Open Dhwani.
4. Tap `Start`.
5. Ask the other person to speak.
6. Watch captions.
7. Type a reply and tap `Speak`.

Expected:

- Caller speech appears as captions.
- Typed replies are spoken aloud through the phone speaker.
- The caller should hear the spoken reply over the call.

## Useful Project Structure

```text
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
│   └── Phase2Assistant.kt
└── ui/
    ├── CallScreen.kt
    ├── CallViewModel.kt
    ├── PermissionGate.kt
    └── theme/Theme.kt
```

Important docs:

```text
docs/PHASE_1_STATUS.md
docs/PHASE_2_STATUS.md
docs/PHASE_3_STATUS.md
docs/PHASE_4_STATUS.md
```

## Troubleshooting

### Gradle Sync Fails

Try:

```bash
./gradlew --stop
./gradlew clean
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Also check:

- JDK is set to 17.
- Android SDK is installed.
- `local.properties` points to the correct SDK path.
- Android Studio has downloaded SDK 34+.

### App Says Gemma Not Loaded

Check that the file exists at:

```text
app/src/main/assets/models/gemma-4-e4b-it-int4.task
```

or inside the app files directory on the phone as:

```text
gemma-4-e4b-it-int4.task
```

Also check that the phone has enough RAM.

### Captions Do Not Work

Check that the Vosk model exists at:

```text
app/src/main/assets/vosk-en/
```

or:

```text
app/src/main/assets/vosk-hi/
```

The model folder should contain actual model files, not just `.gitkeep`.

Then rebuild and reinstall the app.

### TTS Is Too Quiet In A Call

Make sure the phone call is on speaker. Dhwani routes audio using communication mode, but the real call still needs speakerphone for the prototype approach.

### App Crashes When Loading Gemma

Likely causes:

- Phone does not have enough RAM.
- Model file is corrupt or incomplete.
- Wrong `.task` model format.
- Device ABI is not supported.

Try testing first without Gemma, then add the model after captions/TTS work.

## Development Notes

- Do not commit large model files.
- Do not commit `local.properties`.
- Test on a real phone, not only an emulator.
- The current app uses speakerphone loopback, not direct phone-call audio capture.
- Echo cancellation depends on the phone hardware and Android audio stack.
- Gemma generation is blocking right now, not streaming.

## Vision

Dhwani is designed as a private, on-device phone-call assistant. The long-term goal is to combine live captions, smart replies, personal context, call summaries, and eventually sign-language input while keeping sensitive call data on the user's phone.
