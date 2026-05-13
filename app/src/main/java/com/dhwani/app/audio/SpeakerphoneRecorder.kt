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
    fun start() {
        if (recorder != null) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
        val bufferSize = (minBufferSize.coerceAtLeast(SAMPLE_RATE / 10)) * 4

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            channelConfig,
            encoding,
            bufferSize,
        )
        recorder = audioRecord

        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply { enabled = true }
        } else {
            Log.w(TAG, "AEC unavailable on this device")
        }
        if (NoiseSuppressor.isAvailable()) {
            ns = NoiseSuppressor.create(audioRecord.audioSessionId)?.apply { enabled = true }
        }

        audioRecord.startRecording()
        readThread = Thread(::readLoop, "dhwani-audio-record").apply { start() }
    }

    fun audio(): Flow<ShortArray> = pcmChannel.receiveAsFlow()

    fun stop() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        aec?.release()
        aec = null
        ns?.release()
        ns = null
        readThread = null
    }

    private fun readLoop() {
        val buffer = ShortArray(SAMPLE_RATE / 10)
        while (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = recorder?.read(buffer, 0, buffer.size) ?: break
            if (read > 0) pcmChannel.trySend(buffer.copyOf(read))
        }
    }

    companion object {
        private const val TAG = "SpeakerphoneRecorder"
        const val SAMPLE_RATE = 16_000
    }
}
