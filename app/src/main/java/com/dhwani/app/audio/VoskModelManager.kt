package com.dhwani.app.audio

import android.content.Context
import android.util.Log
import java.io.File

object VoskModelManager {
    private const val TAG = "VoskModelManager"
    private val MODEL_NAMES = listOf("vosk-en", "vosk-hi")

    fun prepareModels(context: Context) {
        MODEL_NAMES.forEach { modelName ->
            val target = File(context.filesDir, modelName)
            if (target.exists()) return@forEach
            runCatching {
                copyAssetDirectory(context, modelName, target)
            }.onSuccess {
                Log.i(TAG, "Copied $modelName to ${target.absolutePath}")
            }.onFailure {
                target.deleteRecursively()
                Log.i(TAG, "No bundled Vosk model found at assets/$modelName")
            }
        }
    }

    private fun copyAssetDirectory(context: Context, assetPath: String, target: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }

        target.mkdirs()
        children.forEach { child ->
            copyAssetDirectory(
                context = context,
                assetPath = "$assetPath/$child",
                target = File(target, child),
            )
        }
    }
}
