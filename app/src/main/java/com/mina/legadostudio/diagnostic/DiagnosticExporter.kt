package com.mina.legadostudio.diagnostic

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mina.legadostudio.BuildConfig
import com.mina.legadostudio.StudioApplication
import com.mina.legadostudio.device.DeviceReadiness
import com.mina.legadostudio.mcp.McpConfigStore
import com.mina.legadostudio.service.McpService
import java.io.File
import java.text.DateFormat
import java.util.Date

class DiagnosticExporter(private val app: StudioApplication) {
    suspend fun snapshot(): Map<String, Any?> {
        val mcp = McpService.status(app, includeToken = false)
        return DiagnosticPackager.leanSnapshot(
            generatedAt = System.currentTimeMillis(),
            app = mapOf(
                "version" to BuildConfig.VERSION_NAME,
                "versionCode" to BuildConfig.VERSION_CODE,
                "sdk" to android.os.Build.VERSION.SDK_INT,
                "manufacturer" to android.os.Build.MANUFACTURER,
                "model" to android.os.Build.MODEL,
                "ai" to false,
                "role" to "mcp-runtime",
            ),
            mcp = mcp,
            readiness = DeviceReadiness(app).inspect(
                (mcp["port"] as? Int) ?: McpConfigStore.DEFAULT_PORT,
                mcp["running"] == true,
            ),
        )
    }

    suspend fun entriesForOperationLogs(ids: List<Long>): List<ExportEntry> {
        if (ids.isEmpty()) return emptyList()
        return app.database.dao().operationLogsByIds(ids).map { log ->
            val text = buildString {
                appendLine("id=${log.id}")
                appendLine("time=${formatTime(log.createdAt)}")
                appendLine("level=${log.level}")
                appendLine("category=${log.category}")
                appendLine("message=${log.message}")
                if (log.detail.isNotBlank()) appendLine("detail=${log.detail}")
            }
            ExportEntry("operation-${log.id}.txt", text.toByteArray())
        }
    }

    suspend fun entriesForHttpLogs(ids: List<Long>): List<ExportEntry> {
        if (ids.isEmpty()) return emptyList()
        return app.database.dao().httpLogsByIds(ids).map { log ->
            ExportEntry("http-${log.id}.json", app.gson.toJson(log).toByteArray())
        }
    }

    fun entriesForCrashes(names: Collection<String>): List<ExportEntry> = names.map { name ->
        ExportEntry(name, app.crashLogs.read(name).toByteArray())
    }

    suspend fun entriesForSnapshots(ids: List<String>): List<ExportEntry> {
        if (ids.isEmpty()) return emptyList()
        return app.database.dao().diagnosticSnapshotsByIds(ids).map { snap ->
            ExportEntry("snapshot-${snap.id}.json", app.snapshots.read(snap).toByteArray())
        }
    }

    fun shareEntries(context: Context, entries: List<ExportEntry>) {
        require(entries.isNotEmpty()) { "没有可导出的条目" }
        val dir = File(app.cacheDir, "diagnostics").apply { mkdirs() }
        val file = if (entries.size == 1) {
            File(dir, entries.first().fileName).also { it.writeBytes(entries.first().bytes) }
        } else {
            DiagnosticPackager.zipSelected(File(dir, "阅读书源MCP-选中-${System.currentTimeMillis()}.zip"), entries)
        }
        share(context, file)
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val mime = when {
            file.name.endsWith(".zip", true) -> "application/zip"
            file.name.endsWith(".json", true) -> "application/json"
            else -> "text/plain"
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                "导出选中日志",
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun formatTime(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))
}