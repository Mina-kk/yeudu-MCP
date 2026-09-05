package com.mina.legadostudio.domain

import com.mina.legadostudio.diagnostic.CrashLogFiles
import com.mina.legadostudio.diagnostic.DiagnosticPackager
import com.mina.legadostudio.diagnostic.ExportEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class DiagnosticSelectionTest {
    @Test
    fun snapshotOmitsHttpLogsCrashesProjectsAndSkills() {
        val snapshot = DiagnosticPackager.leanSnapshot(
            generatedAt = 1L,
            app = mapOf("version" to "1.0.0-alpha18", "ai" to false),
            mcp = mapOf("running" to true, "port" to 58823),
            readiness = mapOf("batteryUnrestricted" to true),
        )
        assertEquals(setOf("generatedAt", "app", "mcp", "readiness"), snapshot.keys)
        assertFalse(snapshot.containsKey("httpLogs"))
        assertFalse(snapshot.containsKey("projects"))
        assertFalse(snapshot.containsKey("skills"))
        assertFalse(snapshot.containsKey("crash-logs"))
    }

    @Test
    fun zipContainsOnlySelectedEntries() {
        val dir = createTempDirectory("studio-diag").toFile()
        val zip = File(dir, "selected.zip")
        DiagnosticPackager.zipSelected(
            zip,
            listOf(
                ExportEntry("operation-1.txt", "op".toByteArray()),
                ExportEntry("http-9.json", "{}".toByteArray()),
            ),
        )
        assertEquals(setOf("operation-1.txt", "http-9.json"), DiagnosticPackager.zipEntryNames(zip))
        assertFalse(DiagnosticPackager.zipEntryNames(zip).contains("crash-logs/crash-1.txt"))
        dir.deleteRecursively()
    }

    @Test
    fun crashDeleteRemovesOnlySelectedFiles() {
        val dir = createTempDirectory("studio-crash").toFile()
        File(dir, "crash-1.txt").writeText("one")
        File(dir, "crash-2.txt").writeText("two")
        File(dir, "crash-3.txt").writeText("three")
        assertEquals(3, CrashLogFiles.list(dir).size)
        assertEquals(2, CrashLogFiles.delete(dir, listOf("crash-1.txt", "crash-3.txt")))
        assertEquals(listOf("crash-2.txt"), CrashLogFiles.list(dir).map { it.name })
        dir.deleteRecursively()
    }
}
