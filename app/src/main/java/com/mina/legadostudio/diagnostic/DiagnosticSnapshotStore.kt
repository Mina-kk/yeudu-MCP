package com.mina.legadostudio.diagnostic

import com.mina.legadostudio.BuildConfig
import com.mina.legadostudio.StudioApplication
import com.mina.legadostudio.data.db.DiagnosticSnapshotEntity
import com.mina.legadostudio.device.DeviceReadiness
import com.mina.legadostudio.mcp.McpConfigStore
import com.mina.legadostudio.service.McpService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DiagnosticSnapshotStore(private val app: StudioApplication) {
    private val dir = File(app.filesDir, "diagnostic-snapshots").apply { mkdirs() }
    private val dao get() = app.database.dao()

    suspend fun create(): DiagnosticSnapshotEntity {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val title = "诊断快照 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(createdAt))}"
        val mcp = McpService.status(app, includeToken = false)
        val snapshot = DiagnosticPackager.leanSnapshot(
            generatedAt = createdAt,
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
        val file = File(dir, "$id.json")
        file.writeText(app.gson.toJson(snapshot))
        val entity = DiagnosticSnapshotEntity(id = id, createdAt = createdAt, title = title, path = file.absolutePath)
        dao.saveDiagnosticSnapshot(entity)
        return entity
    }

    fun read(entity: DiagnosticSnapshotEntity): String {
        val file = File(entity.path)
        return if (file.isFile) file.readText() else ""
    }

    suspend fun delete(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        dao.diagnosticSnapshotsByIds(ids).forEach { File(it.path).delete() }
        return dao.deleteDiagnosticSnapshots(ids)
    }
}
