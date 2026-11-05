package com.folddesk.browser

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Resolves address bar input to either a navigable URL or Baidu keyword search.
 */
object AddressBarNavigation {

    private val IPV4_HOST = Regex(
        "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$",
    )

    private val DOMAIN_HOST = Regex(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)+$",
    )

    fun resolve(input: String): String {
        val raw = input.trim()
        if (raw.isEmpty()) return raw

        if (raw.startsWith("file:", ignoreCase = true) ||
            raw.startsWith("javascript:", ignoreCase = true)
        ) {
            return raw
        }

        if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true)
        ) {
            return raw
        }

        if (raw.startsWith("localhost", ignoreCase = true) || looksLikeIpv4Url(raw)) {
            return "http://$raw"
        }

        if (looksLikeHostUrl(raw)) {
            return "https://$raw"
        }

        return baiduSearchUrl(raw)
    }

    fun baiduSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return "https://www.baidu.com/s?wd=$encoded"
    }

    /** True when [resolve] would perform a Baidu search rather than direct navigation. */
    fun isSearchQuery(input: String): Boolean {
        val raw = input.trim()
        if (raw.isEmpty()) return false
        val resolved = resolve(raw)
        return resolved.startsWith("https://www.baidu.com/s?wd=", ignoreCase = true)
    }

    private fun looksLikeIpv4Url(raw: String): Boolean {
        val hostPort = raw.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = hostPort.substringBefore(':')
        return IPV4_HOST.matches(host)
    }

    private fun looksLikeHostUrl(raw: String): Boolean {
        if (raw.contains(' ')) return false
        val authority = raw.substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isEmpty() || !authority.contains('.')) return false
        val host = authority.substringBefore(':')
        return DOMAIN_HOST.matches(host)
    }
}
