package com.mina.legadostudio.domain

import com.mina.legadostudio.network.HttpFetcher
import com.mina.legadostudio.workflow.AutoDiscovery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDiscoveryTest {
    @Test fun prefersBookHomeOverNumericChapterHtml() {
        assertTrue(AutoDiscovery.scoreDetail("https://www.shuzhaige.com/wanxiangzhiwang/", "万相之王") >
            AutoDiscovery.scoreDetail("https://www.shuzhaige.com/wanxiangzhiwang/152957.html", "万相之王"))
        assertTrue(AutoDiscovery.scoreDetail("https://www.shuzhaige.com/wanxiangzhiwang/152957.html", "第一章") < 0)
    }

    @Test fun treatsNumericHtmlAsChapterAndWalksToBookHome() {
        assertTrue(AutoDiscovery.looksLikeChapter("<html><title>万相之王-第一章</title></html>", "https://www.shuzhaige.com/wanxiangzhiwang/152957.html"))
        assertEquals("https://www.shuzhaige.com/wanxiangzhiwang/", AutoDiscovery.bookHomeFromChapter("https://www.shuzhaige.com/wanxiangzhiwang/152957.html"))
        assertTrue(!AutoDiscovery.looksLikeChapter("<html><meta property='og:novel:book_name' content='万相之王'></html>", "https://www.shuzhaige.com/wanxiangzhiwang/"))
    }

    @Test fun reclassifiesChapterCandidateToBookHome() = runBlocking {
        val pages = mapOf(
            "https://site.test/" to "<a href='/book/152957.html'>第一章 开端</a><a href='/book/'>万相之王</a>",
            "https://site.test/book/" to "<html><meta property='og:novel:book_name' content='万相之王'><h1>万相之王</h1><a href='/book/152957.html'>第一章</a></html>",
            "https://site.test/book/152957.html" to "<html><title>第一章 开端</title><div id='content'>正文</div></html>",
        )
        val discovery = AutoDiscovery.forUrls { url ->
            val html = pages[url] ?: error("unexpected $url")
            HttpFetcher.FetchResult(200, url, emptyMap(), html, 1)
        }.discover("https://site.test/")
        assertEquals("https://site.test/book/", discovery.pages.first { it.role == "详情" }.url)
        assertTrue(discovery.pages.any { it.role == "正文" && it.url.endsWith("152957.html") })
    }

    @Test fun doesNotTreatSiteRootAsBookDetail() {
        assertEquals(0, AutoDiscovery.scoreDetail("https://www.shuzhaige.com/", "书斋阁"))
        assertTrue(AutoDiscovery.sameSiteHome("https://www.shuzhaige.com/", "https://www.shuzhaige.com"))
        assertTrue(AutoDiscovery.pathDepth("https://www.shuzhaige.com/") == 0)
        assertTrue(AutoDiscovery.pathDepth("https://www.shuzhaige.com/wanxiangzhiwang/") >= 1)
    }

    @Test fun skipsHomepageBrandLinkWhenDiscovering() = runBlocking {
        val pages = mapOf(
            "https://www.shuzhaige.com/" to """
                <a class="navbar-brand" href="https://www.shuzhaige.com/">书斋阁</a>
                <a href="/wanxiangzhiwang/">万相之王</a>
            """.trimIndent(),
            "https://www.shuzhaige.com/wanxiangzhiwang/" to "<html><meta property='og:novel:book_name' content='万相之王'><a href='/wanxiangzhiwang/152957.html'>第一章</a></html>",
            "https://www.shuzhaige.com/wanxiangzhiwang/152957.html" to "<html><title>第一章</title><div id='content'>正文</div></html>",
        )
        val discovery = AutoDiscovery.forUrls { url ->
            val html = pages[url] ?: error("unexpected $url")
            HttpFetcher.FetchResult(200, url, emptyMap(), html, 1)
        }.discover("https://www.shuzhaige.com/")
        assertEquals("https://www.shuzhaige.com/wanxiangzhiwang/", discovery.pages.first { it.role == "详情" }.url)
    }

    @Test fun skipsNavGenreAndPicksBookHomeFromShuzhaigeShape() = runBlocking {
        val home = """
            <nav class="navbar">
              <a class="navbar-brand" href="https://www.shuzhaige.com/">书斋阁</a>
              <ul class="nav navbar-nav">
                <li><a href="https://www.shuzhaige.com/gudianwenxue/">古典文学</a></li>
                <li><a href="https://www.shuzhaige.com/shijiemingzhu/">世界名著</a></li>
                <li><a href="https://www.shuzhaige.com/writer/">作家</a></li>
              </ul>
            </nav>
            <a class="doc-info-imglink" href="https://www.shuzhaige.com/wanxiangzhiwang/"><img alt="万相之王"></a>
            <a href="https://www.shuzhaige.com/wanxiangzhiwang/152957.html">开始阅读</a>
            <a href="https://www.shuzhaige.com/wanxiangzhiwang/">目录</a>
        """.trimIndent()
        val book = """
            <html><head>
              <meta property="og:novel:book_name" content="万相之王">
              <meta property="og:novel:author" content="天蚕土豆">
            </head><body>
              <h1>万相之王</h1><p>作者：天蚕土豆</p>
              <div id="play_0"><ul>
                <li><a href="https://www.shuzhaige.com/wanxiangzhiwang/152957.html">第一章 我有三个相宫</a></li>
                <li><a href="https://www.shuzhaige.com/wanxiangzhiwang/152958.html">第二章</a></li>
              </ul></div>
            </body></html>
        """.trimIndent()
        val category = """
            <html><title>古典文学-书斋阁</title><h1>古典文学</h1>
            <a href="/1889/">红楼梦</a><a href="/2190/">西游记</a><a href="/3127/">水浒传</a>
            <a href="/3129/">三国演义</a><a href="/a/">A</a><a href="/b/">B</a>
            <a href="/c/">C</a><a href="/d/">D</a><a href="/e/">E</a>
        """.trimIndent()
        val pages = mapOf(
            "https://www.shuzhaige.com/" to home,
            "https://www.shuzhaige.com/gudianwenxue/" to category,
            "https://www.shuzhaige.com/shijiemingzhu/" to category,
            "https://www.shuzhaige.com/writer/" to category,
            "https://www.shuzhaige.com/wanxiangzhiwang/" to book,
            "https://www.shuzhaige.com/wanxiangzhiwang/152957.html" to "<html><title>万相之王-第一章</title><div id='content'>大夏国正文正文正文</div></html>",
        )
        val discovery = AutoDiscovery.forUrls { url ->
            val html = pages[url] ?: error("unexpected $url")
            HttpFetcher.FetchResult(200, url, emptyMap(), html, 1)
        }.discover("https://www.shuzhaige.com/")
        assertEquals("https://www.shuzhaige.com/wanxiangzhiwang/", discovery.pages.first { it.role == "详情" }.url)
        assertTrue(discovery.pages.any { it.role == "正文" && it.url.endsWith("152957.html") })
        assertTrue(discovery.pages.none { it.role == "详情" && it.url.contains("gudianwenxue") })
    }

    @Test fun classifiesGenreListAndBookHome() {
        val category = "<html><title>古典文学-书斋阁</title><h1>古典文学</h1>" +
            (1..10).joinToString("") { "<a href='/book$it/'>书$it</a>" }
        assertTrue(AutoDiscovery.looksLikeCategory(category, "https://www.shuzhaige.com/gudianwenxue/"))
        assertTrue(AutoDiscovery.looksLikeBookPage(
            "<html><meta property='og:novel:book_name' content='万相之王'><div id='play_0'><a href='/w/1.html'>一</a></div></html>",
            "https://www.shuzhaige.com/wanxiangzhiwang/",
        ))
        assertEquals(0, AutoDiscovery.scoreDetail("https://www.shuzhaige.com/gudianwenxue/", "古典文学", inNav = true))
    }

    @Test fun classifiesBiqugeHexChapterPageAsChapter() {
        val url = "http://www.biquge.pro/book/203433/e03ba8cc025f5.html"
        assertTrue(AutoDiscovery.looksLikeChapter("", url))
        assertTrue(AutoDiscovery.looksLikeChapter(
            "<html><body><article id='chapterContent' class='read-article'><section class='read-section jsChapterWrapper'><p>正文</p></section></article></body></html>",
            "http://other.test/x.html",
        ))
        assertTrue(AutoDiscovery.scoreDetail(url, "116.天牢对门") < 0)
        assertTrue(AutoDiscovery.looksLikeChapter("", "https://www.shuzhaige.com/wanxiangzhiwang/") == false)
    }

    @Test fun tocAnchorOnSamePageIsIgnored() = runBlocking {
        val chapter = "<html><title>第一章 试炼</title><div id='chapterContent'><p>" + "正".repeat(60) + "</p></div>" +
            "<a href='#&asideChapter'>目录</a></html>"
        val bookHome = "<html><meta property='og:novel:book_name' content='试炼之书'><h1>试炼之书</h1>" +
            "<a href='/book/1/aaaa1111bbbb.html'>第一章 试炼</a></html>"
        val pages = mapOf(
            "https://site.test/" to "<a href='/book/1/aaaa1111bbbb.html'>第一章 试炼</a>",
            "https://site.test/book/1/" to bookHome,
            "https://site.test/book/1/aaaa1111bbbb.html" to chapter,
        )
        val discovery = AutoDiscovery.forUrls { url ->
            val html = pages[url] ?: error("unexpected $url")
            HttpFetcher.FetchResult(200, url, emptyMap(), html, 1)
        }.discover("https://site.test/")
        assertEquals("https://site.test/book/1/", discovery.pages.first { it.role == "详情" }.url)
        assertTrue(discovery.pages.any { it.role == "正文" && it.url.endsWith("aaaa1111bbbb.html") })
        assertTrue(discovery.pages.none { it.url.contains('#') })
    }

    @Test fun listDirNumericHtmlIsNotChapterAndScoresLow() {
        assertFalse(AutoDiscovery.looksLikeChapter("", "http://www.biquge.pro/lists/41.html"))
        assertTrue(AutoDiscovery.looksLikeChapter("", "https://www.shuzhaige.com/wanxiangzhiwang/152957.html"))
        assertTrue(AutoDiscovery.scoreDetail("http://www.biquge.pro/lists/41.html", "热门小说") < 0)
    }

    @Test fun contentSelectionSkipsNumericListPageAndUsesRealChapter() = runBlocking {
        val detail = """
            <html><head><meta property='og:novel:book_name' content='测试书'></head><body>
              <a href='/lists/42.html'>玄幻小说</a>
              <div class='row row-section'><a href='/book/8513/f92d69f8575bf.html'>第1章</a></div>
            </body></html>
        """.trimIndent()
        val pages = mapOf(
            "https://site.test/" to "<a href='/novel/8516.html'>测试书</a>",
            "https://site.test/novel/8516.html" to detail,
            "https://site.test/book/8513/f92d69f8575bf.html" to "<article id='chapterContent'><p>${"正文".repeat(30)}</p></article>",
        )
        val discovery = AutoDiscovery.forUrls { url ->
            HttpFetcher.FetchResult(200, url, emptyMap(), pages[url] ?: error("unexpected $url"), 1)
        }.discover("https://site.test/")
        assertEquals("https://site.test/book/8513/f92d69f8575bf.html", discovery.pages.first { it.role == "正文" }.url)
    }

    @Test fun fragmentHomeLinkIsNeverSelectedAsBookDetail() {
        assertTrue(AutoDiscovery.sameSiteHome("http://www.biquge.pro/", "http://www.biquge.pro/#"))
        assertEquals(0, AutoDiscovery.scoreDetail("http://www.biquge.pro/#", "更多"))
    }

    @Test fun biqugeNumericNovelPageWinsOverChapterHeuristic() = runBlocking {
        val detail = """
            <html><head><meta property='og:novel:book_name' content='渊天尊'></head><body>
            <h2 class='book-main-title'>渊天尊</h2>
            <div class='row row-section'>
              <a href='/book/7910/515a6b5127ef2.html'>第1章 吴渊</a>
              <a href='/book/7910/c609f69ee7eb0.html'>第2章 欺你少年穷</a>
            </div></body></html>
        """.trimIndent()
        val chapter = "<html><article id='chapterContent'><p>${"正文".repeat(30)}</p></article></html>"
        val pages = mapOf(
            "http://www.biquge.pro/" to "<a href='#'>更多</a><a href='/lists/41.html'>排行榜</a><a href='/novel/7913.html'>渊天尊</a>",
            "http://www.biquge.pro/lists/41.html" to "<html><title>小说排行榜</title></html>",
            "http://www.biquge.pro/novel/7913.html" to detail,
            "http://www.biquge.pro/book/7910/515a6b5127ef2.html" to chapter,
        )
        val discovery = AutoDiscovery.forUrls { url ->
            HttpFetcher.FetchResult(200, url, emptyMap(), pages[url] ?: error("unexpected $url"), 1)
        }.discover("http://www.biquge.pro/")
        assertEquals("http://www.biquge.pro/novel/7913.html", discovery.pages.first { it.role == "详情" }.url)
        assertEquals("http://www.biquge.pro/book/7910/515a6b5127ef2.html", discovery.pages.first { it.role == "正文" }.url)
        assertTrue(discovery.pages.none { it.url.endsWith("/#") })
    }

    @Test fun skipsNon200CandidateAndFallsThrough() = runBlocking {
        val good = "<html><meta property='og:novel:book_name' content='书甲'></html>"
        val pages = mapOf(
            "https://site.test/" to "<a href='/bad.html'>死链</a><a href='/book/x/'>书甲</a>",
            "https://site.test/bad.html" to "whatever",
            "https://site.test/book/x/" to good,
        )
        val codes = mapOf("https://site.test/bad.html" to 404)
        val discovery = AutoDiscovery.forUrls { url ->
            HttpFetcher.FetchResult(codes[url] ?: 200, url, emptyMap(), pages[url] ?: error("unexpected $url"), 1)
        }.discover("https://site.test/")
        assertEquals("https://site.test/book/x/", discovery.pages.first { it.role == "详情" }.url)
        assertTrue(discovery.pages.none { it.url.contains("bad.html") })
    }
}
