package com.folddesk.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressBarNavigationTest {

    @Test
    fun keywordsUseBaiduSearch() {
        val url = AddressBarNavigation.resolve("上海 一网通办")
        assertTrue(url.startsWith("https://www.baidu.com/s?wd="))
    }

    @Test
    fun domainGetsHttps() {
        assertEquals("https://example.com", AddressBarNavigation.resolve("example.com"))
        assertEquals("https://zwdtuser.sh.gov.cn/", AddressBarNavigation.resolve("zwdtuser.sh.gov.cn/"))
    }

    @Test
    fun explicitSchemeUnchanged() {
        assertEquals("https://httpbin.org/headers", AddressBarNavigation.resolve("https://httpbin.org/headers"))
    }

    @Test
    fun localhostAndIpv4UseHttp() {
        assertEquals("http://localhost:8080", AddressBarNavigation.resolve("localhost:8080"))
        assertEquals("http://192.168.1.1/path", AddressBarNavigation.resolve("192.168.1.1/path"))
    }

    @Test
    fun singleWordIsSearchNotUrl() {
        assertTrue(AddressBarNavigation.isSearchQuery("baidu"))
    }
}
