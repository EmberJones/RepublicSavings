package com.example.republicsavingsapp

import android.util.Log

/**
 * Single place for app logging. Every message uses the tag "RepublicSavings", so you can
 * filter Logcat by that word.
 * Safe to call from unit tests: android.util.Log isn't available
 * on the plain JVM, so any failure to log is ignored there.
 */
object AppLog {
    private const val TAG = "RepublicSavings"

    fun d(area: String, message: String) {
        runCatching { Log.d(TAG, "[$area] $message") }
    }

    fun w(area: String, message: String) {
        runCatching { Log.w(TAG, "[$area] $message") }
    }

    fun e(area: String, message: String, error: Throwable? = null) {
        runCatching { Log.e(TAG, "[$area] $message", error) }
    }
}