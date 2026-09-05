package com.mina.legadostudio.domain

import com.mina.legadostudio.network.HttpFetcher
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpFetcherTest {
    @Test fun stripsCallerAcceptEncodingSoOkHttpReturnsDecodedHtml() {
        MockWebServer().use { server ->
            val raw = "<!DOCTYPE html><meta property='og:novel:book_name' content='测试书'>"
            val bytes = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(raw.toByteArray()) } }.toByteArray()
            server.enqueue(MockResponse().setHeader("Content-Encoding", "gzip").setBody(Buffer().write(bytes)))
            server.start()
            val result = HttpFetcher().fetch(HttpFetcher.FetchRequest(server.url("/book").toString(), headers = mapOf("Accept-Encoding" to "gzip, deflate")))
            assertEquals(raw, result.body)
            val request = server.takeRequest()
            assertTrue(request.getHeader("Accept-Encoding").orEmpty() != "gzip, deflate")
        }
    }

    @Test fun recordsRedirectChainAndFinalBody() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final"))
            server.enqueue(MockResponse().setBody("done"))
            server.start()
            val start = server.url("/start").toString()
            val result = HttpFetcher().fetch(HttpFetcher.FetchRequest(start))
            assertEquals("done", result.body)
            assertEquals(server.url("/final").toString(), result.finalUrl)
            assertTrue(result.redirectChain.first().contains("/start"))
            assertTrue(result.redirectChain.last().contains("/final"))
        }
    }
}
