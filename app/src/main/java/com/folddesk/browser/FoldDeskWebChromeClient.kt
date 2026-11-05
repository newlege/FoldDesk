package com.folddesk.browser

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView

class FoldDeskWebChromeClient(
    private val tab: BrowserTab,
    private val callbacks: Callbacks,
) : WebChromeClient() {

    interface Callbacks {
        fun onTitleChanged(tab: BrowserTab, title: String?)
        fun onCreateWindow(fromTab: BrowserTab, resultMsg: Message): Boolean
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            tab.title = title
            callbacks.onTitleChanged(tab, title)
        }
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {
        if (resultMsg == null) return false
        return callbacks.onCreateWindow(tab, resultMsg)
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        callbacks.onTitleChanged(tab, tab.title)
    }
}
