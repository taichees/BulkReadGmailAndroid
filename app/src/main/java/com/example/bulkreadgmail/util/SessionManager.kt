package com.example.bulkreadgmail.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "session_prefs_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(userId: String, accessToken: String, refreshToken: String?, expiresIn: Long) {
        val expiryDate = System.currentTimeMillis() + expiresIn * 1000
        prefs.edit().apply {
            putString("user_id", userId)
            putString("access_token", accessToken)
            if (refreshToken != null) {
                putString("refresh_token", refreshToken)
            }
            putLong("expiry_date", expiryDate)
            apply()
        }
    }

    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getExpiryDate(): Long = prefs.getLong("expiry_date", 0)

    fun updateAccessToken(accessToken: String, expiresIn: Long) {
        val expiryDate = System.currentTimeMillis() + expiresIn * 1000
        prefs.edit().apply {
            putString("access_token", accessToken)
            putLong("expiry_date", expiryDate)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getUserId() != null
}
