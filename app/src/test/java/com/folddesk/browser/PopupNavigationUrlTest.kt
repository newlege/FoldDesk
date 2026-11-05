package com.folddesk.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupNavigationUrlTest {

    @Test
    fun rejectsCommonImageAssets() {
        assertFalse(PopupNavigationUrl.isDocumentNavigation("https://scgj.sh.gov.cn/img/round_1.png"))
        assertFalse(PopupNavigationUrl.isDocumentNavigation("https://example.com/icon.svg"))
    }

    @Test
    fun acceptsHtmlLikePages() {
        assertTrue(PopupNavigationUrl.isDocumentNavigation("https://scgj.sh.gov.cn/xxgk/index.html"))
        assertTrue(PopupNavigationUrl.isDocumentNavigation("https://scgj.sh.gov.cn/path/page"))
    }
}
