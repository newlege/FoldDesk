package com.folddesk.browser

import android.content.Context

class BookmarkStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun all(): List<BookmarkEntry> =
        BookmarkEntry.decodeList(prefs.getString(KEY_BOOKMARKS, null))

    fun saveAll(entries: List<BookmarkEntry>) {
        prefs.edit().putString(KEY_BOOKMARKS, BookmarkEntry.encodeList(entries)).apply()
    }

    fun add(entry: BookmarkEntry) {
        val list = all().toMutableList()
        val index = list.indexOfFirst { urlsMatch(it.url, entry.url) }
        if (index >= 0) {
            list[index] = entry.copy(title = entry.title.ifEmpty { list[index].title })
        } else {
            list.add(entry)
        }
        saveAll(list)
    }

    fun remove(url: String) {
        saveAll(all().filterNot { urlsMatch(it.url, url) })
    }

    fun contains(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return all().any { urlsMatch(it.url, url) }
    }

    companion object {
        private const val PREFS_NAME = "folddesk_browser"
        private const val KEY_BOOKMARKS = "bookmarks"

        fun urlsMatch(a: String, b: String): Boolean {
            return normalizeUrlKey(a) == normalizeUrlKey(b)
        }

        fun normalizeUrlKey(url: String): String {
            return url.trim().trimEnd('/').lowercase()
        }

        fun faviconUrlFor(pageUrl: String): String {
            val encoded = java.net.URLEncoder.encode(pageUrl, Charsets.UTF_8.name())
            return "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$encoded&size=32"
        }
    }
}
