package com.mina.legadostudio.domain

object ContentPagination {
    private val CHAPTER_FILE = Regex("/(\\d+)\\.html?$", RegexOption.IGNORE_CASE)

    fun isNextChapter(current: String, next: String, rule: String): Boolean {
        val text = rule
        if (text.contains("下一章") || text.contains("下一回")) return true
        if (text.contains("下一页") || text.contains("下页")) return false
        val currentPath = current.substringBefore('?').substringBefore('#')
        val nextPath = next.substringBefore('?').substringBefore('#')
        val currentId = CHAPTER_FILE.find(currentPath)?.groupValues?.get(1)?.toLongOrNull()
        val nextId = CHAPTER_FILE.find(nextPath)?.groupValues?.get(1)?.toLongOrNull()
        if (currentId == null || nextId == null || currentId == nextId) return false
        val sameDir = currentPath.substringBeforeLast('/') == nextPath.substringBeforeLast('/')
        val pageSuffix = nextPath.contains('_') || next.contains("page=", true)
        return sameDir && !pageSuffix
    }
}
