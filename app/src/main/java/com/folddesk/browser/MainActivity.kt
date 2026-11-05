package com.folddesk.browser

import android.annotation.SuppressLint
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val startPageUrl = "file:///android_asset/start.html"

    private val tabs = mutableListOf<BrowserTab>()
    private var activeTabId: Long = -1L
    private var nextTabId = 1L
    private val tabChipBindings = mutableMapOf<Long, ItemTabBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets()

        binding.buttonBack.setOnClickListener { activeTab()?.webView?.let { if (it.canGoBack()) it.goBack() } }
        binding.buttonForward.setOnClickListener { activeTab()?.webView?.let { if (it.canGoForward()) it.goForward() } }
        binding.buttonReload.setOnClickListener { activeTab()?.webView?.reload() }
        binding.buttonGo.setOnClickListener { navigateFromAddressBar() }
        binding.buttonNewTab.setOnClickListener { createTab(startPageUrl) }
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
            createTab(startPageUrl)
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

    private fun createTab(loadUrl: String?): BrowserTab? {
        val webView = try {
            createConfiguredWebView()
        } catch (t: Throwable) {
            Log.e(TAG, "WebView create failed", t)
            Toast.makeText(this, getString(R.string.error_webview_init), Toast.LENGTH_LONG).show()
            return null
        }
        val tab = BrowserTab(
            id = nextTabId++,
            webView = webView,
            title = getString(R.string.tab_default_title),
        )
        tabs.add(tab)
        attachClients(tab)
        binding.webViewContainer.addView(
            webView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        addTabChip(tab)
        showTab(tab)
        if (loadUrl != null) {
            try {
                webView.loadUrl(loadUrl, DesktopUserAgent.extraHttpHeaders())
            } catch (t: Throwable) {
                Log.e(TAG, "loadUrl failed", t)
            }
        }
        return tab
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(): WebView {
        val webView = WebView(this)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.visibility = View.GONE
        WebViewConfigurator.configure(webView)
        return webView
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
            if (tab.id == activeTabId) syncAddressBar(url)
        }

        override fun onPageFinished(tab: BrowserTab, url: String?) {
            if (tab.id == activeTabId) syncAddressBar(url)
            updateTabChipTitle(tab)
        }

        override fun openUrlInNewTab(url: String): Boolean {
            createTab(url)
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

        override fun onCreateWindow(fromTab: BrowserTab, resultMsg: Message): Boolean {
            val newTab = createTab(null) ?: return false
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = newTab.webView
            resultMsg.sendToTarget()
            return true
        }
    }

    private fun addTabChip(tab: BrowserTab) {
        val chipBinding = ItemTabBinding.inflate(LayoutInflater.from(this), binding.tabContainer, false)
        chipBinding.tabTitle.text = tab.title
        chipBinding.root.setOnClickListener { showTab(tab) }
        chipBinding.tabClose.setOnClickListener { closeTab(tab) }
        tabChipBindings[tab.id] = chipBinding
        binding.tabContainer.addView(chipBinding.root)
        updateTabChipSelection()
    }

    private fun updateTabChipTitle(tab: BrowserTab) {
        tabChipBindings[tab.id]?.tabTitle?.text = tab.title
    }

    private fun updateTabChipSelection() {
        tabChipBindings.forEach { (id, chip) ->
            chip.root.isSelected = id == activeTabId
        }
    }

    private fun showTab(tab: BrowserTab) {
        activeTabId = tab.id
        tabs.forEach { t ->
            t.webView.visibility = if (t.id == tab.id) View.VISIBLE else View.GONE
        }
        updateTabChipSelection()
        syncAddressBar(tab.webView.url)
        binding.tabScroll.post {
            tabChipBindings[tab.id]?.root?.let { chip ->
                binding.tabScroll.smoothScrollTo(chip.left, 0)
            }
        }
    }

    private fun closeTab(tab: BrowserTab) {
        if (tabs.size <= 1) {
            tab.webView.loadUrl(startPageUrl, DesktopUserAgent.extraHttpHeaders())
            tab.title = getString(R.string.tab_default_title)
            updateTabChipTitle(tab)
            syncAddressBar(startPageUrl)
            return
        }
        val index = tabs.indexOf(tab)
        tabChipBindings.remove(tab.id)?.root?.let { binding.tabContainer.removeView(it) }
        binding.webViewContainer.removeView(tab.webView)
        tab.webView.destroy()
        tabs.remove(tab)
        val next = tabs.getOrNull(index) ?: tabs.last()
        showTab(next)
    }

    private fun activeTab(): BrowserTab? = tabs.find { it.id == activeTabId }

    private fun navigateFromAddressBar() {
        val tab = activeTab() ?: return
        val raw = binding.editUrl.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return
        tab.webView.loadUrl(normalizeUrl(raw), DesktopUserAgent.extraHttpHeaders())
    }

    private fun normalizeUrl(input: String): String {
        if (input.startsWith("file:") || input.startsWith("javascript:")) {
            return input
        }
        return when {
            input.contains(" ") -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.startsWith("localhost") || input.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$")) ->
                "http://$input"
            else -> "https://$input"
        }
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
        nextTabId = savedInstanceState.getLong(STATE_NEXT_TAB_ID, 1L)
        val urls = savedInstanceState.getStringArrayList(STATE_TAB_URLS)
        val titles = savedInstanceState.getStringArrayList(STATE_TAB_TITLES)
        if (urls.isNullOrEmpty()) {
            createTab(startPageUrl)
            return
        }
        urls.forEachIndexed { index, url ->
            val tab = createTab(null) ?: return
            tab.title = titles?.getOrNull(index) ?: tab.title
            updateTabChipTitle(tab)
            val bundle = savedInstanceState.getBundle(webViewStateKey(index))
            val restored = bundle != null && tab.webView.restoreState(bundle) != null
            if (!restored) {
                tab.webView.loadUrl(url, DesktopUserAgent.extraHttpHeaders())
            }
        }
        val activeId = savedInstanceState.getLong(STATE_ACTIVE_TAB_ID, tabs.first().id)
        tabs.find { it.id == activeId }?.let { showTab(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_NEXT_TAB_ID, nextTabId)
        outState.putLong(STATE_ACTIVE_TAB_ID, activeTabId)
        outState.putStringArrayList(STATE_TAB_URLS, ArrayList(tabs.map { it.webView.url ?: startPageUrl }))
        outState.putStringArrayList(STATE_TAB_TITLES, ArrayList(tabs.map { it.title }))
        tabs.forEachIndexed { index, tab ->
            try {
                val b = Bundle()
                tab.webView.saveState(b)
                outState.putBundle(webViewStateKey(index), b)
            } catch (t: Throwable) {
                Log.w(TAG, "saveState failed for tab $index", t)
            }
        }
    }

    private fun webViewStateKey(index: Int) = "$STATE_WEBVIEW_PREFIX$index"

    companion object {
        private const val TAG = "FoldDeskMain"
        private const val STATE_NEXT_TAB_ID = "nextTabId"
        private const val STATE_ACTIVE_TAB_ID = "activeTabId"
        private const val STATE_TAB_URLS = "tabUrls"
        private const val STATE_TAB_TITLES = "tabTitles"
        private const val STATE_WEBVIEW_PREFIX = "webViewState_"
    }
}
