package com.folddesk.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopUserAgentTest {

    @Test
    fun userAgentLooksLikeDesktopChromeOnWindows() {
        val ua = DesktopUserAgent.USER_AGENT_STRING
        assertTrue(ua.contains("Windows NT 10.0"))
        assertTrue(ua.contains("Chrome/"))
        assertFalse(ua.contains("Android", ignoreCase = true))
        assertFalse(ua.contains("Mobile", ignoreCase = true))
    }

    @Test
    fun extraHeadersIncludeAcceptLanguage() {
        assertTrue(DesktopUserAgent.extraHttpHeaders().containsKey("Accept-Language"))
    }
}
