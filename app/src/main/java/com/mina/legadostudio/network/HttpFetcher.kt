package com.mina.legadostudio.network

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.brotli.BrotliInterceptor
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class HttpFetcher(
    private val cookieHeaderProvider: (String) -> String? = { null },
    private val logRecorder: HttpLogRecorder? = null,
    private val userAgentProvider: () -> String = { DEFAULT_UA },
) {
    data class FetchRequest(
        val url: String? = null,
        val method: String? = "GET",
        val headers: Map<String, String>? = emptyMap(),
        val body: String? = null,
        val charset: String? = null,
        val timeoutSec: Int? = 30,
    )
    @Keep
    data class FetchResult(
        @SerializedName("code") val code: Int,
        @SerializedName("finalUrl") val finalUrl: String,
        @SerializedName("headers") val headers: Map<String, String>,
        @SerializedName("body") val body: String,
        @SerializedName("elapsedMs") val elapsedMs: Long,
        @SerializedName("redirectChain") val redirectChain: List<String> = emptyList(),
    )

    fun fetch(input: FetchRequest): FetchResult {
        val url = input.url?.trim().orEmpty()
        require(url.isNotEmpty()) { "请先填写要抓取的网址" }
        require(url.startsWith("http://") || url.startsWith("https://")) { "仅支持 HTTP/HTTPS" }
        val method = input.method?.uppercase()?.takeIf { it in setOf("GET", "POST", "HEAD") } ?: "GET"
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .callTimeout((input.timeoutSec ?: 30).coerceIn(5, 120).toLong(), TimeUnit.SECONDS)
            .addInterceptor(BrotliInterceptor)
            .build()
        val headers = Headers.Builder().apply {
            add("User-Agent", userAgentProvider())
            add("Accept-Language", "zh-CN,zh;q=0.9")
            cookieHeaderProvider(url)?.takeIf { it.isNotBlank() }?.let { add("Cookie", it) }
            input.headers.orEmpty().forEach { (key, value) ->
                // OkHttp 只有在自己协商 Content-Encoding 时才会透明解压；调用方手工设置会把 gzip/br 原始字节泄漏给 HTML 解析器。
                if (key.isNotBlank() && !key.equals("Accept-Encoding", ignoreCase = true)) set(key, value)
            }
        }.build()
        val body = when (method) {
            "GET", "HEAD" -> null
            else -> (input.body.orEmpty()).toRequestBody(
                (headers["Content-Type"] ?: "application/x-www-form-urlencoded; charset=utf-8").toMediaType()
            )
        }
        val request = Request.Builder().url(url).headers(headers).method(method, body).build()
        val started = System.currentTimeMillis()
        try {
            return client.newCall(request).execute().use { response ->
                val bytes = response.body.bytes()
                val charset = input.charset?.let { runCatching { Charset.forName(it) }.getOrNull() }
                    ?: response.body.contentType()?.charset()
                    ?: detectCharset(bytes)
                val finalUrl = response.request.url.toString()
                val text = bytes.toString(charset)
                val responseHeaders = response.headers.toMultimap().mapValues { it.value.joinToString("; ") }
                val redirectChain = generateSequence(response.priorResponse) { it.priorResponse }.toList().asReversed().map { it.request.url.toString() } + finalUrl
                val elapsed = System.currentTimeMillis() - started
                logRecorder?.record(HttpLogRecorder.Draft(method, url, finalUrl, response.code, elapsed, headers.toMultimap().mapValues { it.value.joinToString("; ") }, responseHeaders, input.body.orEmpty(), text, redirectChain = redirectChain))
                if (looksLikeVerification(response.code, finalUrl, text)) {
                    throw com.mina.legadostudio.verification.VerificationRequiredException(finalUrl, response.request.url.host)
                }
                FetchResult(response.code, finalUrl, responseHeaders, text, elapsed, redirectChain)
            }
        } catch (error: Throwable) {
            if (error !is com.mina.legadostudio.verification.VerificationRequiredException) {
                logRecorder?.record(HttpLogRecorder.Draft(method, url, durationMs = System.currentTimeMillis() - started, requestHeaders = headers.toMultimap().mapValues { it.value.joinToString("; ") }, requestBody = input.body.orEmpty(), error = error.stackTraceToString()))
            }
            throw error
        }
    }

    fun looksLikeVerification(code: Int, url: String, body: String): Boolean {
        val sample = body.take(20_000)
        val marker = Regex("Verify Yourself|WAF/VERIFY/CAPTCHA|cf-chl-|challenges\\.cloudflare\\.com|turnstile|altcha-widget|aegis_altcha|人机验证|安全验证", RegexOption.IGNORE_CASE)
        return marker.containsMatchIn(sample) && (code >= 403 || url.contains("verify", true) || url.contains("captcha", true))
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        val head = bytes.take(4096).toByteArray().toString(Charsets.ISO_8859_1)
        val name = Regex("(?i)charset\\s*=\\s*[\"']?([A-Za-z0-9._-]+)").find(head)?.groupValues?.get(1)
        return name?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
    }

    companion object {
        const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/150 Mobile Safari/537.36"
    }
}
