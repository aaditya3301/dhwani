package com.dhwani.app.data

import android.content.Context
import android.content.SharedPreferences

class OnboardingStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    fun reset() {
        prefs.edit().remove(KEY_COMPLETED).apply()
    }

    companion object {
        private const val PREFS_NAME = "dhwani_onboarding_prefs"
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
