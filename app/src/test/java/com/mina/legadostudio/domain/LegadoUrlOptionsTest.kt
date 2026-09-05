package com.mina.legadostudio.domain

import com.mina.legadostudio.runtime.LegadoUrlOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegadoUrlOptionsTest {
    @Test fun parsesMethodHeadersBodyCharsetAndWebView() {
        val parsed = LegadoUrlOptions.parse("https://example.com/api,{\"method\":\"POST\",\"headers\":{\"X-Test\":\"1\"},\"body\":\"q=1\",\"charset\":\"GBK\",\"webView\":true,\"webViewDelayTime\":1200}")
        assertEquals("https://example.com/api", parsed.url)
        assertEquals("POST", parsed.method)
        assertEquals("1", parsed.headers["X-Test"])
        assertEquals("q=1", parsed.body)
        assertEquals("GBK", parsed.charset)
        assertTrue(parsed.webView)
        assertEquals(1200L, parsed.webViewDelayTime)
    }

    @Test fun plainUrlRemainsUntouched() {
        assertEquals("https://example.com/a,b", LegadoUrlOptions.parse("https://example.com/a,b").url)
    }
}
