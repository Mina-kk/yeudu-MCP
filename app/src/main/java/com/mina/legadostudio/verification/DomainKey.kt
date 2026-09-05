package com.mina.legadostudio.verification

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object DomainKey {
    private val compoundSuffixes = setOf(
        "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn",
        "co.uk", "org.uk", "com.au", "co.jp", "com.hk", "com.tw",
    )

    fun fromUrl(url: String): String = fromHost(url.toHttpUrlOrNull()?.host.orEmpty())

    fun fromHost(host: String): String {
        val normalized = host.trim('.').lowercase()
        if (normalized.isBlank() || normalized == "localhost" || normalized.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}")) || normalized.contains(':')) return normalized
        val parts = normalized.split('.')
        if (parts.size <= 2) return normalized
        val suffix2 = parts.takeLast(2).joinToString(".")
        return if (suffix2 in compoundSuffixes && parts.size >= 3) parts.takeLast(3).joinToString(".") else suffix2
    }
}
