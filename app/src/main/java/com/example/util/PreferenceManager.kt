package com.example.util

import android.content.Context

object PreferenceManager {
    private const val PREF_NAME = "MedBankPrefs"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_CNIC = "saved_cnic"

    fun saveRememberMe(context: Context, remember: Boolean, cnic: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, remember)
            if (remember) {
                putString(KEY_SAVED_CNIC, cnic)
            } else {
                remove(KEY_SAVED_CNIC)
            }
            apply()
        }
    }

    fun getRememberMe(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getSavedCnic(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAVED_CNIC, "") ?: ""
    }

    fun clearRememberMe(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, false)
            remove(KEY_SAVED_CNIC)
            apply()
        }
    }
}
