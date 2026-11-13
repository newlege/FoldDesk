package com.folddesk.browser

import android.content.Context

/**
 * Persists user-facing browser preferences (custom start page).
 */
class BrowserSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun defaultStartPageUrl(): String = DEFAULT_START_PAGE

    fun customStartPageUrl(): String? = prefs.getString(KEY_START_PAGE, null)?.takeIf { it.isNotBlank() }

    fun startPageUrl(): String = customStartPageUrl() ?: DEFAULT_START_PAGE

    fun setCustomStartPageUrl(url: String?) {
        prefs.edit().apply {
            if (url.isNullOrBlank()) {
                remove(KEY_START_PAGE)
            } else {
                putString(KEY_START_PAGE, url.trim())
            }
        }.apply()
    }

    fun isStartPageDisplayUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val start = startPageUrl()
        if (url == start) return true
        if (customStartPageUrl() == null && url.endsWith("/start.html")) return true
        return false
    }

    companion object {
        const val DEFAULT_START_PAGE = "file:///android_asset/start.html"
        private const val PREFS_NAME = "folddesk_browser"
        private const val KEY_START_PAGE = "custom_start_page"
    }
}
