# DHWANI — Phase 1: Foundation

> The Deaf Person's Phone-Call Agent. Phase 1 builds the plumbing: Gemma 4 E4B running on a real Android phone, with a bidirectional audio pipe that turns the caller's voice into on-screen text and the deaf user's typed text into spoken audio the caller hears.

---

## 1. Goal of this phase

At the end of Phase 1, you can place a real outgoing call from a Pixel/recent Android phone, put it on speaker, and watch the caller's voice appear as live captions on screen with **<1.2s end-to-end latency**. You can type a reply on screen, hit send, and the caller hears it in natural Hindi or English over the line.

**No AI smarts yet.** No function calling, no context store, no sign-language. Just the pipe, working end-to-end, with Gemma 4 E4B loaded and idle in the foreground service. If this phase doesn't work, nothing else matters.

---

## 2. Architecture for Phase 1

```
┌────────────────────────────────────────────────────────────────┐
│                  Android Phone (Foreground app)                │
│                                                                │
│   ┌──────────┐    ┌─────────────┐    ┌────────────────────┐    │
│   │   Mic    │───▶│   AudioRec  │───▶│   Whisper.cpp /   │    │
│   │ (loopbk) │    │  (16kHz PCM)│    │   MediaPipe ASR    │    │
│   └──────────┘    └─────────────┘    └─────────┬──────────┘    │
│                          ▲                     │               │
│                          │ AEC                 ▼               │
│                          │              ┌───────────────┐      │
│                   ┌──────┴──────┐       │  Live caption │      │
│                   │  Speaker    │       │  UI (Jetpack  │      │
│                   │  (TTS out)  │       │  Compose)     │      │
│                   └─────────────┘       └───────┬───────┘      │
│                          ▲                     │               │
│                          │              ┌──────▼──────────┐    │
│                   ┌──────┴──────┐       │ Text-input UI   │    │
│                   │ Android TTS │◀───── │  (Compose)     │    │
│                   │  (Hindi/En) │       └─────────────────┘    │
│                   └─────────────┘                              │
│                                                                │
│   ┌────────────────────────────────────────────────────┐       │
│   │  Gemma 4 E4B (LiteRT / MediaPipe LLM Inference)    │       │
│   │  loaded on init, idle — used from Phase 2 onward   │       │
│   └────────────────────────────────────────────────────┘       │
└────────────────────────────────────────────────────────────────┘
         │                                       ▲
         │ Speakerphone audio                    │ Speakerphone audio
         ▼                                       │
   ╔═══════════════════════════════════════════════════╗
   ║       The real phone call — over normal           ║
   ║       cellular / IP voice. Counterparty needs     ║
   ║       NO app, NO install.                         ║
   ╚═══════════════════════════════════════════════════╝
```

Critical: the call is a normal call placed by the Android dialer. The app runs *alongside* the call, listening to and speaking into the speakerphone. We never intercept the call audio stream itself — that's the architectural decision that lets this ship.

---

## 3. Prerequisites — exact versions

| Tool | Version | Notes |
|---|---|---|
| Android Studio | Hedgehog 2023.1.1 or later | Iguana/Koala fine |
| Android Gradle Plugin | 8.4.0+ | Required for MediaPipe LLM |
| Kotlin | 2.0.0+ | |
| JDK | 17 | Set in Project Structure → SDK Location |
| Min SDK | 26 (Android 8.0) | for `AudioRecord` + foreground service types |
| Target SDK | 34 | |
| NDK | 26.1.10909125 | for native audio + MediaPipe |
| MediaPipe LLM Inference | 0.10.14+ | `com.google.mediapipe:tasks-genai` |
| Gemma 4 E4B weights | `gemma-4-e4b-it` `.task` file | quantized int4, ~3GB |
| Test device | Pixel 7+/8/9 or Snapdragon 8 Gen 2+ | 8GB RAM minimum |
| Test device fallback | Any phone with 6GB RAM, accept higher latency | |

### Where to get Gemma 4 E4B for MediaPipe

```bash
# After Gemma 4 release on Kaggle / HF, the LiteRT .task bundle will be at:
# https://huggingface.co/google/gemma-4-E4B-it-litert (or similar)
# Download the .task file (~3GB int4 quantized)
# Push to device under /data/local/tmp/ then copy to app-private storage on first launch

adb push gemma-4-e4b-it-int4.task /data/local/tmp/gemma-4-e4b-it-int4.task
```

If Gemma 4 .task bundles aren't published yet at hackathon time, convert from HF weights using MediaPipe's `genai_bundler`:

```bash
pip install mediapipe==0.10.14
python -m mediapipe.tasks.python.genai.converter.llm_converter \
    --input_ckpt=./gemma-4-E4B-it \
    --ckpt_format=safetensors \
    --model_type=GEMMA4_4B \
    --backend=cpu \
    --output_dir=./out \
    --output_tflite_file=./gemma-4-e4b-it-int4.task \
    --vocab_model_file=./gemma-4-E4B-it/tokenizer.model \
    --quantization=int4
```

---

## 4. Project setup — exact commands

```bash
# Start fresh project
# Android Studio → New Project → Empty Activity → Compose
# Name: Dhwani
# Package: com.dhwani.app
# Min SDK: 26
# Build config: Kotlin DSL

# After project opens, run from project root:
mkdir -p app/src/main/assets/models
```

### `app/build.gradle.kts` — dependencies block

```kotlin
android {
    namespace = "com.dhwani.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dhwani.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a") }  // exclude x86 to keep APK lean
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // MediaPipe ships native .so files — avoid duplicate stripping
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // MediaPipe LLM Inference (Gemma 4 via LiteRT)
    implementation("com.google.mediapipe:tasks-genai:0.10.14")

    // On-device speech recognition — Vosk (offline, small Hindi/English models)
    implementation("com.alphacephei:vosk-android:0.3.47@aar")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Permission helpers
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
```

### `AndroidManifest.xml` — permissions and service

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Audio capture for caller voice via speaker loopback -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Read phone state so we know when a call starts/ends -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />
    <uses-permission android:name="android.permission.CALL_PHONE" />

    <!-- Foreground service of type microphone (required Android 14+) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

    <application
        android:name=".DhwaniApp"
        android:label="Dhwani"
        android:theme="@style/Theme.Dhwani"
        android:largeHeap="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".call.CallService"
            android:foregroundServiceType="microphone"
            android:exported="false" />

    </application>
</manifest>
```

---

## 5. Step-by-step build — what to do, in order

### Day 1, morning (4h) — Project scaffold + permissions

1. Create the project per section 4.
2. Add the manifest permissions.
3. Build the permission request screen (Compose):
   - `RECORD_AUDIO`
   - `READ_PHONE_STATE`
   - `POST_NOTIFICATIONS` (Android 13+)
4. Verify: app launches, asks for perms, lands on a blank "Ready" screen.

### Day 1, afternoon (4h) — Load Gemma 4 E4B via MediaPipe

1. Push the `.task` file to your test device:
   ```bash
   adb push gemma-4-e4b-it-int4.task /data/local/tmp/
   ```
2. On first app launch, copy from `/data/local/tmp/` (or assets, your choice — assets bloats APK) into `filesDir`.
3. Wire up the `LlmInference` instance.

**`GemmaEngine.kt`** — singleton, owns the LLM lifecycle.

```kotlin
package com.dhwani.app.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

object GemmaEngine {
    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null

    private val _tokens = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val tokens: SharedFlow<String> = _tokens

    fun init(context: Context) {
        if (llm != null) return
        val modelFile = File(context.filesDir, "gemma-4-e4b-it-int4.task")
        require(modelFile.exists()) { "Gemma model not found at ${modelFile.absolutePath}" }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(2048)
            .setMaxTopK(40)
            .build()

        llm = LlmInference.createFromOptions(context, options)

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTemperature(0.7f)
            .build()

        session = LlmInferenceSession.createFromOptions(llm!!, sessionOptions)
    }

    suspend fun generate(prompt: String): String {
        val s = session ?: error("Gemma not initialized")
        s.addQueryChunk(prompt)
        // For Phase 1 we use the blocking API; Phase 2 swaps to streaming.
        return s.generateResponse()
    }

    fun close() {
        session?.close(); session = null
        llm?.close(); llm = null
    }
}
```

**Verify:** call `GemmaEngine.init(this)` from `DhwaniApp.onCreate()`, then on a button tap run `val r = GemmaEngine.generate("Translate to Hindi: I am running late.")` and toast the result. You should see Hindi output within 2–4 seconds. If this works, Gemma 4 is alive on your device.

### Day 2, morning (4h) — Audio capture with echo cancellation

The hard part. When the phone is on speaker, the mic hears both the caller and our own TTS output. We need acoustic echo cancellation or the system loops.

**`SpeakerphoneRecorder.kt`**:

```kotlin
package com.dhwani.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SpeakerphoneRecorder {
    private var recorder: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private val pcmChannel = Channel<ShortArray>(Channel.BUFFERED)
    private val sampleRate = 16000  // Vosk + Whisper both want 16kHz

    @SuppressLint("MissingPermission")
    fun start() {
        // VOICE_COMMUNICATION source enables platform AEC and NS automatically
        // on most devices — this is THE flag that makes loopback work.
        val source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val bufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding) * 4

        recorder = AudioRecord(source, sampleRate, channelConfig, encoding, bufSize)

        // Belt-and-braces: explicit AEC + NS on top of the source flag
        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(recorder!!.audioSessionId).apply { enabled = true }
        } else {
            Log.w("Dhwani", "AEC unavailable on this device — echo likely")
        }
        if (NoiseSuppressor.isAvailable()) {
            ns = NoiseSuppressor.create(recorder!!.audioSessionId).apply { enabled = true }
        }

        recorder!!.startRecording()

        Thread {
            val buf = ShortArray(1600)  // 100ms at 16kHz
            while (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val n = recorder!!.read(buf, 0, buf.size)
                if (n > 0) pcmChannel.trySend(buf.copyOf(n))
            }
        }.start()
    }

    fun audio(): Flow<ShortArray> = pcmChannel.receiveAsFlow()

    fun stop() {
        recorder?.stop(); recorder?.release(); recorder = null
        aec?.release(); aec = null
        ns?.release(); ns = null
    }
}
```

**Verify:** start the recorder, dump the int16 PCM stream to a file (`/sdcard/Download/test.pcm`), pull it via `adb pull`, open in Audacity (Import Raw, 16kHz, mono, signed 16-bit PCM). You should hear yourself + room audio clearly.

### Day 2, afternoon (3h) — Wire up STT (Vosk, offline)

Vosk's offline Hindi + English models are small (~50MB each) and fast on-device. We're using Vosk as a *stopgap* for Phase 1; Phase 2 may swap to MediaPipe ASR or stay with Vosk if it works.

Download Vosk models:
- Hindi: `vosk-model-small-hi-0.22.zip` (~50MB)
- English: `vosk-model-small-en-in-0.4.zip` (~40MB)

Place under `app/src/main/assets/vosk-hi/` and `app/src/main/assets/vosk-en/` (or copy to filesDir on first launch — preferred for large assets).

**`SpeechToText.kt`**:

```kotlin
package com.dhwani.app.audio

import org.vosk.Model
import org.vosk.Recognizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

class SpeechToText(modelPath: String) {
    private val model = Model(modelPath)
    private val recognizer = Recognizer(model, 16000.0f)

    fun transcribeStream(pcm: Flow<ShortArray>): Flow<Transcript> = flow {
        pcm.collect { chunk ->
            val bytes = ByteArray(chunk.size * 2)
            for (i in chunk.indices) {
                bytes[i*2]     = (chunk[i].toInt() and 0xFF).toByte()
                bytes[i*2 + 1] = ((chunk[i].toInt() shr 8) and 0xFF).toByte()
            }
            val isFinal = recognizer.acceptWaveForm(bytes, bytes.size)
            val json = if (isFinal) recognizer.result else recognizer.partialResult
            val text = JSONObject(json).optString(if (isFinal) "text" else "partial", "")
            if (text.isNotBlank()) emit(Transcript(text, isFinal))
        }
    }.flowOn(Dispatchers.Default)

    data class Transcript(val text: String, val isFinal: Boolean)
}
```

**Verify:** put a YouTube Hindi news clip on a second device, hold your test phone near its speaker, watch transcripts appear in logcat in real time.

### Day 3, morning (3h) — TTS output

Use Android's built-in `TextToSpeech` for Phase 1 (free, on-device for Hindi/English on most Pixel devices). Phase 2 may swap to a higher-quality model.

**`TextToSpeechEngine.kt`**:

```kotlin
package com.dhwani.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TextToSpeechEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    suspend fun speak(text: String, lang: String = "en-IN") = suspendCoroutine<Unit> { cont ->
        if (!ready) { cont.resume(Unit); return@suspendCoroutine }
        tts?.language = Locale.forLanguageTag(lang)
        val id = System.currentTimeMillis().toString()
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { cont.resume(Unit) }
            @Deprecated("") override fun onError(utteranceId: String?) { cont.resume(Unit) }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }
}
```

**Audio routing — the critical detail:**

```kotlin
val am = getSystemService(AUDIO_SERVICE) as AudioManager
am.mode = AudioManager.MODE_IN_COMMUNICATION  // voice-call mode
am.isSpeakerphoneOn = true
```

`USAGE_VOICE_COMMUNICATION` + `MODE_IN_COMMUNICATION` routes TTS to the earpiece/speakerphone *in a way the call's mic on the other end will pick up cleanly*. Without this, your TTS gets dampened to a whisper on the counterparty's end.

### Day 3, afternoon (3h) — Foreground service + Compose UI

**`CallService.kt`**:

```kotlin
package com.dhwani.app.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channelId = "dhwani_call"
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Call in progress", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dhwani is listening")
            .setContentText("Captioning your call")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }
}
```

**`CallScreen.kt`** — minimal Compose UI:

```kotlin
@Composable
fun CallScreen(vm: CallViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top: live caption strip
        Text(
            state.liveCaption,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        )

        // Bottom: input row
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.draftReply,
                onValueChange = vm::onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type to speak…") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = vm::sendReply) { Text("Speak") }
        }
    }
}
```

### Day 3, end of day — End-to-end test

The acceptance test for Phase 1:

1. Open the app. Tap "Start". Grant permissions.
2. Open the phone dialer, call a friend or your own second number.
3. Press the phone's speaker button when the call connects.
4. Bring Dhwani back to foreground (it kept running in foreground service).
5. **Friend says "Hello, can you hear me?" in Hindi or English.**
6. **Within 1.2 seconds, the text "Hello can you hear me" appears as a caption.**
7. Type "Yes I can hear you. Please give me 5 minutes." into the input.
8. Hit "Speak."
9. **The friend hears Hindi/English TTS over the line, clearly.**

If those 9 steps work, Phase 1 is done.

---

## 6. Code structure for Phase 1

```
app/src/main/java/com/dhwani/app/
├── DhwaniApp.kt              # Application class — inits GemmaEngine
├── MainActivity.kt           # Single Activity, hosts Compose nav
├── ui/
│   ├── CallScreen.kt
│   ├── CallViewModel.kt
│   └── theme/                # Compose theme files
├── audio/
│   ├── SpeakerphoneRecorder.kt
│   ├── SpeechToText.kt
│   └── TextToSpeechEngine.kt
├── call/
│   └── CallService.kt        # foreground service
└── llm/
    └── GemmaEngine.kt        # MediaPipe LLM wrapper (idle in Phase 1)
```

---

## 7. Testing checklist — Phase 1 is "done" when

- [ ] App launches without crash on a Pixel 7 or equivalent
- [ ] All permissions granted via UI flow
- [ ] Gemma 4 E4B `.task` file loads — log line `"Gemma loaded in X ms"` appears, X < 8000
- [ ] One-shot prompt round-trip works: prompt in, response out, latency logged
- [ ] `SpeakerphoneRecorder` captures PCM with no echo when TTS is playing simultaneously (visual test in Audacity: TTS signal should be heavily attenuated in the recording)
- [ ] Vosk produces partial transcripts within 200ms of speech onset
- [ ] Vosk produces final transcripts within 800ms of utterance end
- [ ] `TextToSpeechEngine` plays Hindi and English audibly through speakerphone
- [ ] Foreground service notification stays visible across screen rotations and app backgrounding
- [ ] End-to-end manual call test passes (the 9-step scenario above)

---

## 8. Common failure modes and what to do

| Symptom | Cause | Fix |
|---|---|---|
| `LlmInference.createFromOptions` OOMs | E4B int4 needs ~4GB peak | Confirm `largeHeap=true`, drop `maxTokens` to 1024 |
| TTS sounds muffled to caller | Wrong audio mode | Set `AudioManager.MODE_IN_COMMUNICATION` *before* calling `speak()` |
| Mic captures own TTS (loop) | AEC not engaging | Use `VOICE_COMMUNICATION` source, not `MIC`. Test on a Pixel — Samsung's AEC is weaker. |
| Vosk says nothing | Sample rate mismatch | Confirm AudioRecord is 16000Hz, mono, PCM_16BIT |
| Foreground service killed on Android 14 | Missing `FOREGROUND_SERVICE_MICROPHONE` permission | Add to manifest + request at runtime |
| Gemma `.task` not found | Pushed to wrong dir | `adb push` to `/data/local/tmp/`, copy to `filesDir` in code |
| Latency >2s on cheap phone | Expected on non-flagship | Document target device clearly; demo on a Pixel |

---

## 9. Timeline — Phase 1 in 3 days

| Day | Morning | Afternoon |
|---|---|---|
| **Day 1** | Project scaffold, permissions, manifest | Load Gemma 4 E4B, verify single inference |
| **Day 2** | `SpeakerphoneRecorder` + AEC tuning | Vosk STT integration, verify Hindi + English transcription |
| **Day 3** | `TextToSpeechEngine` + audio routing | Foreground service, Compose UI, full E2E test on real call |

If a 4th day is needed, spend it on AEC tuning across 2–3 different phone models.

---

## 10. What this phase enables for Phase 2

Phase 2 will:
- Wrap each user turn in a Gemma 4 prompt that includes their stored personal context
- Stream Gemma 4 tokens via `LlmInferenceSession.generateResponseAsync` instead of the blocking call used here
- Add function calling so Gemma can pull context dynamically (address, doctor info, payment methods)
- Add a smart-reply suggestion strip above the input box during incoming calls

None of that is possible without Phase 1's audio pipeline working end-to-end. **Do not start Phase 2 until every checkbox in section 7 is ticked.**
