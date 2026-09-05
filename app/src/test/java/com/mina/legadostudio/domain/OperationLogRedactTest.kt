package com.mina.legadostudio.domain

import com.mina.legadostudio.diagnostic.LogRedactor
import com.mina.legadostudio.mcp.StudioLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationLogRedactTest {
    @Test
    fun redactsTokenAuthorizationAndApiKey() {
        val raw = "authorization=Bearer secret token: abc123 api-key=sk-live header"
        val safe = LogRedactor.redact(raw)
        assertFalse(safe.contains("Bearer secret"))
        assertFalse(safe.contains("abc123"))
        assertFalse(safe.contains("sk-live"))
        assertTrue(safe.contains("authorization=***"))
        assertTrue(safe.contains("token=***"))
        assertTrue(safe.contains("api-key=***"))
    }

    @Test
    fun studioLogAddUsesRedactor() {
        StudioLog.add("connect token: super-secret-value")
        val last = StudioLog.get(5).last()
        assertFalse(last.contains("super-secret-value"))
        assertTrue(last.contains("token=***"))
    }
}
