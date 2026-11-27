package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedClassEntity::class,
        CachedSubjectEntity::class,
        CachedChapterEntity::class,
        CachedMaterialEntity::class,
        CachedQuizEntity::class,
        CachedQuestionEntity::class,
        CachedExamPaperEntity::class,
        CachedBookmarkEntity::class,
        CachedQuizAttemptEntity::class,
        CachedReadingProgressEntity::class,
        CachedDownloadEntity::class,
        PendingSyncEntity::class,
        CachedPaperAttachmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyHubDatabase : RoomDatabase() {
    abstract fun studyHubDao(): StudyHubDao

    companion object {
        @Volatile
        private var INSTANCE: StudyHubDatabase? = null

        fun getInstance(context: Context): StudyHubDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyHubDatabase::class.java,
                    "studyhub_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
