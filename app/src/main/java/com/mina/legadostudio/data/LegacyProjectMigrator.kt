package com.mina.legadostudio.data

import android.content.Context
import com.google.gson.JsonParser
import com.mina.legadostudio.data.db.ProjectEntity
import java.io.File

class LegacyProjectMigrator(private val context: Context, private val projects: ProjectRepository) {
    suspend fun migrate() {
        val prefs = context.getSharedPreferences("migration_state", Context.MODE_PRIVATE)
        if (prefs.getBoolean("legacyProjectsToRoom", false)) return
        val dir = File(context.filesDir, "projects")
        dir.listFiles { file -> file.extension == "json" }.orEmpty().forEach { file ->
            runCatching {
                val obj = JsonParser.parseString(file.readText()).asJsonObject
                fun text(key: String) = obj.get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                fun number(key: String, fallback: Long) = obj.get(key)?.takeUnless { it.isJsonNull }?.asLong ?: fallback
                val now = System.currentTimeMillis()
                projects.save(ProjectEntity(
                    id = text("id").ifBlank { file.nameWithoutExtension },
                    name = text("name").ifBlank { "迁移项目" },
                    siteUrl = text("siteUrl"),
                    sourceJson = text("sourceJson").ifBlank { "{}" },
                    stage = text("stage").ifBlank { "DRAFT" },
                    notes = text("notes"),
                    createdAt = number("createdAt", now),
                    updatedAt = number("updatedAt", now),
                ))
                file.renameTo(File(file.parentFile, file.name + ".migrated"))
            }
        }
        prefs.edit().putBoolean("legacyProjectsToRoom", true).apply()
    }
}
