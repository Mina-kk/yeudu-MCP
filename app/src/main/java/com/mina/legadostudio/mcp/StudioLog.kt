package com.mina.legadostudio.mcp

import com.mina.legadostudio.diagnostic.LogRedactor
import java.time.Instant
import java.util.ArrayDeque

object StudioLog {
    fun interface Sink {
        fun persist(level: String, category: String, message: String, detail: String)
    }
    fun interface Reader {
        fun recent(limit: Int): List<String>
    }

    @Volatile var sink: Sink? = null
    @Volatile var reader: Reader? = null

    private const val MAX = 100
    private val lines = ArrayDeque<String>()

    @Synchronized fun add(message: String, level: String = "I", category: String = "service", detail: String = "") {
        val safe = LogRedactor.redact(message)
        val safeDetail = LogRedactor.redact(detail)
        lines.addLast("${Instant.now()} $safe")
        while (lines.size > MAX) lines.removeFirst()
        runCatching { sink?.persist(level, category, safe, safeDetail) }
    }

    fun get(limit: Int = 50): List<String> {
        reader?.let { return it.recent(limit.coerceIn(1, 500)) }
        synchronized(this) { return lines.toList().takeLast(limit.coerceIn(1, MAX)) }
    }
}
