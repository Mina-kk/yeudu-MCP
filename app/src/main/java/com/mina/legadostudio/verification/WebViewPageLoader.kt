package com.mina.legadostudio.verification

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.JsonParser
import com.mina.legadostudio.network.HttpFetcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebViewPageLoader(private val context: Context, private val userAgentProvider: () -> String = { HttpFetcher.DEFAULT_UA }) {
    data class Result(val finalUrl: String, val html: String, val elapsedMs: Long)

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun load(url: String, webJs: String? = null, delayMs: Long = 800, timeoutMs: Long = 60_000): Result = withTimeout(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            val main = Handler(Looper.getMainLooper())
            val started = System.currentTimeMillis()
            var view: WebView? = null
            fun finish(result: kotlin.Result<Result>) {
                val target = view
                main.post { target?.stopLoading(); target?.destroy(); view = null }
                if (continuation.isActive) result.onSuccess(continuation::resume).onFailure(continuation::resumeWithException)
            }
            continuation.invokeOnCancellation { main.post { view?.stopLoading(); view?.destroy(); view = null } }
            main.post {
                runCatching {
                    WebView(context).also { webView ->
                        view = webView
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.settings.userAgentString = userAgentProvider()
                        webView.settings.allowFileAccess = false
                        webView.settings.allowContentAccess = false
                        webView.settings.safeBrowsingEnabled = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                        webView.webChromeClient = WebChromeClient()
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(v: WebView, finalUrl: String) {
                                main.postDelayed({ poll(v, finalUrl, webJs, started, finish = ::finish) }, delayMs.coerceIn(0, 10_000))
                            }
                        }
                        webView.loadUrl(url)
                    }
                }.onFailure { finish(kotlin.Result.failure(it)) }
            }
        }
    }

    private fun poll(view: WebView, url: String, webJs: String?, started: Long, finish: (kotlin.Result<Result>) -> Unit) {
        val script = if (webJs.isNullOrBlank()) {
            "document.documentElement.outerHTML"
        } else {
            val executable = if (Regex("\\breturn\\b").containsMatchIn(webJs)) webJs
            else "return eval(${org.json.JSONObject.quote(webJs)});"
            "(function(){try{var r=(function(){${executable}})();return r==null?null:String(r);}catch(e){return '__STUDIO_ERROR__'+e;}})()"
        }
        view.evaluateJavascript(script) { raw ->
            val value = decode(raw)
            when {
                value == null -> Handler(Looper.getMainLooper()).postDelayed({ poll(view, view.url ?: url, webJs, started, finish) }, 300)
                value.startsWith("__STUDIO_ERROR__") -> finish(kotlin.Result.failure(IllegalStateException(value.removePrefix("__STUDIO_ERROR__"))))
                else -> {
                    CookieManager.getInstance().flush()
                    finish(kotlin.Result.success(Result(view.url ?: url, value, System.currentTimeMillis() - started)))
                }
            }
        }
    }

    private fun decode(value: String?): String? {
        if (value == null || value == "null") return null
        return runCatching { JsonParser.parseString(value).takeUnless { it.isJsonNull }?.asString }.getOrElse { value.trim('"') }
    }
}
