package com.miniplay.app.core.analytics

import android.util.Log
import com.miniplay.app.BuildConfig

/**
 * Local analytics abstraction. v1 only logs to Logcat in debug builds — nothing
 * leaves the device (MiniPlay is offline). A real sink (Firebase, etc.) can
 * replace [LogcatAnalytics] behind this interface later.
 */
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    fun logScreen(screen: String)
}

class LogcatAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        if (BuildConfig.DEBUG) Log.d(TAG, "event=$name ${params.entries.joinToString()}")
    }

    override fun logScreen(screen: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "screen=$screen")
    }

    private companion object {
        const val TAG = "MiniPlayAnalytics"
    }
}
