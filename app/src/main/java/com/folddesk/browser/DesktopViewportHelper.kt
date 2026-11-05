package com.folddesk.browser

import android.webkit.WebView

/**
 * Nudges pages toward desktop layout when sites ship mobile viewport/CSS.
 * Not a substitute for real Chromium layout engine differences.
 */
object DesktopViewportHelper {

    private const val INJECT_JS = """
        (function() {
          try {
            var meta = document.querySelector('meta[name="viewport"]');
            if (!meta) {
              meta = document.createElement('meta');
              meta.setAttribute('name', 'viewport');
              (document.head || document.documentElement).appendChild(meta);
            }
            var w = Math.max(window.innerWidth || 0, screen.width || 0, 1280);
            meta.setAttribute('content', 'width=' + w + ', initial-scale=1.0, minimum-scale=0.25, maximum-scale=3.0');
            if (document.documentElement) {
              document.documentElement.style.width = '100%';
            }
          } catch (e) {}
        })();
    """

    fun apply(webView: WebView) {
        webView.evaluateJavascript(INJECT_JS, null)
    }
}
