package com.mina.legadostudio.diagnostic

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ExportEntry(val fileName: String, val bytes: ByteArray) {
    override fun equals(other: Any?) = other is ExportEntry && fileName == other.fileName && bytes.contentEquals(other.bytes)
    override fun hashCode() = 31 * fileName.hashCode() + bytes.contentHashCode()
}

object DiagnosticPackager {
    fun leanSnapshot(
        generatedAt: Long,
        app: Map<String, Any?>,
        mcp: Map<String, Any?>,
        readiness: Any?,
    ): Map<String, Any?> = linkedMapOf(
        "generatedAt" to generatedAt,
        "app" to app,
        "mcp" to mcp,
        "readiness" to readiness,
    )

    fun zipSelected(file: File, entries: List<ExportEntry>): File {
        require(entries.isNotEmpty()) { "没有可导出的条目" }
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.fileName))
                zip.write(entry.bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    fun zipEntryNames(file: File): Set<String> = ZipFile(file).use { zip ->
        zip.entries().toList().map { it.name }.toSet()
    }
}
