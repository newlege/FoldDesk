package com.folddesk.browser

import android.util.Log
import android.webkit.WebView

/**
 * Creates tabs and handles popup window WebView handoff for [WebChromeClient.onCreateWindow].
 */
class TabManager(
    private val host: TabHost,
) {

    interface TabHost {
        fun createConfiguredWebView(): WebView
        fun onTabAdded(tab: BrowserTab, select: Boolean)
        fun onTabSelected(tab: BrowserTab)
        fun onTabTitleChanged(tab: BrowserTab)
        fun showError(message: String)
    }

    private val tabs = mutableListOf<BrowserTab>()
    private var activeTabId: Long = -1L
    private var nextTabId = 1L

    fun allTabs(): List<BrowserTab> = tabs.toList()

    fun activeTab(): BrowserTab? = tabs.find { it.id == activeTabId }

    fun nextTabIdSeed(): Long = nextTabId

    fun restoreSeed(seed: Long) {
        nextTabId = seed
    }

    fun createTab(loadUrl: String?, select: Boolean = true): BrowserTab? {
        return try {
            Log.i(TAG, "createTab url=$loadUrl select=$select")
            val webView = host.createConfiguredWebView()
            val tab = BrowserTab(
                id = nextTabId++,
                webView = webView,
                title = "",
            )
            tabs.add(tab)
            host.onTabAdded(tab, select)
            if (loadUrl != null) {
                webView.loadUrl(loadUrl, DesktopUserAgent.extraHttpHeaders())
            }
            tab
        } catch (t: Throwable) {
            Log.e(TAG, "createTab failed", t)
            host.showError(t.message ?: "WebView error")
            null
        }
    }

    /**
     * Transport path only — caller must [selectTab] after child [WebViewClient.onPageStarted].
     */
    fun createTabForWindowHandoff(onHandoff: (WebView) -> Unit): BrowserTab? {
        return try {
            Log.i(TAG, "createTabForWindowHandoff begin")
            val webView = host.createConfiguredWebView()
            val tab = BrowserTab(
                id = nextTabId++,
                webView = webView,
                title = "",
            )
            tabs.add(tab)
            host.onTabAdded(tab, select = false)
            onHandoff(webView)
            Log.i(TAG, "createTabForWindowHandoff done tabId=${tab.id}")
            tab
        } catch (t: Throwable) {
            Log.e(TAG, "createTabForWindowHandoff failed", t)
            host.showError(t.message ?: "WebView error")
            null
        }
    }

    fun selectTab(tab: BrowserTab) {
        Log.i(TAG, "selectTab id=${tab.id}")
        activeTabId = tab.id
        host.onTabSelected(tab)
    }

    fun closeTab(tab: BrowserTab, onLastTabReset: (BrowserTab) -> Unit): BrowserTab? {
        if (tabs.size <= 1) {
            onLastTabReset(tab)
            return tab
        }
        val index = tabs.indexOf(tab)
        tabs.remove(tab)
        val next = tabs.getOrNull(index) ?: tabs.last()
        selectTab(next)
        return next
    }

    companion object {
        const val TAG = "FoldDeskTabs"
    }
}
