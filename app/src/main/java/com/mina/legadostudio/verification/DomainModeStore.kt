package com.mina.legadostudio.verification

import android.content.Context

class DomainModeStore(context: Context) {
    private val prefs = context.getSharedPreferences("domain_runtime_modes", Context.MODE_PRIVATE)
    fun requiresWebView(url: String): Boolean = prefs.getBoolean("webview:${DomainKey.fromUrl(url)}", false)
    fun requireWebView(url: String, required: Boolean = true) {
        prefs.edit().putBoolean("webview:${DomainKey.fromUrl(url)}", required).apply()
    }
}
