package com.mina.legadostudio.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPaginationTest {
    @Test fun treatsNextChapterRuleAsChapterNotPage() {
        assertTrue(
            ContentPagination.isNextChapter(
                "https://site.test/book/152957.html",
                "https://site.test/book/152958.html",
                "text.下一章@href",
            ),
        )
        assertFalse(
            ContentPagination.isNextChapter(
                "https://site.test/book/152957.html",
                "https://site.test/book/152957_2.html",
                "text.下一页@href",
            ),
        )
    }
}
