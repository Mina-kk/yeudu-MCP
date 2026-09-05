package com.mina.legadostudio.domain

import com.mina.legadostudio.runtime.LegadoStringRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegadoStringRuleTest {
    @Test fun splitsFieldSelectorFromRegexReplacement() {
        val part = LegadoStringRule.split("h1@text##广告-##").single()
        assertEquals("h1@text", part.rule)
        assertEquals("广告-", part.replaceRegex)
        assertEquals("", part.replacement)
        assertEquals(false, part.replaceFirst)
    }

    @Test fun leadingHashesMeanReplaceCurrentResultWithoutExtracting() {
        val part = LegadoStringRule.split("##广告尾巴##").single()
        assertEquals("", part.rule)
        assertEquals("广告尾巴", part.replaceRegex)
        assertEquals("", part.replacement)
        assertEquals("前半章后半章", LegadoStringRule.replace("前半章广告尾巴后半章", part))
    }

    @Test fun documentedTripleHashExtractsFirstMatchOnly() {
        val part = LegadoStringRule.split("##\\d+##数字\$0###").single()
        assertTrue(part.replaceFirst)
        assertEquals("数字123", LegadoStringRule.replace("abc123def456", part))
    }

    @Test fun trailingHashSetAlsoEnablesReplaceFirst() {
        val part = LegadoStringRule.split("name##广告##\$1##").single()
        assertEquals("name", part.rule)
        assertEquals(true, part.replaceFirst)
        assertEquals("\$1", part.replacement)
    }

    @Test fun replaceFirstWithoutMatchReturnsEmptyNotOriginal() {
        val part = LegadoStringRule.split("##zzz##$0###").single()
        assertEquals("", LegadoStringRule.replace("nothing here", part))
    }

    @Test fun atJsKeepsCodeBeforeHashHashAsJavascript() {
        val part = LegadoStringRule.split("@js:result.replace(/广告/g,'')##尾巴##").single()
        assertEquals(LegadoStringRule.Mode.Js, part.mode)
        assertEquals("result.replace(/广告/g,'')", part.rule)
        assertEquals("尾巴", part.replaceRegex)
    }

    @Test fun jsBlockThenReplacementAreTwoParts() {
        val parts = LegadoStringRule.split("<js>String(result)</js>##广告##")
        assertEquals(2, parts.size)
        assertEquals(LegadoStringRule.Mode.Js, parts[0].mode)
        assertEquals("String(result)", parts[0].rule)
        assertEquals("", parts[1].rule)
        assertEquals("广告", parts[1].replaceRegex)
    }

    @Test fun unescapesHtmlEntitiesLikeOfficialGetString() {
        assertEquals("书名&作者", LegadoStringRule.unescapeHtml("书名&amp;作者"))
    }
}
