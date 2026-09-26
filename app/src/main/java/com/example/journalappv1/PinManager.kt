package com.example.journalappv1

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PinManager {
    private const val PREFS_NAME = "JournalAppSecurity"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_ENABLED = "pin_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun removePin(context: Context) {
        prefs(context).edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val saltStr = p.getString(KEY_SALT, null) ?: return false
        val hashStr = p.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        val expected = Base64.decode(hashStr, Base64.NO_WRAP)
        return hash(pin, salt).contentEquals(expected)
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }
}