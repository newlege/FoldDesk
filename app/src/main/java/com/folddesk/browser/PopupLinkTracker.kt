package com.folddesk.browser

import android.util.Log
import android.view.MotionEvent
import android.webkit.WebView

/**
 * Remembers the last link URL the user tapped (for target=_blank without Transport).
 */
class PopupLinkTracker {

    private var lastUrl: String? = null
    private var lastWebView: WebView? = null

    fun attach(webView: WebView) {
        webView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.post { record(v as WebView) }
            }
            false
        }
    }

    fun record(webView: WebView) {
        try {
            val hit = webView.hitTestResult ?: return
            if (hit.type != WebView.HitTestResult.SRC_ANCHOR_TYPE &&
                hit.type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                return
            }
            val url = hit.extra
            if (url.isNullOrBlank()) return
            PopupNavigationUrl.acceptOrLog("hit-test-record", url)?.let { accepted ->
                lastUrl = accepted
                lastWebView = webView
            }
        } catch (t: Throwable) {
            Log.w(TabManager.TAG, "record link tap failed", t)
        }
    }

    fun consumeFrom(webView: WebView): String? {
        val url = lastUrl
        if (url == null || lastWebView !== webView) return null
        lastUrl = null
        lastWebView = null
        return url
    }
}
