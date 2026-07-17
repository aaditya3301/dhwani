package com.dhwani.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureJsonStore(
    context: Context,
    prefsName: String,
    private val keyAlias: String,
) {
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun readObject(key: String): JSONObject? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            JSONObject(decrypt(raw))
        }.getOrNull()
    }

    fun readArray(key: String): org.json.JSONArray? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            org.json.JSONArray(decrypt(raw))
        }.getOrNull()
    }

    fun writeObject(key: String, value: JSONObject) {
        prefs.edit().putString(key, encrypt(value.toString())).apply()
    }

    fun writeArray(key: String, value: org.json.JSONArray) {
        prefs.edit().putString(key, encrypt(value.toString())).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        return payload.toString()
    }

    private fun decrypt(payload: String): String {
        val json = JSONObject(payload)
        val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(json.getString("value"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
