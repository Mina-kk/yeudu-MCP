package com.mina.legadostudio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BookSourceRuleSanitizerTest {
    @Test fun removesAcceptEncodingFromGeneratedSourceHeader() {
        val json = """{"header":"{\"User-Agent\":\"UA\",\"Accept-Encoding\":\"gzip, deflate\"}"}"""
        val root = com.google.gson.JsonParser.parseString(BookSourceRuleSanitizer.sanitizeJson(json)).asJsonObject
        val header = com.google.gson.JsonParser.parseString(root.get("header").asString).asJsonObject
        assertEquals("UA", header.get("User-Agent").asString)
        assertFalse(header.has("Accept-Encoding"))
    }

    @Test fun stripsRepeatedCssPrefixAfterOr() {
        val raw = "@css:.m-box ul a[href\$='.html']||@css:ul a[href\$='.html']"
        assertEquals("@css:.m-box ul a[href\$='.html']||ul a[href\$='.html']", BookSourceRuleSanitizer.normalizeCssOr(raw))
    }

    @Test fun dropsNextChapterAsContentPagination() {
        val json = """{"ruleContent":{"content":"#content@html","nextContentUrl":"text.下一章@href"}}"""
        val cleaned = com.google.gson.JsonParser.parseString(BookSourceRuleSanitizer.sanitizeJson(json)).asJsonObject
        org.junit.Assert.assertTrue(cleaned.getAsJsonObject("ruleContent").get("nextContentUrl") == null)
    }

    @Test fun sanitizesChapterListInsideSourceJson() {
        val json = """{"ruleToc":{"chapterList":"@css:.m-box ul a||@css:#play_0 a"}}"""
        val cleaned = BookSourceRuleSanitizer.sanitizeJson(json)
        assertFalse(cleaned.contains("||@css:"))
        assertEquals("@css:.m-box ul a||#play_0 a", com.google.gson.JsonParser.parseString(cleaned).asJsonObject.getAsJsonObject("ruleToc").get("chapterList").asString)
    }
}
