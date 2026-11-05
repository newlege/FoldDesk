package com.folddesk.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.folddesk.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val startPageUrl = "file:///android_asset/start.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            configureWebView(binding.webView)
            binding.webView.webViewClient = FoldDeskWebViewClient(
                onPageStarted = { syncAddressBar(it) },
                onPageFinished = { syncAddressBar(it) },
            )
            binding.webView.webChromeClient = android.webkit.WebChromeClient()
        } catch (t: Throwable) {
            Log.e(TAG, "WebView setup failed", t)
            Toast.makeText(
                this,
                getString(R.string.error_webview_init),
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        binding.buttonBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.buttonForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.buttonReload.setOnClickListener { binding.webView.reload() }
        binding.buttonGo.setOnClickListener { navigateFromAddressBar() }
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
            try {
                binding.webView.restoreState(savedInstanceState)
            } catch (t: Throwable) {
                Log.w(TAG, "WebView state restore failed; loading start page", t)
                loadStartPage()
            }
        } else {
            loadStartPage()
        }
    }

    private fun loadStartPage() {
        try {
            binding.webView.loadUrl(startPageUrl, DesktopUserAgent.extraHttpHeaders())
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load start page", t)
            Toast.makeText(this, getString(R.string.error_webview_init), Toast.LENGTH_LONG).show()
        }
    }

    private fun configureWebView(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false

        settings.userAgentString = DesktopUserAgent.USER_AGENT_STRING

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.textZoom = 100

        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        DesktopClientHintsApplier.applySafely(settings)
    }

    private fun navigateFromAddressBar() {
        val raw = binding.editUrl.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return
        val url = normalizeUrl(raw)
        binding.webView.loadUrl(url, DesktopUserAgent.extraHttpHeaders())
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
        if (url == startPageUrl) {
            binding.editUrl.setText("")
            binding.editUrl.hint = getString(R.string.hint_home)
        } else {
            binding.editUrl.setText(url)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            binding.webView.saveState(outState)
        } catch (t: Throwable) {
            Log.w(TAG, "WebView state save failed", t)
        }
    }

    companion object {
        private const val TAG = "FoldDeskMain"
    }
}
