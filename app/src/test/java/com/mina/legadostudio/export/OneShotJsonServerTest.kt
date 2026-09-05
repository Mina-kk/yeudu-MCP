package com.mina.legadostudio.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class OneShotJsonServerTest {
    @Test
    fun servesUtf8JsonOnceAndThenCloses() {
        val json = "[{\"bookSourceName\":\"测试🎃\",\"bookSourceUrl\":\"https://example.com/#简体\"}]"
        val server = OneShotJsonServer.start(json, 5_000)
        try {
            assertTrue(server.url.startsWith("http://127.0.0.1:"))
            assertFalse(server.url.contains(json))

            val connection = URL(server.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
            assertEquals(200, connection.responseCode)
            assertEquals("application/json; charset=utf-8", connection.getHeaderField("Content-Type"))
            assertEquals("no-store", connection.getHeaderField("Cache-Control"))
            val body = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toString(StandardCharsets.UTF_8.name())
            }
            assertEquals(json, body)
            for (i in 0 until 20) {
                if (server.isClosed()) break
                Thread.sleep(25)
            }
            assertTrue(server.isClosed())
        } finally {
            server.close()
        }
    }

    @Test
    fun wrongPathDoesNotConsumePayload() {
        val server = OneShotJsonServer.start("[]", 5_000)
        try {
            val original = URL(server.url)
            val wrong = URL("http://127.0.0.1:${server.port}/wrong.json").openConnection() as HttpURLConnection
            wrong.connectTimeout = 2_000
            wrong.readTimeout = 2_000
            assertEquals(404, wrong.responseCode)
            assertFalse(server.isClosed())

            val correct = original.openConnection() as HttpURLConnection
            correct.connectTimeout = 2_000
            correct.readTimeout = 2_000
            assertEquals(200, correct.responseCode)
            correct.inputStream.use { input -> while (input.read() >= 0) { } }
        } finally {
            server.close()
        }
    }

    @Test
    fun closeRevokesEndpoint() {
        val server = OneShotJsonServer.start("[]", 5_000)
        server.close()
        assertTrue(server.isClosed())
        try {
            URL(server.url).openConnection().getInputStream()
            fail("closed loopback endpoint must reject connections")
        } catch (_: Exception) {
            assertTrue(server.isClosed())
        }
    }
}
