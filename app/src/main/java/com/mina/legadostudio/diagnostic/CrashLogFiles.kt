package com.mina.legadostudio.diagnostic

import java.io.File

data class CrashItem(
    val name: String,
    val createdAt: Long,
    val size: Long,
    val path: String,
)

object CrashLogFiles {
    fun list(dir: File): List<CrashItem> = dir.listFiles().orEmpty()
        .filter(File::isFile)
        .sortedByDescending(File::lastModified)
        .map { CrashItem(it.name, it.lastModified(), it.length(), it.absolutePath) }

    fun read(dir: File, name: String, limit: Int = 100_000): String {
        val file = File(dir, name)
        require(file.canonicalFile.startsWith(dir.canonicalFile) && file.isFile) { "崩溃日志不存在" }
        return file.readText().take(limit)
    }

    fun delete(dir: File, names: Collection<String>): Int {
        var removed = 0
        names.forEach { name ->
            val file = File(dir, name)
            if (file.canonicalFile.startsWith(dir.canonicalFile) && file.isFile && file.delete()) removed++
        }
        return removed
    }
}
