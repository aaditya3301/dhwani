package com.dhwani.app.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object GemmaEngine {
    private const val TAG = "GemmaEngine"
    private const val MODEL_NAME = "gemma3-1b-it-int4.task"

    private var llm: LlmInference? = null
    private var lastError: String? = null
    private val generationMutex = Mutex()

    val status: String
        get() = when {
            llm != null -> "Gemma loaded"
            lastError != null -> "Gemma unavailable: $lastError"
            else -> "Gemma not loaded"
        }

    val isReady: Boolean
        get() = llm != null

    fun prepareModel(context: Context): File {
        val target = File(context.filesDir, MODEL_NAME)
        if (isUsableModel(target)) return target

        val staging = File(context.filesDir, "$MODEL_NAME.part")
        target.delete()
        staging.delete()

        runCatching {
            context.assets.open("models/$MODEL_NAME").use { input ->
                staging.outputStream().use { output -> input.copyTo(output) }
            }
            check(isUsableModel(staging)) { "Bundled Gemma model is incomplete" }
            check(staging.renameTo(target)) { "Could not activate the Gemma model" }
        }.onSuccess {
            Log.i(TAG, "Copied Gemma model to ${target.absolutePath}")
        }.onFailure { error ->
            staging.delete()
            target.delete()
            Log.w(TAG, "Could not prepare Gemma model: ${error.message}")
        }
        return target
    }

    @Synchronized
    fun init(context: Context) {
        if (llm != null) return
        val modelFile = prepareModel(context)
        if (!isUsableModel(modelFile)) {
            lastError = "Put $MODEL_NAME in app filesDir or app/src/main/assets/models"
            return
        }

        val start = System.currentTimeMillis()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(2048)
            .setMaxTopK(40)
            .build()

        try {
            llm = LlmInference.createFromOptions(context, options)
            lastError = null
            Log.i(TAG, "Gemma loaded in ${System.currentTimeMillis() - start} ms")
        } catch (error: Throwable) {
            lastError = error.message ?: error.javaClass.simpleName
            throw error
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        generationMutex.withLock {
            val activeLlm = llm ?: error("Gemma not initialized")
            activeLlm.generateResponse(prompt)
        }
    }

    @Synchronized
    fun close() {
        llm?.close()
        llm = null
    }

    private fun isUsableModel(file: File): Boolean {
        return file.isFile && file.length() >= MIN_MODEL_BYTES
    }

    private const val MIN_MODEL_BYTES = 100L * 1024L * 1024L
}
