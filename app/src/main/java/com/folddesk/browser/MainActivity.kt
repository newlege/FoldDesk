package com.folddesk.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.folddesk.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val startPageUrl = "file:///android_asset/start.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureWebView(binding.webView)
        binding.webView.webViewClient = FoldDeskWebViewClient(
            onPageStarted = { syncAddressBar(it) },
            onPageFinished = { syncAddressBar(it) },
        )
        binding.webView.webChromeClient = android.webkit.WebChromeClient()

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

        if (savedInstanceState == null) {
            binding.webView.loadUrl(startPageUrl, DesktopUserAgent.extraHttpHeaders())
        }
    }

    private fun configureWebView(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
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

        applyDesktopClientHints(settings)
    }

    private fun applyDesktopClientHints(settings: WebSettings) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            return
        }
        val metadata = UserAgentMetadata.Builder()
            .setBrandVersionList(
                listOf(
                    brandVersion("Google Chrome", "131"),
                    brandVersion("Chromium", "131"),
                    brandVersion("Not_A Brand", "24"),
                ),
            )
            .setFullVersion("131.0.6778.69")
            .setPlatform("Windows")
            .setPlatformVersion("10.0.0")
            .setArchitecture("x86")
            .setModel("")
            .setMobile(false)
            .setBitness(64)
            .setWow64(false)
            .build()
        WebSettingsCompat.setUserAgentMetadata(settings, metadata)
    }

    private fun brandVersion(brand: String, major: String): UserAgentMetadata.BrandVersion =
        UserAgentMetadata.BrandVersion.Builder()
            .setBrand(brand)
            .setMajorVersion(major)
            .build()

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
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }
}
