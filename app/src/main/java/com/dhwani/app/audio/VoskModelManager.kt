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
            if (isUsableModel(target)) return@forEach

            val staging = File(context.filesDir, "$modelName.part")
            target.deleteRecursively()
            staging.deleteRecursively()
            runCatching {
                copyAssetDirectory(context, modelName, staging)
                check(isUsableModel(staging)) { "Bundled $modelName model is incomplete" }
                check(staging.renameTo(target)) { "Could not activate $modelName model" }
            }.onSuccess {
                Log.i(TAG, "Copied $modelName to ${target.absolutePath}")
            }.onFailure { error ->
                staging.deleteRecursively()
                target.deleteRecursively()
                Log.w(TAG, "Could not prepare $modelName: ${error.message}")
            }
        }
    }

    fun isUsableModel(directory: File): Boolean {
        return REQUIRED_MODEL_FILES.all { relativePath ->
            File(directory, relativePath).let { it.isFile && it.length() > 0L }
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

    private val REQUIRED_MODEL_FILES = listOf(
        "am/final.mdl",
        "conf/model.conf",
        "graph/Gr.fst",
        "graph/HCLr.fst",
    )
}
