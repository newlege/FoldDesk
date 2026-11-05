package com.folddesk.browser

import android.util.Log
import android.webkit.WebSettings
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Applies desktop Client Hints when the system WebView supports them.
 * Never throws — falls back to [WebSettings.userAgentString] only (set by caller).
 */
object DesktopClientHintsApplier {

    private const val TAG = "FoldDeskClientHints"

    fun applySafely(settings: WebSettings) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            Log.i(TAG, "USER_AGENT_METADATA not supported; using UA string only")
            return
        }
        try {
            applyMetadata(settings)
            Log.i(TAG, "Desktop User-Agent Client Hints applied")
        } catch (t: Throwable) {
            Log.w(TAG, "Client Hints failed; using UA string only", t)
        }
    }

    private fun applyMetadata(settings: WebSettings) {
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
            .setMobile(false)
            .build()
        WebSettingsCompat.setUserAgentMetadata(settings, metadata)
    }

    private fun brandVersion(brand: String, major: String): UserAgentMetadata.BrandVersion =
        UserAgentMetadata.BrandVersion.Builder()
            .setBrand(brand)
            .setMajorVersion(major)
            .build()
}
