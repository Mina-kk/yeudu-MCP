package com.mina.legadostudio.runtime

import com.google.gson.JsonParser
import com.mina.legadostudio.network.HttpFetcher

object LegadoUrlOptions {
    data class Parsed(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val charset: String? = null,
        val bodyJs: String? = null,
        val webView: Boolean = false,
        val webJs: String? = null,
        val webViewDelayTime: Long = 800,
    )

    fun parse(value: String): Parsed {
        val marker = value.lastIndexOf(",{")
        if (marker < 0) return Parsed(value.trim())
        val rawUrl = value.substring(0, marker).trim()
        val options = runCatching { JsonParser.parseString(value.substring(marker + 1)).asJsonObject }.getOrNull()
            ?: return Parsed(value.trim())
        val headers = options.getAsJsonObject("header")?.entrySet()?.associate { it.key to it.value.asString }
            ?: options.getAsJsonObject("headers")?.entrySet()?.associate { it.key to it.value.asString }.orEmpty()
        return Parsed(
            url = rawUrl,
            method = options.get("method")?.asString ?: "GET",
            headers = headers,
            body = options.get("body")?.takeUnless { it.isJsonNull }?.asString,
            charset = options.get("charset")?.takeUnless { it.isJsonNull }?.asString,
            bodyJs = options.get("bodyJs")?.takeUnless { it.isJsonNull }?.asString,
            webView = options.get("webView")?.takeUnless { it.isJsonNull }?.asBoolean ?: false,
            webJs = options.get("webJs")?.takeUnless { it.isJsonNull }?.asString,
            webViewDelayTime = options.get("webViewDelayTime")?.takeUnless { it.isJsonNull }?.asLong ?: 800,
        )
    }

    fun toFetchRequest(value: String): HttpFetcher.FetchRequest {
        val parsed = parse(value)
        return HttpFetcher.FetchRequest(parsed.url, parsed.method, parsed.headers, parsed.body, parsed.charset)
    }
}
