package com.mina.legadostudio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC") fun observeProjects(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC") suspend fun allProjects(): List<ProjectEntity>
    @Query("SELECT * FROM projects WHERE id=:id") fun observeProject(id: String): Flow<ProjectEntity?>
    @Query("SELECT * FROM projects WHERE id=:id") suspend fun project(id: String): ProjectEntity?
    @Query("SELECT * FROM projects WHERE siteUrl=:url ORDER BY updatedAt DESC LIMIT 1") suspend fun projectBySiteUrl(url: String): ProjectEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProject(value: ProjectEntity)
    @Query("DELETE FROM projects WHERE id IN (:ids)") suspend fun deleteProjects(ids: List<String>): Int

    @Query("SELECT * FROM source_revisions WHERE projectId=:projectId ORDER BY id DESC") fun observeRevisions(projectId: String): Flow<List<SourceRevisionEntity>>
    @Query("SELECT * FROM source_revisions WHERE id=:id") suspend fun revision(id: Long): SourceRevisionEntity?
    @Insert suspend fun addRevision(value: SourceRevisionEntity)

    @Query("SELECT * FROM http_logs ORDER BY id DESC LIMIT :limit") fun observeHttpLogs(limit: Int): Flow<List<HttpLogEntity>>
    @Query("SELECT * FROM http_logs WHERE id=:id") suspend fun httpLog(id: Long): HttpLogEntity?
    @Query("SELECT * FROM http_logs WHERE id IN (:ids)") suspend fun httpLogsByIds(ids: List<Long>): List<HttpLogEntity>
    @Insert suspend fun addHttpLog(value: HttpLogEntity)
    @Query("DELETE FROM http_logs") suspend fun clearHttpLogs()
    @Query("DELETE FROM http_logs WHERE id IN (:ids)") suspend fun deleteHttpLogs(ids: List<Long>): Int

    @Query("SELECT * FROM operation_logs ORDER BY id DESC LIMIT :limit") fun observeOperationLogs(limit: Int): Flow<List<OperationLogEntity>>
    @Query("SELECT * FROM operation_logs ORDER BY id DESC LIMIT :limit") suspend fun latestOperationLogs(limit: Int): List<OperationLogEntity>
    @Query("SELECT * FROM operation_logs WHERE id=:id") suspend fun operationLog(id: Long): OperationLogEntity?
    @Query("SELECT * FROM operation_logs WHERE id IN (:ids)") suspend fun operationLogsByIds(ids: List<Long>): List<OperationLogEntity>
    @Insert suspend fun addOperationLog(value: OperationLogEntity)
    @Query("DELETE FROM operation_logs WHERE id IN (:ids)") suspend fun deleteOperationLogs(ids: List<Long>): Int
    @Query("DELETE FROM operation_logs WHERE id NOT IN (SELECT id FROM operation_logs ORDER BY id DESC LIMIT :keep)") suspend fun trimOperationLogs(keep: Int)

    @Query("SELECT * FROM diagnostic_snapshots ORDER BY createdAt DESC") fun observeDiagnosticSnapshots(): Flow<List<DiagnosticSnapshotEntity>>
    @Query("SELECT * FROM diagnostic_snapshots WHERE id=:id") suspend fun diagnosticSnapshot(id: String): DiagnosticSnapshotEntity?
    @Query("SELECT * FROM diagnostic_snapshots WHERE id IN (:ids)") suspend fun diagnosticSnapshotsByIds(ids: List<String>): List<DiagnosticSnapshotEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDiagnosticSnapshot(value: DiagnosticSnapshotEntity)
    @Query("DELETE FROM diagnostic_snapshots WHERE id IN (:ids)") suspend fun deleteDiagnosticSnapshots(ids: List<String>): Int

    @Query("SELECT * FROM verification_sessions ORDER BY updatedAt DESC") fun observeVerificationSessions(): Flow<List<VerificationSessionEntity>>
    @Query("SELECT * FROM verification_sessions WHERE id=:id") suspend fun verificationSession(id: String): VerificationSessionEntity?
    @Query("SELECT * FROM verification_sessions WHERE jobId=:jobId AND domain=:domain AND status='WAITING' LIMIT 1") suspend fun waitingVerification(jobId: String, domain: String): VerificationSessionEntity?
    @Query("SELECT * FROM verification_sessions WHERE jobId=:jobId AND domain=:domain ORDER BY updatedAt DESC LIMIT 1") suspend fun latestVerification(jobId: String, domain: String): VerificationSessionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveVerificationSession(value: VerificationSessionEntity)
    @Query("DELETE FROM verification_sessions WHERE id=:id") suspend fun deleteVerificationSession(id: String)
}