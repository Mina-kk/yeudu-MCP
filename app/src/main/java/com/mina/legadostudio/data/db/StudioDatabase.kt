package com.mina.legadostudio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, SourceRevisionEntity::class, HttpLogEntity::class, VerificationSessionEntity::class, OperationLogEntity::class, DiagnosticSnapshotEntity::class],
    version = 11,
    exportSchema = true,
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun dao(): StudioDao

    companion object {
        @Volatile private var instance: StudioDatabase? = null

        fun get(context: Context): StudioDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StudioDatabase::class.java, "studio-v1.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instance = it }
        }
    }
}