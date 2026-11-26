package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyHubDao {

    // Classes
    @Query("SELECT * FROM cached_classes ORDER BY grade ASC")
    fun observeClasses(): Flow<List<CachedClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClasses(items: List<CachedClassEntity>)

    // Subjects
    @Query("SELECT * FROM cached_subjects ORDER BY classGrade ASC, name ASC")
    fun observeAllSubjects(): Flow<List<CachedSubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(items: List<CachedSubjectEntity>)

    // Chapters
    @Query("SELECT * FROM cached_chapters ORDER BY classGrade ASC, chapterNumber ASC")
    fun observeAllChapters(): Flow<List<CachedChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(items: List<CachedChapterEntity>)

    // Materials
    @Query("SELECT * FROM cached_materials ORDER BY classGrade ASC, title ASC")
    fun observeAllMaterials(): Flow<List<CachedMaterialEntity>>

    @Query("SELECT * FROM cached_materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: String): CachedMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterials(items: List<CachedMaterialEntity>)

    @Query("UPDATE cached_materials SET downloadedOffline = :downloaded, downloadedAtMillis = :timestamp WHERE id = :materialId")
    suspend fun setMaterialDownloaded(materialId: String, downloaded: Boolean, timestamp: Long)

    // Quizzes & Questions
    @Query("SELECT * FROM cached_quizzes ORDER BY classGrade ASC, title ASC")
    fun observeAllQuizzes(): Flow<List<CachedQuizEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizzes(items: List<CachedQuizEntity>)

    @Query("SELECT * FROM cached_questions WHERE quizId = :quizId ORDER BY id ASC")
    fun observeQuestionsForQuiz(quizId: String): Flow<List<CachedQuestionEntity>>

    @Query("SELECT * FROM cached_questions WHERE quizId = :quizId ORDER BY id ASC")
    suspend fun getQuestionsForQuizOnce(quizId: String): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_questions")
    fun observeAllQuestions(): Flow<List<CachedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(items: List<CachedQuestionEntity>)

    // Exam Papers
    @Query("SELECT * FROM cached_exam_papers ORDER BY year DESC, title ASC")
    fun observeAllExamPapers(): Flow<List<CachedExamPaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamPapers(items: List<CachedExamPaperEntity>)

    @Query("UPDATE cached_exam_papers SET downloadedOffline = :downloaded WHERE id = :paperId")
    suspend fun setExamPaperDownloaded(paperId: String, downloaded: Boolean)

    // User Bookmarks
    @Query("SELECT * FROM cached_bookmarks WHERE userId = :userId ORDER BY savedAtMillis DESC")
    fun observeBookmarks(userId: String): Flow<List<CachedBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(item: CachedBookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmarks(items: List<CachedBookmarkEntity>)

    @Query("DELETE FROM cached_bookmarks WHERE userId = :userId AND materialId = :materialId")
    suspend fun deleteBookmarkByMaterial(userId: String, materialId: String)

    // User Quiz Attempts
    @Query("SELECT * FROM cached_quiz_attempts WHERE userId = :userId ORDER BY attemptedAtMillis DESC")
    fun observeQuizAttempts(userId: String): Flow<List<CachedQuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizAttempt(item: CachedQuizAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizAttempts(items: List<CachedQuizAttemptEntity>)

    // User Reading Progress
    @Query("SELECT * FROM cached_reading_progress WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeReadingProgress(userId: String): Flow<List<CachedReadingProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(item: CachedReadingProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgressList(items: List<CachedReadingProgressEntity>)

    // User Downloads
    @Query("SELECT * FROM cached_downloads WHERE userId = :userId ORDER BY downloadedAtMillis DESC")
    fun observeDownloads(userId: String): Flow<List<CachedDownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(item: CachedDownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloads(items: List<CachedDownloadEntity>)

    @Query("DELETE FROM cached_downloads WHERE userId = :userId AND materialId = :materialId")
    suspend fun deleteDownloadByMaterial(userId: String, materialId: String)

    // Pending Sync Queue
    @Query("SELECT * FROM pending_sync_queue WHERE userId = :userId ORDER BY createdAtMillis ASC")
    fun observePendingSync(userId: String): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync_queue WHERE userId = :userId ORDER BY createdAtMillis ASC")
    suspend fun getPendingSyncOnce(userId: String): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSync(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync_queue WHERE syncKey = :syncKey")
    suspend fun removeSync(syncKey: String)

    @Query("UPDATE pending_sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE syncKey = :syncKey")
    suspend fun markSyncFailed(syncKey: String, error: String)

    @Query("DELETE FROM cached_bookmarks WHERE userId = :userId")
    suspend fun clearUserBookmarks(userId: String)

    @Query("DELETE FROM cached_quiz_attempts WHERE userId = :userId")
    suspend fun clearUserQuizAttempts(userId: String)

    @Query("DELETE FROM cached_reading_progress WHERE userId = :userId")
    suspend fun clearUserReadingProgress(userId: String)

    @Query("DELETE FROM cached_downloads WHERE userId = :userId")
    suspend fun clearUserDownloads(userId: String)

    @Query("DELETE FROM pending_sync_queue WHERE userId = :userId")
    suspend fun clearUserPendingSync(userId: String)
}
