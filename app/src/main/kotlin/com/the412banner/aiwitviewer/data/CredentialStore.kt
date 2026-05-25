package com.the412banner.aiwitviewer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the email + password locally so the app can re-auth across restarts.
 * Stored in EncryptedSharedPreferences (AES-256-GCM, keys in Android Keystore).
 *
 * NOTE: We store the cleartext password because the AIWIT cloud requires it on
 * every login (no refresh-token flow). The vendor's app does the same.
 */
class CredentialStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val mk = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "aiwit_creds",
            mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var email: String?
        get() = prefs.getString("email", null)
        set(value) = prefs.edit().putString("email", value).apply()

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit().putString("password", value).apply()

    var appSn: String?
        get() = prefs.getString("app_sn", null)
        set(value) = prefs.edit().putString("app_sn", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = !email.isNullOrBlank() && !password.isNullOrBlank()
}
