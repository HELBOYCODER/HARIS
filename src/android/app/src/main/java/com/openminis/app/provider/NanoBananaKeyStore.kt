package com.openminis.app.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for Nano Banana (Gemini) API keys (multiple per provider)
 * Uses Android Keystore + EncryptedSharedPreferences
 */
object NanoBananaKeyStore {

    private const val TAG = "NanoBananaKeyStore"
    private const val PREFS_NAME = "nano_banana_prefs"
    private const val KEY_PREFIX = "gemini_api_key_" // providerId suffix

    private var prefs: SharedPreferences? = null

    /** Initialize the secure storage */
    fun init(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            Log.d(TAG, "NanoBananaKeyStore initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize secure storage", e)
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /** Store a Gemini API key for a provider (adds to the list) */
    fun storeApiKey(context: Context, providerId: String, apiKey: String): Boolean {
        if (prefs == null) init(context)
        return try {
            val setKey = KEY_PREFIX + providerId
            val existing = prefs?.getStringSet(setKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            existing.add(apiKey)
            prefs?.edit()?.putStringSet(setKey, existing)?.apply()
            Log.d(TAG, "Stored API key for $providerId, total=${existing.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store API key", e)
            false
        }
    }

    /** Retrieve all stored keys for a provider */
    fun getApiKeys(providerId: String): List<String> {
        val setKey = KEY_PREFIX + providerId
        return prefs?.getStringSet(setKey, null)?.toList() ?: emptyList()
    }

    /** Get the current (rotated) key for a provider */
    fun getCurrentKey(providerId: String): String? {
        val keys = getApiKeys(providerId)
        if (keys.isEmpty()) return null
        val indexKey = "${KEY_PREFIX}index_$providerId"
        val idx = prefs?.getInt(indexKey, 0) ?: 0
        return keys[idx % keys.size]
    }

    /** Rotate to the next key (call on rate‑limit) */
    fun rotateKey(providerId: String) {
        val keys = getApiKeys(providerId)
        if (keys.isEmpty()) return
        val indexKey = "${KEY_PREFIX}index_$providerId"
        val current = prefs?.getInt(indexKey, 0) ?: 0
        val next = (current + 1) % keys.size
        prefs?.edit()?.putInt(indexKey, next)?.apply()
        Log.d(TAG, "Rotated API key for $providerId to index $next")
    }

    /** Delete all keys for a provider */
    fun deleteAllKeys(providerId: String): Boolean {
        return try {
            val setKey = KEY_PREFIX + providerId
            val indexKey = "${KEY_PREFIX}index_$providerId"
            prefs?.edit()?.remove(setKey)?.remove(indexKey)?.apply()
            Log.d(TAG, "Deleted all API keys for $providerId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete API keys", e)
            false
        }
    }

    /** Simple validation of a key */
    fun isValidKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        return key.startsWith("AI") || key.startsWith("sk-")
    }
}
