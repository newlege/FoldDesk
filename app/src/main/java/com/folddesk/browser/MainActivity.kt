package com.folddesk.browser

import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.folddesk.browser.databinding.ActivityMainBinding
import com.folddesk.browser.databinding.ItemTabBinding

class MainActivity : AppCompatActivity(), TabManager.TabHost {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabManager: TabManager
    private val startPageUrl = "file:///android_asset/start.html"
    private val popupLinkTracker = PopupLinkTracker()

    private val tabChipBindings = mutableMapOf<Long, ItemTabBinding>()

    /** Child tab waiting for first real navigation before UI switch (Transport path). */
    private var pendingPopupTabId: Long = -1L
    private var popupOpenerTabId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tabManager = TabManager(this)
        applyEdgeToEdgeInsets()

        binding.buttonBack.setOnClickListener { tabManager.activeTab()?.webView?.let { if (it.canGoBack()) it.goBack() } }
        binding.buttonForward.setOnClickListener { tabManager.activeTab()?.webView?.let { if (it.canGoForward()) it.goForward() } }
        binding.buttonReload.setOnClickListener { tabManager.activeTab()?.webView?.reload() }
        binding.buttonGo.setOnClickListener { navigateFromAddressBar() }
        binding.buttonNewTab.setOnClickListener { tabManager.createTab(startPageUrl) }
        binding.editUrl.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                navigateFromAddressBar()
                true
            } else {
                false
            }
        }

        if (savedInstanceState != null) {
            restoreTabs(savedInstanceState)
        } else {
            tabManager.createTab(startPageUrl)
        }
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.chromeRoot.setPadding(0, bars.top, 0, 0)
            binding.webViewContainer.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun createConfiguredWebView(): WebView {
        val webView = WebView(this)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.visibility = View.INVISIBLE
        WebViewConfigurator.configure(webView)
        return webView
    }

    override fun onTabAdded(tab: BrowserTab, select: Boolean) {
        if (tab.title.isEmpty()) {
            tab.title = getString(R.string.tab_default_title)
        }
        popupLinkTracker.attach(tab.webView)
        attachClients(tab)
        binding.webViewContainer.addView(
            tab.webView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        addTabChip(tab)
        if (select) {
            tabManager.selectTab(tab)
        } else {
            applyWebViewVisibility(tabManager.activeTab(), tab)
        }
    }

    override fun onTabSelected(tab: BrowserTab) {
        val active = tab
        tabManager.allTabs().forEach { t ->
            applyWebViewVisibility(active, t)
        }
        updateTabChipSelection(active.id)
        syncAddressBar(active.webView.url)
        binding.tabScroll.post {
            tabChipBindings[active.id]?.root?.let { chip ->
                binding.tabScroll.smoothScrollTo(chip.left, 0)
            }
        }
    }

    /**
     * Honor WebView: avoid GONE on opener during popup handoff; use INVISIBLE for background tabs.
     */
    private fun applyWebViewVisibility(active: BrowserTab?, tab: BrowserTab) {
        val webView = tab.webView
        when {
            active != null && tab.id == active.id -> {
                webView.visibility = View.VISIBLE
                webView.bringToFront()
            }
            pendingPopupTabId != -1L && tab.id == popupOpenerTabId -> {
                webView.visibility = View.VISIBLE
            }
            else -> {
                webView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onTabTitleChanged(tab: BrowserTab) {
        updateTabChipTitle(tab)
    }

    override fun showError(message: String) {
        Toast.makeText(this, getString(R.string.error_webview_init), Toast.LENGTH_LONG).show()
    }

    private fun attachClients(tab: BrowserTab) {
        tab.webView.webViewClient = FoldDeskWebViewClient(
            tab = tab,
            callbacks = webViewCallbacks,
        )
        tab.webView.webChromeClient = FoldDeskWebChromeClient(
            tab = tab,
            callbacks = chromeCallbacks,
        )
    }

    private val webViewCallbacks = object : FoldDeskWebViewClient.Callbacks {
        override fun onPageStarted(tab: BrowserTab, url: String?) {
            Log.d(TabManager.TAG, "onPageStarted tab=${tab.id} url=$url pendingPopup=$pendingPopupTabId")
            if (tab.id == pendingPopupTabId && isRealNavigation(url)) {
                completePopupHandoff(tab, url)
            }
            if (tabManager.activeTab()?.id == tab.id) syncAddressBar(url)
        }

        override fun onPageFinished(tab: BrowserTab, url: String?) {
            if (tab.id == pendingPopupTabId && isRealNavigation(url)) {
                completePopupHandoff(tab, url)
            }
            if (tabManager.activeTab()?.id == tab.id) syncAddressBar(url)
            updateTabChipTitle(tab)
        }

        override fun openUrlInNewTab(url: String): Boolean {
            tabManager.createTab(url)
            return true
        }
    }

    private val chromeCallbacks = object : FoldDeskWebChromeClient.Callbacks {
        override fun onTitleChanged(tab: BrowserTab, title: String?) {
            if (!title.isNullOrBlank()) {
                tab.title = title
                updateTabChipTitle(tab)
            }
        }

        override fun onCreateWindow(fromTab: BrowserTab, opener: WebView, resultMsg: Message): Boolean {
            return handleCreateWindow(fromTab, opener, resultMsg)
        }
    }

    private fun handleCreateWindow(openerTab: BrowserTab, opener: WebView, resultMsg: Message): Boolean {
        Log.i(TabManager.TAG, "onCreateWindow start openerTab=${openerTab.id}")
        return try {
            val directUrl = resolvePopupUrl(opener)
            if (directUrl != null) {
                Log.i(TabManager.TAG, "onCreateWindow hybrid direct tab url=$directUrl")
                popupLinkTracker.consumeFrom(opener)
                opener.post {
                    try {
                        tabManager.createTab(directUrl)
                    } catch (t: Throwable) {
                        Log.e(TabManager.TAG, "hybrid createTab failed", t)
                    }
                }
                return false
            }

            Log.i(TabManager.TAG, "onCreateWindow using WebViewTransport fallback")
            val transport = resultMsg.obj as? WebView.WebViewTransport
            if (transport == null) {
                Log.e(TabManager.TAG, "onCreateWindow: no WebViewTransport")
                return false
            }
            val childTab = tabManager.createTabForWindowHandoff { childWebView ->
                Log.i(TabManager.TAG, "onCreateWindow transport assign child tabId pending")
                transport.webView = childWebView
                resultMsg.sendToTarget()
            }
            if (childTab == null) {
                Log.e(TabManager.TAG, "onCreateWindow: child tab creation failed")
                false
            } else {
                pendingPopupTabId = childTab.id
                popupOpenerTabId = openerTab.id
                Log.i(TabManager.TAG, "onCreateWindow transport ok child=${childTab.id} opener=${openerTab.id}")
                true
            }
        } catch (t: Throwable) {
            Log.e(TabManager.TAG, "onCreateWindow fatal", t)
            clearPopupHandoff()
            false
        }
    }

    private fun completePopupHandoff(tab: BrowserTab, url: String?) {
        Log.i(TabManager.TAG, "completePopupHandoff tab=${tab.id} url=$url")
        clearPopupHandoff()
        tabManager.selectTab(tab)
    }

    private fun clearPopupHandoff() {
        pendingPopupTabId = -1L
        popupOpenerTabId = -1L
    }

    private fun resolvePopupUrl(opener: WebView): String? {
        popupLinkTracker.consumeFrom(opener)?.let { return normalizePopupUrl(it) }
        val hit = opener.hitTestResult
        if (hit != null) {
            when (hit.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                -> hit.extra?.let { return normalizePopupUrl(it) }
            }
        }
        return null
    }

    private fun normalizePopupUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("javascript:", ignoreCase = true)) return null
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        return null
    }

    private fun isRealNavigation(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (url == "about:blank") return false
        return true
    }

    private fun addTabChip(tab: BrowserTab) {
        val chipBinding = ItemTabBinding.inflate(LayoutInflater.from(this), binding.tabContainer, false)
        chipBinding.tabTitle.text = tab.title
        chipBinding.root.setOnClickListener { tabManager.selectTab(tab) }
        chipBinding.tabClose.setOnClickListener { closeTab(tab) }
        tabChipBindings[tab.id] = chipBinding
        binding.tabContainer.addView(chipBinding.root)
    }

    private fun updateTabChipTitle(tab: BrowserTab) {
        tabChipBindings[tab.id]?.tabTitle?.text = tab.title
    }

    private fun updateTabChipSelection(activeId: Long) {
        tabChipBindings.forEach { (id, chip) ->
            chip.root.isSelected = id == activeId
        }
    }

    private fun closeTab(tab: BrowserTab) {
        if (tab.id == pendingPopupTabId || tab.id == popupOpenerTabId) {
            clearPopupHandoff()
        }
        tabManager.closeTab(tab) { last ->
            last.webView.loadUrl(startPageUrl, DesktopUserAgent.extraHttpHeaders())
            last.title = getString(R.string.tab_default_title)
            updateTabChipTitle(last)
            syncAddressBar(startPageUrl)
        }
        if (tabManager.allTabs().none { it.id == tab.id }) {
            tabChipBindings.remove(tab.id)?.root?.let { binding.tabContainer.removeView(it) }
            binding.webViewContainer.removeView(tab.webView)
            tab.webView.destroy()
        }
    }

    private fun navigateFromAddressBar() {
        val tab = tabManager.activeTab() ?: return
        val raw = binding.editUrl.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return
        tab.webView.loadUrl(AddressBarNavigation.resolve(raw), DesktopUserAgent.extraHttpHeaders())
    }

    private fun syncAddressBar(url: String?) {
        if (url.isNullOrBlank()) return
        if (url == startPageUrl || url == "about:blank") {
            binding.editUrl.setText("")
            binding.editUrl.hint = getString(R.string.hint_home)
        } else {
            binding.editUrl.setText(url)
            binding.editUrl.hint = getString(R.string.hint_url)
        }
    }

    private fun restoreTabs(savedInstanceState: Bundle) {
        tabManager.restoreSeed(savedInstanceState.getLong(STATE_NEXT_TAB_ID, 1L))
        val urls = savedInstanceState.getStringArrayList(STATE_TAB_URLS)
        val titles = savedInstanceState.getStringArrayList(STATE_TAB_TITLES)
        if (urls.isNullOrEmpty()) {
            tabManager.createTab(startPageUrl)
            return
        }
        urls.forEachIndexed { index, url ->
            val tab = tabManager.createTab(null, select = index == 0) ?: return
            tab.title = titles?.getOrNull(index) ?: tab.title
            updateTabChipTitle(tab)
            val bundle = savedInstanceState.getBundle(webViewStateKey(index))
            val restored = bundle != null && tab.webView.restoreState(bundle) != null
            if (!restored) {
                tab.webView.loadUrl(url, DesktopUserAgent.extraHttpHeaders())
            }
        }
        val activeId = savedInstanceState.getLong(STATE_ACTIVE_TAB_ID, tabManager.allTabs().first().id)
        tabManager.allTabs().find { it.id == activeId }?.let { tabManager.selectTab(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val tabs = tabManager.allTabs()
        outState.putLong(STATE_NEXT_TAB_ID, tabManager.nextTabIdSeed())
        outState.putLong(STATE_ACTIVE_TAB_ID, tabManager.activeTab()?.id ?: -1L)
        outState.putStringArrayList(STATE_TAB_URLS, ArrayList(tabs.map { it.webView.url ?: startPageUrl }))
        outState.putStringArrayList(STATE_TAB_TITLES, ArrayList(tabs.map { it.title }))
        tabs.forEachIndexed { index, tab ->
            try {
                val b = Bundle()
                tab.webView.saveState(b)
                outState.putBundle(webViewStateKey(index), b)
            } catch (t: Throwable) {
                Log.w(TabManager.TAG, "saveState failed for tab $index", t)
            }
        }
    }

    private fun webViewStateKey(index: Int) = "$STATE_WEBVIEW_PREFIX$index"

    companion object {
        private const val STATE_NEXT_TAB_ID = "nextTabId"
        private const val STATE_ACTIVE_TAB_ID = "activeTabId"
        private const val STATE_TAB_URLS = "tabUrls"
        private const val STATE_TAB_TITLES = "tabTitles"
        private const val STATE_WEBVIEW_PREFIX = "webViewState_"
    }
}
