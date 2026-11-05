package com.folddesk.browser

import android.net.Uri
import android.util.Log

object PopupNavigationUrl {

    private val SUBRESOURCE_EXTENSIONS = setOf(
        ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".bmp", ".avif",
        ".woff", ".woff2", ".ttf", ".eot", ".otf",
        ".css", ".js", ".mjs", ".map",
        ".mp3", ".mp4", ".webm", ".ogg", ".wav",
        ".pdf", ".zip", ".rar",
    )

    fun isDocumentNavigation(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("javascript:", ignoreCase = true)) return false
        if (trimmed.startsWith("mailto:", ignoreCase = true)) return false
        if (trimmed.startsWith("tel:", ignoreCase = true)) return false
        if (trimmed.startsWith("data:", ignoreCase = true)) return false

        val path = runCatching { Uri.parse(trimmed).path?.lowercase() }.getOrNull()
        if (path.isNullOrEmpty()) {
            return true
        }
        if (SUBRESOURCE_EXTENSIONS.any { path.endsWith(it) }) {
            return false
        }
        return true
    }

    fun acceptOrLog(source: String, raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (!isDocumentNavigation(raw)) {
            Log.i(TabManager.TAG, "reject popup url ($source): $raw")
            return null
        }
        Log.i(TabManager.TAG, "accept popup url ($source): $raw")
        return raw
    }
}
