package com.dhwani.app.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GemmaEngine {
    private const val TAG = "GemmaEngine"
    private const val MODEL_NAME = "gemma-4-e4b-it-int4.task"

    private var llm: LlmInference? = null
    private var lastError: String? = null

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
        if (target.exists()) return target

        runCatching {
            context.assets.open("models/$MODEL_NAME").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.onSuccess {
            Log.i(TAG, "Copied Gemma model to ${target.absolutePath}")
        }.onFailure {
            Log.i(TAG, "No bundled Gemma model found at assets/models/$MODEL_NAME")
        }
        return target
    }

    fun init(context: Context) {
        if (llm != null) return
        val modelFile = prepareModel(context)
        if (!modelFile.exists()) {
            lastError = "Put $MODEL_NAME in app filesDir or app/src/main/assets/models"
            return
        }

        val start = System.currentTimeMillis()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(2048)
            .setMaxTopK(40)
            .build()

        llm = LlmInference.createFromOptions(context, options)
        lastError = null
        Log.i(TAG, "Gemma loaded in ${System.currentTimeMillis() - start} ms")
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        val activeLlm = llm ?: error("Gemma not initialized")
        activeLlm.generateResponse(prompt)
    }

    fun close() {
        llm?.close()
        llm = null
    }
}
