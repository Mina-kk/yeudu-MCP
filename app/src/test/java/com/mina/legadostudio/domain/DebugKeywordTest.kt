package com.mina.legadostudio.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugKeywordTest {
    @Test fun prefersCheckKeyWord() {
        val json = """{"ruleSearch":{"checkKeyWord":"万相之王"}}"""
        assertEquals("万相之王", DebugKeyword.searchKeyword(json, "<h1>古典文学</h1>", "https://www.shuzhaige.com/gudianwenxue/"))
    }

    @Test fun doesNotUseCategoryTitle() {
        val html = "<html><title>古典文学-书斋阁</title><h1>古典文学</h1></html>"
        assertEquals("", DebugKeyword.searchKeyword("{}", html, "https://www.shuzhaige.com/gudianwenxue/"))
    }

    @Test fun usesOgNovelBookName() {
        val html = "<html><meta property='og:novel:book_name' content='万相之王'><h1>万相之王</h1></html>"
        assertEquals("万相之王", DebugKeyword.searchKeyword("{}", html, "https://www.shuzhaige.com/wanxiangzhiwang/"))
    }
}
