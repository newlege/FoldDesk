package com.folddesk.browser

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class FoldDeskWebViewClient(
    private val tab: BrowserTab,
    private val callbacks: Callbacks,
) : WebViewClient() {

    interface Callbacks {
        fun onPageStarted(tab: BrowserTab, url: String?)
        fun onPageFinished(tab: BrowserTab, url: String?)
        fun openUrlInNewTab(url: String): Boolean
    }

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
        callbacks.onPageStarted(tab, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view != null) {
            DesktopViewportHelper.apply(view)
        }
        callbacks.onPageFinished(tab, url)
    }
}
