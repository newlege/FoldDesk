package com.folddesk.browser

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewConfigurator {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        settings.userAgentString = DesktopUserAgent.USER_AGENT_STRING

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.textZoom = 100

        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        DesktopClientHintsApplier.applySafely(settings)
    }
}
