package com.folddesk.browser

import org.json.JSONArray
import org.json.JSONObject

data class BookmarkEntry(
    val url: String,
    val title: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("title", title)
    }

    companion object {
        fun fromJson(obj: JSONObject): BookmarkEntry? {
            val url = obj.optString("url", "").trim()
            if (url.isEmpty()) return null
            val title = obj.optString("title", "").trim().ifEmpty { url }
            return BookmarkEntry(url = url, title = title)
        }

        fun decodeList(raw: String?): List<BookmarkEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        fromJson(arr.getJSONObject(i))?.let { add(it) }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeList(entries: List<BookmarkEntry>): String {
            val arr = JSONArray()
            entries.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}
