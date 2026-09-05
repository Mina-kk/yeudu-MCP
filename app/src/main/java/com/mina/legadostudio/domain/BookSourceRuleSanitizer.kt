package com.mina.legadostudio.domain

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object BookSourceRuleSanitizer {
    private val CSS_OR_PREFIX = Regex("\\|\\|\\s*@css:", RegexOption.IGNORE_CASE)

    fun normalizeCssOr(rule: String): String {
        val trimmed = rule.trim()
        if (!trimmed.startsWith("@css:", true)) return trimmed
        return trimmed.replace(CSS_OR_PREFIX, "||")
    }

    private fun sanitizeTransportHeaders(root: JsonObject) {
        val header = root.get("header") ?: return
        val obj = when {
            header.isJsonObject -> header.asJsonObject.deepCopy()
            header.isJsonPrimitive -> runCatching { JsonParser.parseString(header.asString).asJsonObject }.getOrNull()
            else -> null
        } ?: return
        obj.entrySet().map { it.key }.filter { it.equals("Accept-Encoding", ignoreCase = true) }.forEach(obj::remove)
        if (header.isJsonPrimitive) root.addProperty("header", obj.toString()) else root.add("header", obj)
    }

    fun sanitizeJson(json: String): String {
        val parsed = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return json
        if (!parsed.isJsonObject) return json
        val root = parsed.asJsonObject
        sanitizeTransportHeaders(root)
        fun fix(obj: JsonObject?, keys: List<String>) {
            if (obj == null) return
            for (key in keys) {
                val value = obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: continue
                val normalized = normalizeCssOr(value)
                if (normalized != value) obj.addProperty(key, normalized)
            }
        }
        fix(root.getAsJsonObject("ruleSearch"), listOf("bookList", "name", "author", "bookUrl", "coverUrl", "intro", "kind", "lastChapter"))
        fix(root.getAsJsonObject("ruleExplore"), listOf("bookList", "name", "author", "bookUrl", "coverUrl", "intro", "kind", "lastChapter"))
        fix(root.getAsJsonObject("ruleBookInfo"), listOf("name", "author", "intro", "coverUrl", "tocUrl", "kind", "lastChapter"))
        fix(root.getAsJsonObject("ruleToc"), listOf("chapterList", "chapterName", "chapterUrl", "nextTocUrl"))
        fix(root.getAsJsonObject("ruleContent"), listOf("content", "nextContentUrl"))
        val content = root.getAsJsonObject("ruleContent")
        val next = content?.get("nextContentUrl")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
        if (next.contains("下一章") || next.contains("下一回")) {
            content.remove("nextContentUrl")
        }
        return root.toString()
    }
}
