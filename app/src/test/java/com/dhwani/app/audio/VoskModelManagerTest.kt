package com.dhwani.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VoskModelManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun modelRequiresEveryCoreFileToBeNonEmpty() {
        val model = temporaryFolder.newFolder("vosk-test")
        assertFalse(VoskModelManager.isUsableModel(model))

        listOf(
            "am/final.mdl",
            "conf/model.conf",
            "graph/Gr.fst",
            "graph/HCLr.fst",
        ).forEach { relativePath ->
            model.resolve(relativePath).apply {
                parentFile?.mkdirs()
                writeText("model data")
            }
        }

        assertTrue(VoskModelManager.isUsableModel(model))
        model.resolve("graph/Gr.fst").writeText("")
        assertFalse(VoskModelManager.isUsableModel(model))
    }
}
