package com.mina.legadostudio.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceImportPayloadTest {
    @Test
    fun wrapsObjectAsArray() {
        val json = SourceImportPayload.arrayJson("""{"bookSourceName":"测","bookSourceUrl":"https://example.com"}""")
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("https://example.com"))
    }

    @Test
    fun passesThroughArray() {
        val json = SourceImportPayload.arrayJson("""[{"bookSourceUrl":"https://a.example"},{"bookSourceUrl":"https://b.example"}]""")
        assertEquals(2, json.split("bookSourceUrl").size - 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyArray() {
        SourceImportPayload.arrayJson("[]")
    }
}
