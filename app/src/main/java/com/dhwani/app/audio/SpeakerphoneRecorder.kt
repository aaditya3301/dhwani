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
    private var readThread: Thread? = null
    private val pcmChannel = Channel<ShortArray>(Channel.BUFFERED)

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start() {
        if (recorder != null) return
        while (pcmChannel.tryReceive().isSuccess) Unit

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
        check(minBufferSize > 0) { "16 kHz mono recording is unavailable on this device" }
        val bufferSize = minBufferSize.coerceAtLeast(SAMPLE_RATE / 10) * 4

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            channelConfig,
            encoding,
            bufferSize,
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            error("Could not initialize the phone microphone")
        }

        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply { enabled = true }
            } else {
                Log.w(TAG, "AEC unavailable on this device")
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioRecord.audioSessionId)?.apply { enabled = true }
            }

            audioRecord.startRecording()
            check(audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "The phone microphone did not start recording"
            }
            recorder = audioRecord
            readThread = Thread({ readLoop(audioRecord) }, "dhwani-audio-record").apply { start() }
        } catch (error: Throwable) {
            releaseEffects()
            audioRecord.release()
            throw error
        }
    }

    fun audio(): Flow<ShortArray> = pcmChannel.receiveAsFlow()

    @Synchronized
    fun stop() {
        val activeRecorder = recorder ?: return
        recorder = null
        runCatching { activeRecorder.stop() }
        readThread?.let { thread ->
            if (thread !== Thread.currentThread()) {
                runCatching { thread.join(READ_THREAD_JOIN_MS) }
            }
        }
        readThread = null
        releaseEffects()
        activeRecorder.release()
    }

    private fun readLoop(audioRecord: AudioRecord) {
        val buffer = ShortArray(SAMPLE_RATE / 10)
        while (recorder === audioRecord &&
            audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
        ) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read > 0) {
                pcmChannel.trySend(buffer.copyOf(read))
            } else if (read < 0) {
                Log.w(TAG, "AudioRecord read failed with code $read")
                break
            }
        }
    }

    private fun releaseEffects() {
        aec?.release()
        aec = null
        ns?.release()
        ns = null
    }

    companion object {
        private const val TAG = "SpeakerphoneRecorder"
        private const val READ_THREAD_JOIN_MS = 500L
        const val SAMPLE_RATE = 16_000
    }
}
