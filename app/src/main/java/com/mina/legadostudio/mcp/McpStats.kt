package com.mina.legadostudio.mcp

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object McpStats {
    @Volatile private var onChanged: (() -> Unit)? = null
    private val active = AtomicInteger(0)
    private val lastAccess = AtomicLong(0)
    fun setListener(listener: (() -> Unit)?) { onChanged = listener }
    fun onRequest() { lastAccess.set(System.currentTimeMillis()); onChanged?.invoke() }
    fun connected() { active.incrementAndGet(); onRequest() }
    fun disconnected() { active.updateAndGet { (it - 1).coerceAtLeast(0) }; onChanged?.invoke() }
    fun resetConnections() { active.set(0); onChanged?.invoke() }
    fun snapshot(): Map<String, Long> = mapOf("clientCount" to active.get().toLong(), "lastAccessAt" to lastAccess.get())
}
