package com.mina.legadostudio.data.db

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "projects", indices = [Index("siteUrl")])
data class ProjectEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("siteUrl") val siteUrl: String,
    @SerializedName("sourceJson") val sourceJson: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("notes") val notes: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
)

@Keep
@Entity(tableName = "source_revisions", indices = [Index("projectId"), Index("createdAt")])
data class SourceRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val sourceJson: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Keep
@Entity(tableName = "http_logs", indices = [Index("createdAt")])
data class HttpLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val url: String,
    val finalUrl: String,
    val statusCode: Int,
    val durationMs: Long,
    val requestHeaders: String,
    val responseHeaders: String,
    val requestBody: String,
    val responseBody: String,
    val error: String,
    val redirectChain: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Keep
@Entity(tableName = "verification_sessions", indices = [Index("jobId"), Index("status")])
data class VerificationSessionEntity(
    @PrimaryKey @SerializedName("id") val id: String,
    @SerializedName("jobId") val jobId: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("url") val url: String,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("status") val status: String,
    @SerializedName("finalUrl") val finalUrl: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
)

@Keep
@Entity(tableName = "operation_logs", indices = [Index("createdAt")])
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val level: String,
    val category: String,
    val message: String,
    val detail: String = "",
)

@Keep
@Entity(tableName = "diagnostic_snapshots", indices = [Index("createdAt")])
data class DiagnosticSnapshotEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val title: String,
    val path: String,
)