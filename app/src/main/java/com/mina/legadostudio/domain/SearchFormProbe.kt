package com.mina.legadostudio.domain

import org.jsoup.Jsoup
import java.net.URLEncoder

data class SearchForm(
    val action: String,
    val method: String,
    val keywordField: String,
    val hidden: Map<String, String>,
) {
    fun encode(keyword: String): String {
        val fields = hidden.filter { it.key.isNotBlank() }.toMutableMap()
        fields[keywordField] = keyword
        return fields.entries.joinToString("&") { (key, value) ->
            "${enc(key)}=${enc(value)}"
        }
    }

    fun actionWithQuery(keyword: String): String {
        val query = encode(keyword)
        return if (action.contains('?')) "$action&$query" else "$action?$query"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

object SearchFormProbe {
    private val KEYWORD_NAMES = setOf("keyboard", "searchkey", "searchKey", "keyword", "key", "q", "search", "wd", "s")

    fun detect(html: String, baseUrl: String): SearchForm? {
        val doc = Jsoup.parse(html, baseUrl)
        for (form in doc.select("form")) {
            // 登录/注册表单不是搜索：关键词会被当成用户名提交
            if (form.select("input[type=password]").isNotEmpty()) continue
            val action = form.absUrl("action").ifBlank { baseUrl }
            if (!action.startsWith("http")) continue
            val actionHint = (action + " " + form.attr("name") + " " + form.attr("id")).lowercase()
            if (listOf("login", "register", "/user/", "reg.").any { it in actionHint }) continue

            val named = form.select("input[name], textarea[name]").filter { it.attr("type") != "hidden" && it.attr("type") != "submit" }
            var keyword = named.firstOrNull { it.attr("name") in KEYWORD_NAMES }
            if (keyword == null) {
                val searchy = listOf("搜", "search").any { hint ->
                    actionHint.contains(hint) || named.any { it.attr("placeholder").contains(hint) }
                }
                if (!searchy) continue
                keyword = named.firstOrNull { it.attr("type") in setOf("text", "search", "") }
            }
            val field = keyword?.attr("name").orEmpty()
            if (field.isBlank()) continue
            val method = form.attr("method").ifBlank { "get" }.uppercase()
            val hidden = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }
            return SearchForm(action, if (method == "POST") "POST" else "GET", field, hidden)
        }
        return null
    }
}
