# Phase 1 Status

## Status

Phase 1 is implemented and locally tested as the foundation of Dhwani.

## Built

- Android app scaffold using Kotlin and Jetpack Compose.
- Runtime permission flow for microphone, phone state, and notifications.
- Foreground microphone service for active call-captioning sessions.
- Speakerphone microphone capture using `VOICE_COMMUNICATION`.
- Acoustic echo cancellation and noise suppression setup.
- Offline speech-to-text pipeline using Vosk.
- Android Text-to-Speech output for typed replies.
- MediaPipe Gemma `.task` loading and one-shot inference test.
- Model asset copy flow for Vosk and Gemma.
- Minimal call UI with Start, Stop, captions, typed reply, Speak, and Test Gemma.

## Working

- App opens on the test phone.
- Permissions can be granted successfully.
- Vosk captions appear when speaking near the phone.
- Typed replies are spoken through Android Text-to-Speech.
- Gemma loads and returns a response for the test translation prompt.
- The foreground service starts while listening.

## Tested

- Android Studio debug build succeeds.
- App install and launch on phone.
- Permission flow.
- Vosk speech transcription.
- TTS output.
- Gemma test inference.
- Basic Start and Stop flow.

## Mocked Or Approximate

- Call audio is captured through speakerphone loopback, not direct call audio interception.
- Echo cancellation depends on device hardware and Android audio stack.
- Phase 1 does not yet include automatic phone-call state handling beyond the app-controlled Start/Stop flow.

## Known Gaps

- Needs real-call testing across multiple phones.
- Needs latency measurement for captions and TTS.
- Needs stronger handling for missing/corrupt model files.
- Native library 16 KB page-size warning remains for future production readiness.

## Next Updates

- Record real-call test results.
- Add measured latency numbers.
- Document device compatibility results.
