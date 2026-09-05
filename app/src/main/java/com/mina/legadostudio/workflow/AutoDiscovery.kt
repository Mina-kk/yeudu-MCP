package com.mina.legadostudio.workflow

import com.mina.legadostudio.domain.SearchFormProbe
import com.mina.legadostudio.network.HttpFetcher
import com.mina.legadostudio.verification.AdaptivePageLoader
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AutoDiscovery(
    private val load: suspend (HttpFetcher.FetchRequest) -> HttpFetcher.FetchResult,
) {
    constructor(loader: AdaptivePageLoader) : this({ req: HttpFetcher.FetchRequest -> loader.load(req) })

    data class Page(val role: String, val url: String, val code: Int, val html: String)
    data class Result(val pages: List<Page>, val messages: List<String>)

    suspend fun discover(siteUrl: String): Result {
        val pages = mutableListOf<Page>()
        val messages = mutableListOf<String>()
        val home = fetch(siteUrl, "首页").also(pages::add)
        val homeDoc = Jsoup.parse(home.html, home.url)
        val ranked = homeDoc.select("a[href]").mapNotNull { a ->
            val url = a.absUrl("href")
            if (!url.startsWith("http") || sameSiteHome(home.url, url)) null
            else scoreDetail(url, a.text(), isChromeLink(a)) to url
        }.sortedByDescending { it.first }
        val candidates = ranked.filter { it.first > 0 }.map { it.second }.distinct()
            .ifEmpty { ranked.map { it.second }.distinct() }

        var detail: Page? = null
        var chapterFromCandidate: Page? = null
        for (url in candidates.take(10)) {
            val page = try {
                fetch(url, "详情")
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (verification: com.mina.legadostudio.verification.VerificationRequiredException) {
                throw verification
            } catch (error: Exception) {
                messages += "详情候选抓取失败 $url：${error.message}"
                continue
            }
            if (page.code != 200) {
                messages += "跳过不可用候选 $url（HTTP ${page.code}）"
                continue
            }
            when {
                sameSiteHome(home.url, page.url) || looksLikeCategory(page.html, page.url) || looksLikeSiteHome(page.html, page.url) -> {
                    messages += "跳过非书籍页 ${page.url}"
                }
                // HTML 中的 og:novel / 大量章节链接是比“数字 .html”更强的书籍主页证据。
                // 例如 /novel/7913.html 是详情页，不能仅因文件名为数字就降级成正文。
                looksLikeBookPage(page.html, page.url) -> {
                    detail = page
                    break
                }
                looksLikeChapter(page.html, page.url) -> {
                    chapterFromCandidate = chapterFromCandidate ?: page
                    val bookHome = bookHomeFromChapter(page.url)
                    if (bookHome != null && !sameSiteHome(home.url, bookHome)) {
                        val bookPage = try {
                            fetch(bookHome, "详情")
                        } catch (cancellation: kotlinx.coroutines.CancellationException) {
                            throw cancellation
                        } catch (verification: com.mina.legadostudio.verification.VerificationRequiredException) {
                            throw verification
                        } catch (error: Exception) {
                            messages += "书籍主页抓取失败 $bookHome：${error.message}"
                            continue
                        }
                        if (bookPage.code == 200 &&
                            (looksLikeBookPage(bookPage.html, bookPage.url) ||
                                (!looksLikeCategory(bookPage.html, bookPage.url) && !looksLikeSiteHome(bookPage.html, bookPage.url)))
                        ) {
                            messages += "首页候选像章节页，已改用 ${bookPage.url} 作为详情/目录。"
                            detail = bookPage
                            break
                        }
                    }
                }
                else -> if (detail == null) detail = page
            }
        }

        if (detail == null) {
            messages += "没有从首页自动识别详情页；AI 将先使用首页证据。"
            return Result(pages, messages)
        }
        pages += detail
        if (chapterFromCandidate != null && pages.none { it.role == "正文" }) {
            pages += chapterFromCandidate.copy(role = "正文")
        }

        val detailDoc = Jsoup.parse(detail.html, detail.url)
        fun stripFragment(value: String) = value.substringBefore('#')
        val tocUrl = detailDoc.select("a[href]").firstOrNull { a ->
            val target = a.absUrl("href")
            Regex("目录|章节列表|全部章节|查看全部|章节目录").containsMatchIn(a.text()) &&
                !looksLikeChapter("", stripFragment(target)) &&
                stripFragment(target).isNotBlank() &&
                stripFragment(target) != stripFragment(detail.url)
        }?.absUrl("href")?.substringBefore('#')?.takeIf { it.isNotBlank() }
        if (tocUrl != null && tocUrl != detail.url) {
            try {
                val toc = fetch(tocUrl, "目录")
                if (toc.code == 200) pages += toc else messages += "目录页不可用（HTTP ${toc.code}），与详情共用"
            }
            catch (cancellation: kotlinx.coroutines.CancellationException) { throw cancellation }
            catch (verification: com.mina.legadostudio.verification.VerificationRequiredException) { throw verification }
            catch (error: Exception) { messages += "目录页抓取失败：${error.message}" }
        }
        if (pages.none { it.role == "目录" }) {
            pages += detail.copy(role = "目录")
            messages += "目录与详情页共用同一页面。"
        }

        if (pages.none { it.role == "正文" }) {
            val tocPage = pages.lastOrNull { it.role == "目录" } ?: detail
            val tocDoc = Jsoup.parse(tocPage.html, tocPage.url)
            val chapterUrl = tocDoc.select("a[href]").mapNotNull { a ->
                val url = a.absUrl("href")
                val text = a.text()
                if (url.isNotBlank() && (looksLikeChapter("", url) || CHAPTER_TEXT.containsMatchIn(text))) url else null
            }.firstOrNull()
            if (chapterUrl != null) {
                try {
                    val content = fetch(chapterUrl, "正文")
                    if (content.code == 200) pages += content else messages += "正文页不可用（HTTP ${content.code}）"
                }
                catch (cancellation: kotlinx.coroutines.CancellationException) { throw cancellation }
                catch (verification: com.mina.legadostudio.verification.VerificationRequiredException) { throw verification }
                catch (error: Exception) { messages += "正文页抓取失败：${error.message}" }
            } else messages += "没有自动识别正文链接，可在高级模式补充样本。"
        }

        val detailPage = pages.firstOrNull { it.role == "详情" }
        val keyword = detailPage?.let { page ->
            val doc = Jsoup.parse(page.html, page.url)
            val og = doc.selectFirst("[property=og:novel:book_name]")?.attr("content")?.trim().orEmpty()
            val h1 = if (looksLikeBookPage(page.html, page.url)) doc.selectFirst("h1")?.text()?.trim().orEmpty() else ""
            (og.ifBlank { h1 })
        }.orEmpty()
        val form = SearchFormProbe.detect(home.html, home.url)
        if (form != null && keyword.length in 2..40 && !GENERIC_TEXT.containsMatchIn(keyword) && !keyword.contains("404")) {
            try {
                val request = if (form.method == "POST") {
                    HttpFetcher.FetchRequest(url = form.action, method = "POST", body = form.encode(keyword))
                } else {
                    HttpFetcher.FetchRequest(url = form.actionWithQuery(keyword), method = "GET")
                }
                val result = load(request)
                pages += Page("搜索", result.finalUrl, result.code, result.body.take(180_000))
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (verification: com.mina.legadostudio.verification.VerificationRequiredException) {
                throw verification
            } catch (error: Exception) {
                messages += "搜索结果页抓取失败：${error.message}"
            }
        }
        return Result(pages.distinctBy { "${it.role}:${it.url}" }, messages)
    }

    private suspend fun fetch(url: String, role: String): Page {
        val result = load(HttpFetcher.FetchRequest(url = url))
        return Page(role, result.finalUrl, result.code, result.body.take(180_000))
    }

    companion object {
        fun forUrls(get: suspend (String) -> HttpFetcher.FetchResult) =
            AutoDiscovery { req: HttpFetcher.FetchRequest -> get(req.url.orEmpty()) }

        private val CHAPTER_FILE = Regex("/\\d+\\.html?$", RegexOption.IGNORE_CASE)
        private val CHAPTER_HEX_FILE = Regex("/[0-9a-f]{10,}\\.html?$", RegexOption.IGNORE_CASE)
        // 列表/排行目录下的数字 .html 是列表分页，不是章节
        private val LIST_DIR = Regex("/(lists?|sort|rank|ranks|top|category|categories|all|paihang|bookcase)(/|\\d)", RegexOption.IGNORE_CASE)
        private val CHAPTER_TEXT = Regex("第.{0,12}[章节回]")
        private val GENERIC_TEXT = Regex("首页|分类|完本|连载|排行|作家|登录|注册|古典文学|世界名著|影视原著|最新入库")
        private val CATEGORY_PATH = Regex("/(writer|authors?|category|categories|sort|tag|tags|rank|ranks)(/|$)", RegexOption.IGNORE_CASE)

        internal fun pathDepth(url: String): Int {
            val path = url.substringAfter("://").substringAfter('/', missingDelimiterValue = "").substringBefore('?').trim('/')
            return if (path.isEmpty()) 0 else path.split('/').size
        }

        internal fun sameSiteHome(home: String, url: String): Boolean {
            fun norm(value: String) = value.substringBefore('#').substringBefore('?').trimEnd('/').lowercase()
            return norm(home) == norm(url)
        }

        internal fun isChromeLink(a: Element): Boolean {
            val self = (a.id() + " " + a.className()).lowercase()
            if (listOf("nav", "brand", "logo", "menu", "footer").any { it in self }) return true
            return a.parents().any { parent ->
                val hint = (parent.id() + " " + parent.className() + " " + parent.tagName()).lowercase()
                parent.tagName() == "nav" || parent.tagName() == "header" || parent.tagName() == "footer" ||
                    listOf("navbar", "nav-collapse", "nav-menu", "main-menu", "footer", "header-menu").any { it in hint }
            }
        }

        internal fun scoreDetail(url: String, text: String, inNav: Boolean = false): Int {
            if (!url.startsWith("http")) return 0
            val withoutFragment = url.substringBefore('#')
            if (url.contains('#') && pathDepth(withoutFragment) == 0) return 0
            if (pathDepth(withoutFragment) == 0) return 0
            if (inNav) return 0
            val trimmed = text.trim()
            if (trimmed.isNotEmpty() && GENERIC_TEXT.containsMatchIn(trimmed)) return 0
            var score = 0
            val detailPath = Regex("/(novel|info|detail)/", RegexOption.IGNORE_CASE).containsMatchIn(url)
            if (Regex("/(book|novel|info|detail)/", RegexOption.IGNORE_CASE).containsMatchIn(url)) score += 8
            if (url.endsWith("/") && pathDepth(url) >= 1 && !CHAPTER_FILE.containsMatchIn(url)) score += 6
            if (CHAPTER_FILE.containsMatchIn(url) && !detailPath) score -= 12
            if (CHAPTER_HEX_FILE.containsMatchIn(url)) score -= 14
            if (CHAPTER_TEXT.containsMatchIn(text)) score -= 12
            if (Regex("chapter|read|login|register|category|list", RegexOption.IGNORE_CASE).containsMatchIn(url)) score -= 8
            if (LIST_DIR.containsMatchIn(url.substringAfter("://").let { "/${it.substringAfter('/')}" })) score -= 10
            if (CATEGORY_PATH.containsMatchIn(url.substringAfter("://").substringAfter('/').let { "/$it" })) score -= 10
            if (trimmed == "目录") score += 4
            if (trimmed.length in 2..50) score += 1
            return score
        }

        internal fun looksLikeSiteHome(html: String, url: String): Boolean {
            if (pathDepth(url) == 0) return true
            if (looksLikeBookPage(html, url)) return false
            val doc = Jsoup.parse(html, url)
            val bookLinks = doc.select("a[href]").count { a ->
                val href = a.absUrl("href")
                pathDepth(href) >= 1 && !CHAPTER_FILE.containsMatchIn(href)
            }
            val chapterLinks = doc.select("a[href]").count { CHAPTER_FILE.containsMatchIn(it.absUrl("href")) }
            return bookLinks >= 8 && chapterLinks < 3
        }

        internal fun looksLikeCategory(html: String, url: String): Boolean {
            if (looksLikeBookPage(html, url) || looksLikeChapter(html, url)) return false
            if (pathDepth(url) == 0) return true
            val doc = Jsoup.parse(html, url)
            val title = doc.title() + " " + doc.selectFirst("h1")?.text().orEmpty()
            if (GENERIC_TEXT.containsMatchIn(title) && doc.selectFirst("[property=og:novel:book_name]") == null) return true
            val bookish = doc.select("a[href]").count { a ->
                val href = a.absUrl("href")
                href.endsWith("/") && pathDepth(href) in 1..2 && !CHAPTER_FILE.containsMatchIn(href) && !sameSiteHome(url, href)
            }
            val chapterLinks = doc.select("a[href]").count { CHAPTER_FILE.containsMatchIn(it.absUrl("href")) }
            return bookish >= 8 && chapterLinks < 3
        }

        internal fun looksLikeBookPage(html: String, url: String): Boolean {
            if (CHAPTER_FILE.containsMatchIn(url)) return false
            val doc = Jsoup.parse(html, url)
            if (doc.selectFirst("[property=og:novel:book_name], [property=og:novel:author]") != null) return true
            val chapterLinks = doc.select("a[href]").count { CHAPTER_FILE.containsMatchIn(it.absUrl("href")) }
            if (chapterLinks >= 8) return true
            val hasAuthor = Regex("作者[:：]").containsMatchIn(doc.text())
            val h1 = doc.selectFirst("h1")?.text().orEmpty().trim()
            return hasAuthor && h1.length in 1..40 && !GENERIC_TEXT.containsMatchIn(h1) && chapterLinks >= 1
        }

        internal fun pickBookUrl(doc: org.jsoup.nodes.Document, home: String): String? =
            doc.select("a[href]").mapNotNull { a ->
                val url = a.absUrl("href")
                if (!url.startsWith("http") || sameSiteHome(home, url)) null else scoreDetail(url, a.text(), isChromeLink(a)) to url
            }.filter { it.first > 0 }.maxByOrNull { it.first }?.second

        internal fun looksLikeChapter(html: String, url: String): Boolean {
            if (LIST_DIR.containsMatchIn(url)) return false
            val doc = html.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it, url) }
            if (doc?.selectFirst("[property=og:novel:book_name], [property=og:novel:author]") != null) return false
            if (CHAPTER_FILE.containsMatchIn(url) || CHAPTER_HEX_FILE.containsMatchIn(url)) return true
            if (doc == null) return false
            if (doc.selectFirst("#chapterContent, .jsChapterWrapper, #readContent") != null) return true
            val title = doc.title() + " " + doc.selectFirst("h1")?.text().orEmpty()
            return CHAPTER_TEXT.containsMatchIn(title)
        }

        internal fun bookHomeFromChapter(url: String): String? {
            val trimmed = url.substringBefore('?').trimEnd('/')
            val parent = trimmed.substringBeforeLast('/', missingDelimiterValue = "")
            if (parent.isBlank() || parent == trimmed || !parent.startsWith("http")) return null
            return if (parent.endsWith("/")) parent else "$parent/"
        }
    }
}
