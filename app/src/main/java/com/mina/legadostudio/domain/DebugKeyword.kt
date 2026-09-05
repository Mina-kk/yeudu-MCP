package com.mina.legadostudio.domain

import com.google.gson.JsonParser
import com.mina.legadostudio.workflow.AutoDiscovery
import org.jsoup.Jsoup

object DebugKeyword {
    private val GENERIC = Regex("首页|分类|文学|名著|排行|完本|连载|书斋阁")

    fun searchKeyword(sourceJson: String, detailHtml: String, detailUrl: String): String {
        val root = runCatching { JsonParser.parseString(sourceJson).asJsonObject }.getOrNull()
        val check = root?.getAsJsonObject("ruleSearch")
            ?.get("checkKeyWord")
            ?.takeUnless { it.isJsonNull }
            ?.asString
            .orEmpty()
            .trim()
        if (usable(check)) return check
        val doc = Jsoup.parse(detailHtml, detailUrl)
        val og = doc.selectFirst("[property=og:novel:book_name]")?.attr("content").orEmpty().trim()
        if (usable(og)) return og
        if (AutoDiscovery.looksLikeBookPage(detailHtml, detailUrl)) {
            val h1 = doc.selectFirst("h1")?.text().orEmpty().trim()
            if (usable(h1)) return h1
        }
        return ""
    }

    private fun usable(value: String): Boolean = value.length in 2..40 && !GENERIC.containsMatchIn(value)
}
