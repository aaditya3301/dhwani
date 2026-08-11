# Dhwani

An on-device Android call assistant for deaf and hard-of-hearing users.

## Features

- Offline English and Hindi live captions
- Typed and smart replies spoken through Android TTS
- Personal context, call briefings, and encrypted summaries
- Front-camera Indian Sign Language input
- On-device Gemma, MediaPipe, and ONNX Runtime inference

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android 8.0+ arm64 phone with USB debugging

## Setup

```bash
git clone https://github.com/aaditya3301/dhwani.git
cd dhwani
```

Open the project in Android Studio and let Gradle sync. Android Studio creates
`local.properties`; confirm that it points to your Android SDK.

Add the optional caption and assistant models:

```text
app/src/main/assets/vosk-en/                         English captions
app/src/main/assets/vosk-hi/                         Hindi captions
app/src/main/assets/models/gemma3-1b-it-int4.task   Smart features
```

Use the Vosk English India and Hindi models from
[Vosk](https://alphacephei.com/vosk/models). Download the Gemma task file from
[Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT).

Sign-recognition assets are already included.

## Run

Connect the phone, accept the USB debugging prompt, then run:

```powershell
.\gradlew.bat installDebug
```

On macOS or Linux:

```bash
./gradlew installDebug
```

You can also select the phone in Android Studio and click **Run**.

## Use

1. Put the phone call on speaker.
2. Open **Live** and start captions.
3. Type, choose, or sign a reply, then tap **Speak**.

Camera access is requested only for sign input. Call data and inference stay on
the device.

## Models

Sign recognition uses a hybrid offline pipeline. Google's MediaPipe Gesture
Recognizer handles seven stable single-hand gestures. ISL recognition uses the
official small Transformer checkpoint from the MIT-licensed
[AI4Bharat INCLUDE repository](https://github.com/AI4Bharat/INCLUDE), converted
to ONNX without changing its weights. It reads up to 169 frames containing the
first 25 pose landmarks and all 21 landmarks from each hand.

The INCLUDE model was trained as a 263-class isolated-word classifier. Dhwani
only accepts a focused subset useful during calls, including greetings,
pronouns, health terms, phone terms, money, and time. A result must agree across
three temporal views and pass confidence, framing, and class-separation checks.
Anything else is shown as unknown instead of being forced into labels such as
`DOG`, `ELECTION`, or `TRANSPORTATION`.

Tap **Recognize sign**, keep your face, shoulders, elbows, and hands visible,
then perform one complete sign once. The app recognizes isolated signs, not
continuous sign-language sentences. `FLOWER` is not one of the INCLUDE classes
and is intentionally returned as unknown.
