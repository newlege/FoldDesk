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
     * Popup path for target=_blank / window.open.
     * Does not switch away from the opener until after WebViewTransport handoff completes.
     */
    fun createTabForWindowHandoff(onHandoff: (WebView) -> Unit): BrowserTab? {
        return try {
            val webView = host.createConfiguredWebView()
            val tab = BrowserTab(
                id = nextTabId++,
                webView = webView,
                title = "",
            )
            tabs.add(tab)
            host.onTabAdded(tab, select = false)
            onHandoff(webView)
            webView.post {
                try {
                    selectTab(tab)
                } catch (t: Throwable) {
                    Log.e(TAG, "select tab after handoff failed", t)
                }
            }
            tab
        } catch (t: Throwable) {
            Log.e(TAG, "createTabForWindowHandoff failed", t)
            host.showError(t.message ?: "WebView error")
            null
        }
    }

    fun selectTab(tab: BrowserTab) {
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

    fun indexOf(tab: BrowserTab): Int = tabs.indexOf(tab)

    companion object {
        const val TAG = "FoldDeskTabs"
    }
}
