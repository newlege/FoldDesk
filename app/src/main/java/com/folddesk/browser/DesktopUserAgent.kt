package com.folddesk.browser

/**
 * Desktop Chrome user agent and Client Hints metadata for WebView.
 *
 * WebView limits (documented for MVP):
 * - [android.webkit.WebSettings.userAgentString] replaces the default Android/mobile UA for
 *   subresource requests when set; we also inject the same string on main-frame loads.
 * - [androidx.webkit.WebSettingsCompat.setUserAgentMetadata] (API 29+, WebView 113+) sets
 *   Sec-CH-UA* hints and affects `navigator.userAgentData` when sites use Client Hints.
 * - `navigator.platform` is still often reported as "Linux armv8l" on Android WebView;
 *   there is no supported API to spoof it to "Win32". Sites relying only on UA / UA-CH should
 *   see desktop; legacy platform checks may still detect Android.
 */
object DesktopUserAgent {
    /** Windows desktop Chrome ~131 — keep in sync with verify-user-agent.sh */
    const val USER_AGENT_STRING: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"

    const val ACCEPT_LANGUAGE: String = "en-US,en;q=0.9"

    fun extraHttpHeaders(): Map<String, String> = mapOf(
        "Accept-Language" to ACCEPT_LANGUAGE,
    )
}
