package com.dhwani.app

import android.app.Application
import android.util.Log

class DhwaniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Dhwani app started")
    }

    companion object {
        private const val TAG = "DhwaniApp"
    }
}
