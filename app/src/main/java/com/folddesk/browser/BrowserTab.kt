package com.folddesk.browser

import android.webkit.WebView

data class BrowserTab(
    val id: Long,
    val webView: WebView,
    var title: String,
    val linkBridge: FoldDeskLinkBridge = FoldDeskLinkBridge(),
)
