# Dhwani

Dhwani is an Android accessibility prototype that helps deaf users participate in live phone calls.

The app runs alongside a normal phone call and creates a simple communication bridge: caller speech is converted into live captions, and the user's typed replies are spoken back through the phone speaker using Text-to-Speech.

## Core Idea

Dhwani does not require the other caller to install an app or use a special calling service. The user places or receives a regular phone call, enables speakerphone, and Dhwani handles the on-device accessibility layer.

## What It Does

- Captions caller speech in real time
- Lets the user type replies during the call
- Speaks typed replies aloud through Android Text-to-Speech
- Runs speech recognition locally on the device
- Loads an on-device Gemma model for future assistant features

## Technology

- Kotlin
- Jetpack Compose
- Android Foreground Services
- Vosk offline speech recognition
- Android Text-to-Speech
- MediaPipe LLM Inference

## Vision

Dhwani is designed as a foundation for a private, on-device phone-call assistant that can later provide smart replies, personal context, call summaries, and multimodal input while keeping the core calling experience simple and accessible.
