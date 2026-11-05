package com.folddesk.browser

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebView

/**
 * Ensures WebView is initialized safely before [MainActivity] inflates a WebView.
 */
class FoldDeskApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        warmUpWebViewProvider()
    }

    private fun warmUpWebViewProvider() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix("folddesk_browser")
            } catch (t: Throwable) {
                Log.w("FoldDeskApp", "WebView data directory suffix skipped", t)
            }
        }
        try {
            val pkg = WebView.getCurrentWebViewPackage()
            Log.i("FoldDeskApp", "WebView provider: ${pkg?.packageName ?: "unknown"}")
        } catch (t: Throwable) {
            Log.e("FoldDeskApp", "WebView provider unavailable", t)
        }
    }
}
