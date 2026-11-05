package com.folddesk.browser

import android.webkit.JavascriptInterface
import android.util.Log

/** Records anchor clicks from injected JS (prefers real href over hit-test image src). */
class FoldDeskLinkBridge {

    @Volatile
    private var lastBlankHref: String? = null

    @JavascriptInterface
    fun notifyAnchorClick(href: String, target: String) {
        val t = target.trim()
        val isBlankTarget = t.equals("_blank", ignoreCase = true) || t.equals("blank", ignoreCase = true)
        if (!isBlankTarget) return
        PopupNavigationUrl.acceptOrLog("js-bridge", href)?.let { lastBlankHref = it }
    }

    fun consumeBlankHref(): String? = lastBlankHref?.also { lastBlankHref = null }
}
