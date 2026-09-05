package com.mina.legadostudio.network

import android.content.Context

class RuntimeConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("runtime_network_config", Context.MODE_PRIVATE)
    var userAgent: String
        get() = prefs.getString("userAgent", null)?.takeIf { it.isNotBlank() } ?: HttpFetcher.DEFAULT_UA
        set(value) {
            require(value.trim().length in 8..500) { "User-Agent 长度必须在 8..500" }
            prefs.edit().putString("userAgent", value.trim()).apply()
        }
}
