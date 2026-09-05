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

    /**
     * 目标书源类型,与阅读(Legado)bookSourceType 对齐:
     * 0 文本(默认) / 1 音频 / 2 图片 / 3 文件 / 4 视频。
     * 文本类型下抓取会拦截图片、音视频等二进制响应;其余类型保留二进制元数据。
     */
    var bookSourceType: Int
        get() = prefs.getInt("bookSourceType", 0).takeIf { it in 0..4 } ?: 0
        set(value) {
            require(value in 0..4) { "书源类型必须在 0..4" }
            prefs.edit().putInt("bookSourceType", value).apply()
        }

    companion object {
        val TYPE_NAMES = listOf("文本", "音频", "图片", "文件", "视频")
        fun typeName(type: Int): String = TYPE_NAMES.getOrElse(type) { "文本" }
    }
}
