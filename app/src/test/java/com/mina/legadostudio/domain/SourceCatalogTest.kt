package com.mina.legadostudio.domain

import com.mina.legadostudio.data.db.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceCatalogTest {
    private fun source(id: String, url: String, updatedAt: Long, name: String = id) = ProjectEntity(
        id = id,
        name = name,
        siteUrl = url,
        sourceJson = "{}",
        stage = "VALIDATED",
        notes = "",
        createdAt = updatedAt,
        updatedAt = updatedAt,
    )

    @Test
    fun groupsSameRegistrableDomainAndSortsByUpdatedAt() {
        val groups = SourceCatalog.groupByDomain(
            listOf(
                source("old", "https://www.example.com/a", 10, "旧稿"),
                source("new", "https://m.example.com/b", 30, "修复稿"),
                source("mid", "https://example.com", 20, "中稿"),
                source("other", "https://other.test/x", 40, "其他"),
            )
        )
        assertEquals(listOf("other.test", "example.com"), groups.map { it.domain })
        assertEquals(listOf("new", "mid", "old"), groups.first { it.domain == "example.com" }.items.map { it.id })
        assertEquals(listOf("other"), groups.first { it.domain == "other.test" }.items.map { it.id })
    }

    @Test
    fun blankUrlUsesUnknownDomain() {
        val groups = SourceCatalog.groupByDomain(listOf(source("blank", "  ", 1)))
        assertEquals(listOf(SourceCatalog.UNKNOWN_DOMAIN), groups.map { it.domain })
    }

    @Test
    fun schemeLessHostStillGroups() {
        assertEquals("example.com", SourceCatalog.domainOf("www.example.com/path"))
    }
}