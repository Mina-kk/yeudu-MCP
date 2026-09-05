package com.mina.legadostudio.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderCatalogTest {
    @Test
    fun knownPackagesUseFriendlyNames() {
        val original = ReaderCatalog.describe("io.legado.app", "阅读")
        assertEquals("阅读 原版", original.label)
        assertTrue(original.known)
        val md3 = ReaderCatalog.describe("io.legado.app.md3", "阅读")
        assertEquals("阅读 MD3", md3.label)
    }

    @Test
    fun unknownPackagesAreMarked() {
        val unknown = ReaderCatalog.describe("com.example.reader", "某某阅读")
        assertEquals("某某阅读 · 未知阅读", unknown.label)
        assertFalse(unknown.known)
        val empty = ReaderCatalog.describe("com.foo.bar", "")
        assertEquals("未知阅读", empty.label)
    }

    @Test
    fun lastChoiceIsPinnedFirst() {
        val apps = mutableListOf(
            ReaderCatalog.describe("io.legado.app", "阅读"),
            ReaderCatalog.describe("io.legado.app.md3", "阅读"),
            ReaderCatalog.describe("com.foo.reader", "第三方"),
        )
        ReaderCatalog.sort(apps, "com.foo.reader")
        assertEquals("com.foo.reader", apps.first().packageName)
    }
}
