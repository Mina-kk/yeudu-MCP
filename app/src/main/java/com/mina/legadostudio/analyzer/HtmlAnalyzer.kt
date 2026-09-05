package com.mina.legadostudio.analyzer

import org.jsoup.Jsoup

class HtmlAnalyzer {
    data class Candidate(val selector: String, val count: Int, val sample: String)
    data class Report(
        val title: String,
        val language: String,
        val links: Int,
        val images: Int,
        val forms: Int,
        val candidates: Map<String, List<Candidate>>,
    )

    fun analyze(html: String, baseUrl: String): Report {
        val doc = Jsoup.parse(html, baseUrl)
        return Report(
            title = doc.title(),
            language = doc.selectFirst("html")?.attr("lang").orEmpty(),
            links = doc.select("a[href]").size,
            images = doc.select("img[src]").size,
            forms = doc.select("form").size,
            candidates = mapOf(
                "bookList" to candidates(doc, listOf(".book-card", ".book-item", ".list-item", "li:has(a[href*=book])")),
                "bookName" to candidates(doc, listOf("h1", ".book-name", ".bookname", "[property=og:novel:book_name]")),
                "author" to candidates(doc, listOf(".author", "[property=og:novel:author]", "*:matchesOwn(作者[:：])")),
                "toc" to candidates(doc, listOf("#list a", ".chapter-list a", ".chapterList a", "a[href*=chapter]")),
                "content" to candidates(doc, listOf("#content", ".content", ".read-content", ".chapter-content", "article")),
            )
        )
    }

    fun testSelector(html: String, baseUrl: String, selector: String): List<Map<String, String>> {
        val doc = Jsoup.parse(html, baseUrl)
        return doc.select(selector).take(50).map { element ->
            mapOf(
                "text" to element.text().take(500),
                "html" to element.outerHtml().take(1_500),
                "href" to element.absUrl("href"),
                "src" to element.absUrl("src"),
            )
        }
    }

    private fun candidates(doc: org.jsoup.nodes.Document, selectors: List<String>): List<Candidate> =
        selectors.mapNotNull { selector ->
            runCatching {
                val elements = doc.select(selector)
                if (elements.isEmpty()) null else Candidate(selector, elements.size, elements.first()!!.text().take(240))
            }.getOrNull()
        }
}
