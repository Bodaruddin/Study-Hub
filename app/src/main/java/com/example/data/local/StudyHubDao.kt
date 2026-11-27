package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("DELETE FROM cached_classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    @Query("DELETE FROM cached_subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

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

    @Query("DELETE FROM cached_materials WHERE id = :id")
    suspend fun deleteMaterialById(id: String)

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

    @Query("DELETE FROM cached_exam_papers WHERE id = :id")
    suspend fun deleteExamPaperById(id: String)

    // Paper Attachments
    @Query("SELECT * FROM cached_paper_attachments WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun observeAttachmentsForParent(parentId: String): Flow<List<CachedPaperAttachmentEntity>>

    @Query("SELECT * FROM cached_paper_attachments WHERE parentId = :parentId ORDER BY sortOrder ASC")
    suspend fun getAttachmentsForParentOnce(parentId: String): List<CachedPaperAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaperAttachments(items: List<CachedPaperAttachmentEntity>)

    @Query("DELETE FROM cached_paper_attachments WHERE parentId = :parentId")
    suspend fun deleteAttachmentsForParent(parentId: String)

    @Query("DELETE FROM cached_paper_attachments WHERE groupId = :groupId")
    suspend fun deleteAttachmentGroup(groupId: String)

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

    @Query("DELETE FROM cached_classes")
    suspend fun clearCachedClasses()

    @Query("DELETE FROM cached_subjects")
    suspend fun clearCachedSubjects()

    @Query("DELETE FROM cached_chapters")
    suspend fun clearCachedChapters()

    @Query("DELETE FROM cached_materials")
    suspend fun clearCachedMaterials()

    @Query("DELETE FROM cached_quizzes")
    suspend fun clearCachedQuizzes()

    @Query("DELETE FROM cached_questions")
    suspend fun clearCachedQuestions()

    @Query("DELETE FROM cached_exam_papers")
    suspend fun clearCachedExamPapers()

    @Query("DELETE FROM cached_paper_attachments")
    suspend fun clearCachedPaperAttachments()

    @Transaction
    suspend fun clearCurriculumCache() {
        clearCachedClasses()
        clearCachedSubjects()
        clearCachedChapters()
        clearCachedMaterials()
        clearCachedQuizzes()
        clearCachedQuestions()
        clearCachedExamPapers()
        clearCachedPaperAttachments()
    }

    @Transaction
    suspend fun replaceClasses(items: List<CachedClassEntity>) {
        clearCachedClasses()
        if (items.isNotEmpty()) upsertClasses(items)
    }

    @Transaction
    suspend fun replaceSubjects(items: List<CachedSubjectEntity>) {
        clearCachedSubjects()
        if (items.isNotEmpty()) upsertSubjects(items)
    }

    @Transaction
    suspend fun replaceChapters(items: List<CachedChapterEntity>) {
        clearCachedChapters()
        if (items.isNotEmpty()) upsertChapters(items)
    }

    @Transaction
    suspend fun replaceMaterials(items: List<CachedMaterialEntity>) {
        clearCachedMaterials()
        if (items.isNotEmpty()) upsertMaterials(items)
    }

    @Transaction
    suspend fun replaceQuizzes(items: List<CachedQuizEntity>) {
        clearCachedQuizzes()
        if (items.isNotEmpty()) upsertQuizzes(items)
    }

    @Transaction
    suspend fun replaceQuestions(items: List<CachedQuestionEntity>) {
        clearCachedQuestions()
        if (items.isNotEmpty()) upsertQuestions(items)
    }

    @Transaction
    suspend fun replaceExamPapers(items: List<CachedExamPaperEntity>) {
        clearCachedExamPapers()
        if (items.isNotEmpty()) upsertExamPapers(items)
    }
}
