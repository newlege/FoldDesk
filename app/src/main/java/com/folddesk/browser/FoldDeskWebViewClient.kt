package com.folddesk.browser

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Ensures every navigation and subresource request carries desktop headers / UA override.
 */
class FoldDeskWebViewClient(
    private val onPageStarted: (String?) -> Unit = {},
    private val onPageFinished: (String?) -> Unit = {},
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) {
            return false
        }
        val url = request.url.toString()
        view.loadUrl(url, DesktopUserAgent.extraHttpHeaders())
        return true
    }

    @Deprecated("Deprecated in API 24+")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        view.loadUrl(url, DesktopUserAgent.extraHttpHeaders())
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(url)
    }
}
