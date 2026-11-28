package com.example.data.remote

import android.content.Context
import com.example.data.local.CachedBookmarkEntity
import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedClassEntity
import com.example.data.local.CachedDownloadEntity
import com.example.data.local.CachedExamPaperEntity
import com.example.data.local.CachedMaterialEntity
import com.example.data.local.CachedQuestionEntity
import com.example.data.local.CachedQuizAttemptEntity
import com.example.data.local.CachedQuizEntity
import com.example.data.local.CachedReadingProgressEntity
import com.example.data.local.CachedSubjectEntity
import com.example.data.local.PendingSyncEntity
import com.example.data.local.StudyHubDao
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.BookmarkRecord
import com.example.data.model.ChapterItem
import com.example.data.model.DownloadRecord
import com.example.data.model.ExamPaper
import com.example.data.model.MaterialType
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SupabaseMappers
import com.example.data.model.SupabaseMappers.toSupabaseJson
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class StudyHubRepository(
    private val supabase: SupabaseRestClient,
    private val dao: StudyHubDao,
    private val syncManager: OfflineSyncManager,
    private val appContext: Context
) {
    private val auth = Firebase.auth
    private val syncHelper = SupabaseSeedAndSyncHelper(supabase, dao, appContext)
    private val profileRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    private val cloudRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

    @Volatile
    private var verifiedAdminUid: String? = null

    fun isSupabaseConfigured(): Boolean = supabase.isConfigured()

    fun getSupabaseConnectionStatus(): SupabaseConnectionStatus = supabase.connectionStatus()

    suspend fun testSupabaseConnection(url: String, anonKey: String): SupabaseTestResult {
        requireAdmin()
        return supabase.testConnection(urlOverride = url, anonKeyOverride = anonKey)
    }

    suspend fun saveManualSupabaseConfig(url: String, anonKey: String): Result<SupabaseTestResult> {
        return try {
            requireAdmin()
            supabase.saveManualConfig(url, anonKey)
            val test = supabase.testConnection()
            if (test.connected && test.schemaReady) {
                syncHelper.seedCurriculumToCloudIfEmpty()
                syncHelper.syncCurriculumFromCloud(isAdmin = true)
                cloudRefreshTrigger.tryEmit(Unit)
                profileRefreshTrigger.tryEmit(Unit)
            }
            Result.success(test)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearManualSupabaseConfig(): SupabaseConnectionStatus {
        requireAdmin()
        supabase.clearManualConfig()
        return supabase.connectionStatus()
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing StudyHub data.")

    fun isVerifiedAdminEmail(email: String?): Boolean =
        email != null &&
            BOOTSTRAP_ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) } &&
            (auth.currentUser?.isEmailVerified == true)

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return isVerifiedAdminEmail(user.email) || verifiedAdminUid == user.uid
    }

    private fun requireAdmin(): String {
        val user = auth.currentUser
            ?: throw SecurityException("User must be signed in before accessing StudyHub admin controls.")
        if (!isVerifiedAdminEmail(user.email) && verifiedAdminUid != user.uid) {
            throw SecurityException("Admin privileges required for this operation.")
        }
        return user.uid
    }

    suspend fun ensureLocalSeedData() {
        syncHelper.ensureLocalSeedData(force = false)
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String
    ): Result<UserProfile> {
        val path = "user_profiles/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Authenticated UID mismatch." }

            val fallbackRole = if (isVerifiedAdminEmail(email)) "admin" else "student"
            val defaultProfile = UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.ifBlank { "StudyHub Student" },
                email = email,
                selectedClass = 7L,
                role = fallbackRole,
                subscriptionTier = "free",
                avgScore = 0L,
                dayStreak = 0L,
                points = 0L,
                dailyGoalCount = 5L,
                dailyCompletedCount = 0L,
                darkThemeEnabled = fallbackRole == "admin",
                notificationsEnabled = true,
                examRemindersEnabled = true,
                newMaterialsEnabled = true
            )

            if (!supabase.isConfigured()) {
                verifiedAdminUid = if (fallbackRole == "admin") uid else null
                return Result.success(defaultProfile)
            }

            val rows = supabase.select("user_profiles", mapOf("id" to "eq.$uid"), limit = 1)
            if (rows.length() > 0) {
                val existing = SupabaseMappers.userProfileFromJson(rows.getJSONObject(0))
                val promoted = if (isVerifiedAdminEmail(email) && existing.role != "admin") {
                    supabase.updateWhere(
                        table = "user_profiles",
                        filters = mapOf("id" to "eq.$uid"),
                        bodyJson = JSONObject().apply {
                            put("role", "admin")
                            put("updated_at", SupabaseMappers.nowIso())
                        }
                    )
                    existing.copy(role = "admin")
                } else {
                    existing
                }
                verifiedAdminUid = if (promoted.role == "admin" || isVerifiedAdminEmail(email)) uid else null
                Result.success(promoted)
            } else {
                supabase.upsertOne("user_profiles", defaultProfile.toSupabaseJson(uid))
                verifiedAdminUid = if (defaultProfile.role == "admin") uid else null
                Result.success(defaultProfile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val activeUid = requireUserId()
        if (activeUid != uid) return@flow

        suspend fun fetchOnce(): UserProfile? {
            if (!supabase.isConfigured()) return null
            val rows = supabase.select("user_profiles", mapOf("id" to "eq.$uid"), limit = 1)
            val profile = if (rows.length() > 0) SupabaseMappers.userProfileFromJson(rows.getJSONObject(0)) else null
            if (profile != null) {
                verifiedAdminUid = if (profile.role == "admin" || isVerifiedAdminEmail(auth.currentUser?.email)) uid else null
            }
            return profile
        }

        runCatching { fetchOnce() }.onSuccess { emit(it) }
        profileRefreshTrigger.collect {
            runCatching { fetchOnce() }.onSuccess { emit(it) }
        }
    }

    suspend fun updateUserProfileFields(uid: String, fields: Map<String, Any>): Result<Unit> {
        val path = "user_profiles/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot update another user's profile." }
            if (supabase.isConfigured()) {
                val payload = SupabaseMappers.userProfilePatchToSnakeCase(fields)
                supabase.updateWhere("user_profiles", mapOf("id" to "eq.$uid"), payload)
                profileRefreshTrigger.tryEmit(Unit)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountData(uid: String): Result<Unit> {
        val path = "user_profiles/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot delete another user's account." }
            if (supabase.isConfigured()) {
                val userFilter = mapOf("user_id" to "eq.$uid")
                listOf("bookmarks", "quiz_attempts", "reading_progress", "downloads").forEach { table ->
                    supabase.deleteWhere(table, userFilter)
                }
                supabase.deleteWhere("user_profiles", mapOf("id" to "eq.$uid"))
            }
            dao.clearUserBookmarks(uid)
            dao.clearUserQuizAttempts(uid)
            dao.clearUserReadingProgress(uid)
            dao.clearUserDownloads(uid)
            dao.clearUserPendingSync(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // Offline-first Room Streams
    fun observeLocalClasses(): Flow<List<SchoolClass>> =
        dao.observeClasses().map { list -> list.map { it.toDomain() } }

    fun observeLocalSubjects(): Flow<List<SubjectItem>> =
        dao.observeAllSubjects().map { list -> list.map { it.toDomain() } }

    fun observeLocalChapters(): Flow<List<ChapterItem>> =
        dao.observeAllChapters().map { list -> list.map { it.toDomain() } }

    fun observeLocalMaterials(): Flow<List<StudyMaterial>> =
        dao.observeAllMaterials().map { list -> list.map { it.toDomain() } }

    fun observeDownloadedMaterialIds(): Flow<Set<String>> =
        dao.observeAllMaterials().map { list -> list.filter { it.downloadedOffline }.map { it.id }.toSet() }

    fun observeLocalQuizzes(): Flow<List<QuizItem>> =
        dao.observeAllQuizzes().map { list -> list.map { it.toDomain() } }

    fun observeAllLocalQuestions(): Flow<List<QuizQuestion>> =
        dao.observeAllQuestions().map { list -> list.map { it.toDomain() } }

    fun observeLocalExamPapers(): Flow<List<ExamPaper>> =
        dao.observeAllExamPapers().map { list -> list.map { it.toDomain() } }

    fun observeDownloadedPaperIds(): Flow<Set<String>> =
        dao.observeAllExamPapers().map { list -> list.filter { it.downloadedOffline }.map { it.id }.toSet() }

    fun observeLocalBookmarks(userId: String): Flow<List<BookmarkRecord>> =
        dao.observeBookmarks(userId).map { list -> list.map { it.toDomain() } }

    fun observeLocalQuizAttempts(userId: String): Flow<List<QuizAttemptRecord>> =
        dao.observeQuizAttempts(userId).map { list -> list.map { it.toDomain() } }

    fun observeLocalReadingProgress(userId: String): Flow<List<ReadingProgressRecord>> =
        dao.observeReadingProgress(userId).map { list -> list.map { it.toDomain() } }

    fun observeLocalDownloads(userId: String): Flow<List<DownloadRecord>> =
        dao.observeDownloads(userId).map { list -> list.map { it.toDomain() } }

    fun observePendingSyncCount(userId: String): Flow<Int> =
        dao.observePendingSync(userId).map { it.size }

    fun observeCloudAnnouncements(isAdmin: Boolean): Flow<List<AnnouncementItem>> = flow {
        requireUserId()
        suspend fun fetchList(): List<AnnouncementItem> {
            if (!supabase.isConfigured()) return SeedCurriculumData.defaultAnnouncements
            val effectiveAdmin = isAdmin && isCurrentUserAdmin()
            val filters = if (effectiveAdmin) emptyMap() else mapOf("published" to "eq.true")
            val arr = supabase.select("announcements", filters, order = "created_at.desc")
            val parsed = (0 until arr.length()).map { i ->
                SupabaseMappers.announcementFromJson(arr.getJSONObject(i))
            }
            return parsed
        }

        emit(runCatching { fetchList() }.getOrElse {
            handleCloudError(it, OperationType.LIST, "announcements")
            SeedCurriculumData.defaultAnnouncements
        })
        cloudRefreshTrigger.collect {
            runCatching { fetchList() }.onSuccess { emit(it) }
        }
    }

    fun observeCloudAppSettings(): Flow<AppSettings> = flow {
        requireUserId()
        suspend fun fetchSettings(): AppSettings {
            if (!supabase.isConfigured()) return SeedCurriculumData.defaultAppSettings
            val arr = supabase.select("app_settings", mapOf("published" to "eq.true"), limit = 1)
            return if (arr.length() > 0) {
                SupabaseMappers.appSettingsFromJson(arr.getJSONObject(0))
            } else {
                SeedCurriculumData.defaultAppSettings
            }
        }

        emit(runCatching { fetchSettings() }.getOrElse {
            handleCloudError(it, OperationType.LIST, "app_settings")
            SeedCurriculumData.defaultAppSettings
        })
        cloudRefreshTrigger.collect {
            runCatching { fetchSettings() }.onSuccess { emit(it) }
        }
    }

    suspend fun syncCurriculumFromCloud(isAdmin: Boolean): Result<Unit> {
        requireUserId()
        val effectiveAdmin = isAdmin && isCurrentUserAdmin()
        val res = syncHelper.syncCurriculumFromCloud(effectiveAdmin)
        cloudRefreshTrigger.tryEmit(Unit)
        return res
    }

    suspend fun syncUserSubcollectionsFromCloud(uid: String): Result<Unit> {
        val activeUid = requireUserId()
        check(activeUid == uid)
        return syncHelper.syncUserSubcollectionsFromCloud(uid)
    }

    // Student Offline-First Mutations (Immediate Room write + Queued Sync to Supabase)
    suspend fun toggleBookmarkOfflineFirst(uid: String, material: StudyMaterial, currentlyBookmarked: Boolean): Result<Unit> {
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot modify bookmarks for another user." }
            val docId = "bm_${material.id}"
            val syncKey = "bookmark_${uid}_${material.id}"
            if (currentlyBookmarked) {
                dao.deleteBookmarkByMaterial(uid, material.id)
                dao.enqueueSync(
                    PendingSyncEntity(
                        syncKey = syncKey,
                        userId = uid,
                        actionType = OfflineSyncManager.ACTION_DELETE_BOOKMARK,
                        targetId = docId,
                        payloadJson = "{}",
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                val accent = when (material.subjectName.lowercase()) {
                    "science" -> "green"
                    "mathematics" -> "orange"
                    "social science" -> "purple"
                    "hindi" -> "pink"
                    else -> "blue"
                }
                val subtitle = MaterialType.fromKey(material.materialType).label
                dao.upsertBookmark(
                    CachedBookmarkEntity(
                        id = docId,
                        userId = uid,
                        materialId = material.id,
                        title = material.title,
                        subjectName = material.subjectName,
                        classGrade = material.classGrade,
                        materialType = material.materialType,
                        subtitle = subtitle,
                        accentKey = accent,
                        savedAtMillis = System.currentTimeMillis()
                    )
                )
                val payload = JSONObject().apply {
                    put("materialId", material.id)
                    put("title", material.title)
                    put("subjectName", material.subjectName)
                    put("classGrade", material.classGrade)
                    put("materialType", material.materialType)
                    put("subtitle", subtitle)
                    put("accentKey", accent)
                }
                dao.enqueueSync(
                    PendingSyncEntity(
                        syncKey = syncKey,
                        userId = uid,
                        actionType = OfflineSyncManager.ACTION_UPSERT_BOOKMARK,
                        targetId = docId,
                        payloadJson = payload.toString(),
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            }
            syncManager.flushPendingSync(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleDownloadOfflineFirst(uid: String, material: StudyMaterial, currentlyDownloaded: Boolean): Result<Unit> {
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot modify downloads for another user." }
            val docId = "dl_${material.id}"
            val syncKey = "download_${uid}_${material.id}"
            val now = System.currentTimeMillis()
            if (currentlyDownloaded) {
                dao.setMaterialDownloaded(material.id, false, 0L)
                dao.deleteDownloadByMaterial(uid, material.id)
                dao.enqueueSync(
                    PendingSyncEntity(
                        syncKey = syncKey,
                        userId = uid,
                        actionType = OfflineSyncManager.ACTION_DELETE_DOWNLOAD,
                        targetId = docId,
                        payloadJson = "{}",
                        createdAtMillis = now
                    )
                )
            } else {
                dao.setMaterialDownloaded(material.id, true, now)
                dao.upsertDownload(
                    CachedDownloadEntity(
                        id = docId,
                        userId = uid,
                        materialId = material.id,
                        title = material.title,
                        subjectName = material.subjectName,
                        classGrade = material.classGrade,
                        fileSizeLabel = material.fileSizeLabel,
                        downloadedAtMillis = now
                    )
                )
                val payload = JSONObject().apply {
                    put("materialId", material.id)
                    put("title", material.title)
                    put("subjectName", material.subjectName)
                    put("classGrade", material.classGrade)
                    put("fileSizeLabel", material.fileSizeLabel)
                }
                dao.enqueueSync(
                    PendingSyncEntity(
                        syncKey = syncKey,
                        userId = uid,
                        actionType = OfflineSyncManager.ACTION_ADD_DOWNLOAD,
                        targetId = docId,
                        payloadJson = payload.toString(),
                        createdAtMillis = now
                    )
                )
            }
            syncManager.flushPendingSync(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleExamPaperDownload(paper: ExamPaper, currentlyDownloaded: Boolean) {
        requireUserId()
        dao.setExamPaperDownloaded(paper.id, !currentlyDownloaded)
    }

    suspend fun recordReadingProgressOfflineFirst(uid: String, material: StudyMaterial, progressPercent: Long) {
        val activeUid = requireUserId()
        check(activeUid == uid) { "Cannot modify reading progress for another user." }
        val docId = "prog_${material.id}"
        val now = System.currentTimeMillis()
        dao.upsertReadingProgress(
            CachedReadingProgressEntity(
                id = docId,
                userId = uid,
                materialId = material.id,
                title = material.title,
                subjectName = material.subjectName,
                chapterTitle = material.chapterTitle,
                classGrade = material.classGrade,
                progressPercent = progressPercent.coerceIn(0L, 100L),
                updatedAtMillis = now
            )
        )
        val payload = JSONObject().apply {
            put("materialId", material.id)
            put("title", material.title)
            put("subjectName", material.subjectName)
            put("chapterTitle", material.chapterTitle)
            put("classGrade", material.classGrade)
            put("progressPercent", progressPercent.coerceIn(0L, 100L))
        }
        dao.enqueueSync(
            PendingSyncEntity(
                syncKey = "prog_${uid}_${material.id}",
                userId = uid,
                actionType = OfflineSyncManager.ACTION_UPSERT_PROGRESS,
                targetId = docId,
                payloadJson = payload.toString(),
                createdAtMillis = now
            )
        )
        syncManager.flushPendingSync(uid)
    }

    suspend fun saveQuizAttemptOfflineFirst(
        uid: String,
        quiz: QuizItem,
        mode: String,
        totalQuestions: Int,
        correctAnswers: Int,
        durationSeconds: Int,
        pointsEarned: Int
    ): Result<Unit> {
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot record quiz attempt for another user." }
            val now = System.currentTimeMillis()
            val attemptId = "att_${quiz.id}_$now"
            val scorePercent = if (totalQuestions > 0) ((correctAnswers * 100) / totalQuestions).toLong() else 0L
            dao.upsertQuizAttempt(
                CachedQuizAttemptEntity(
                    id = attemptId,
                    userId = uid,
                    quizId = quiz.id,
                    quizTitle = quiz.title,
                    subjectName = quiz.subjectName,
                    classGrade = quiz.classGrade,
                    mode = mode,
                    totalQuestions = totalQuestions.toLong(),
                    correctAnswers = correctAnswers.toLong(),
                    scorePercent = scorePercent,
                    durationSeconds = durationSeconds.toLong(),
                    pointsEarned = pointsEarned.toLong(),
                    attemptedAtMillis = now
                )
            )
            val payload = JSONObject().apply {
                put("quizId", quiz.id)
                put("quizTitle", quiz.title)
                put("subjectName", quiz.subjectName)
                put("classGrade", quiz.classGrade)
                put("mode", mode)
                put("totalQuestions", totalQuestions.toLong())
                put("correctAnswers", correctAnswers.toLong())
                put("scorePercent", scorePercent)
                put("durationSeconds", durationSeconds.toLong())
                put("pointsEarned", pointsEarned.toLong())
            }
            dao.enqueueSync(
                PendingSyncEntity(
                    syncKey = "attempt_${uid}_$attemptId",
                    userId = uid,
                    actionType = OfflineSyncManager.ACTION_ADD_QUIZ_ATTEMPT,
                    targetId = attemptId,
                    payloadJson = payload.toString(),
                    createdAtMillis = now
                )
            )
            syncManager.flushPendingSync(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin CRUD & Seeding Operations
    suspend fun seedCurriculumToCloudIfEmpty(): Result<Unit> {
        requireAdmin()
        return syncHelper.seedCurriculumToCloudIfEmpty()
    }

    suspend fun seedOrRepairCloudCurriculum(): Result<Int> {
        requireAdmin()
        val res = syncHelper.seedOrRepairCloudCurriculum()
        if (res.isSuccess) cloudRefreshTrigger.tryEmit(Unit)
        return res
    }

    suspend fun resetLocalCacheAndResync(isAdmin: Boolean): Result<Unit> {
        return try {
            requireUserId()
            dao.clearCurriculumCache()
            syncHelper.ensureLocalSeedData(force = true)
            syncCurriculumFromCloud(isAdmin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSchoolClass(item: SchoolClass, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "class_${item.grade}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("classes", item.toSupabaseJson(docId))
                syncHelper.markCloudSeedDone()
            }
            syncHelper.markLocalSeedDone()
            dao.upsertClasses(listOf(CachedClassEntity(docId, item.grade, item.name, item.description, item.studentCount, item.published)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "classes/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteSchoolClass(classId: String): Result<Unit> {
        if (classId.isBlank()) return Result.failure(IllegalArgumentException("Invalid class ID"))
        return try {
            requireAdmin()
            var grade: Long? = dao.getClassById(classId)?.grade
            if (grade == null && supabase.isConfigured()) {
                val rows = supabase.select("classes", mapOf("id" to "eq.$classId"), selectCols = "grade", limit = 1)
                if (rows.length() > 0) {
                    grade = rows.optJSONObject(0)?.optLong("grade")?.takeIf { it > 0L }
                }
            }
            if (grade == null) {
                grade = classId.removePrefix("class_").toLongOrNull()
            }

            if (supabase.isConfigured()) {
                if (grade != null) {
                    val gradeFilter = mapOf("class_grade" to "eq.$grade")
                    val chapterIds = fetchIds("chapters", gradeFilter)
                    val materialIds = fetchIds("study_materials", gradeFilter)
                    val paperIds = fetchIds("exam_papers", gradeFilter)
                    val quizIds = fetchIds("quizzes", gradeFilter)

                    (chapterIds + materialIds + paperIds).distinct().forEach { parentId ->
                        syncHelper.deleteRemoteAttachmentsForParent(parentId)
                    }
                    quizIds.distinct().forEach { quizId ->
                        supabase.deleteWhere("quiz_questions", mapOf("quiz_id" to "eq.$quizId"))
                    }
                    supabase.deleteWhere("exam_papers", gradeFilter)
                    supabase.deleteWhere("quizzes", gradeFilter)
                    supabase.deleteWhere("study_materials", gradeFilter)
                    supabase.deleteWhere("chapters", gradeFilter)
                    supabase.deleteWhere("subjects", gradeFilter)
                }
                supabase.deleteWhere("classes", mapOf("id" to "eq.$classId"))
                syncHelper.markCloudSeedDone()
            }
            syncHelper.markLocalSeedDone()
            if (grade != null) {
                val removedParents = dao.deleteClassCascade(classId, grade)
                removedParents.forEach { AttachmentMediaHelper.deleteParentMediaCache(appContext, it) }
            } else {
                dao.deleteClassById(classId)
            }
            cloudRefreshTrigger.tryEmit(Unit)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "classes/$classId")
            Result.failure(e)
        }
    }

    suspend fun saveSubject(item: SubjectItem, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "sub_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("subjects", item.toSupabaseJson(docId))
            }
            dao.upsertSubjects(listOf(CachedSubjectEntity(docId, item.classGrade, item.name, item.subtitle, item.iconKey, item.colorKey, item.chapterCount, item.published)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "subjects/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        if (subjectId.isBlank()) return Result.failure(IllegalArgumentException("Invalid subject ID"))
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                val subjFilter = mapOf("subject_id" to "eq.$subjectId")
                val chapterIds = fetchIds("chapters", subjFilter)
                val materialIds = fetchIds("study_materials", subjFilter)
                val paperIds = fetchIds("exam_papers", subjFilter)
                val quizIds = fetchIds("quizzes", subjFilter)

                (chapterIds + materialIds + paperIds).distinct().forEach { parentId ->
                    syncHelper.deleteRemoteAttachmentsForParent(parentId)
                }
                quizIds.distinct().forEach { quizId ->
                    supabase.deleteWhere("quiz_questions", mapOf("quiz_id" to "eq.$quizId"))
                }
                supabase.deleteWhere("exam_papers", subjFilter)
                supabase.deleteWhere("quizzes", subjFilter)
                supabase.deleteWhere("study_materials", subjFilter)
                supabase.deleteWhere("chapters", subjFilter)
                supabase.deleteWhere("subjects", mapOf("id" to "eq.$subjectId"))
            }
            val removedParents = dao.deleteSubjectCascade(subjectId)
            removedParents.forEach { AttachmentMediaHelper.deleteParentMediaCache(appContext, it) }
            cloudRefreshTrigger.tryEmit(Unit)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "subjects/$subjectId")
            Result.failure(e)
        }
    }

    suspend fun saveChapter(item: ChapterItem, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "chap_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("chapters", item.toSupabaseJson(docId))
            }
            dao.upsertChapters(listOf(CachedChapterEntity(docId, item.classGrade, item.subjectId, item.subjectName, item.chapterNumber, item.title, item.summary, item.published)))
            if (!isCreate) {
                syncHelper.updateAttachmentsPublishedState(docId, item.published)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "chapters/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveChapterWithAttachments(
        item: ChapterItem,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>
    ): Result<Unit> {
        val docId = item.id.ifBlank { "chap_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            val finalItem = item.copy(id = docId)
            if (supabase.isConfigured()) {
                supabase.upsertOne("chapters", finalItem.toSupabaseJson(docId))
            }
            dao.upsertChapters(
                listOf(
                    CachedChapterEntity(
                        id = docId,
                        classGrade = finalItem.classGrade,
                        subjectId = finalItem.subjectId,
                        subjectName = finalItem.subjectName,
                        chapterNumber = finalItem.chapterNumber,
                        title = finalItem.title,
                        summary = finalItem.summary,
                        published = finalItem.published
                    )
                )
            )
            syncHelper.syncAttachmentsForParent(
                parentId = docId,
                published = finalItem.published,
                drafts = drafts
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "chapters/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteChapter(chapterId: String): Result<Unit> {
        if (chapterId.isBlank()) return Result.failure(IllegalArgumentException("Invalid chapter ID"))
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                val chapFilter = mapOf("chapter_id" to "eq.$chapterId")
                val materialIds = fetchIds("study_materials", chapFilter)
                val quizIds = fetchIds("quizzes", chapFilter)

                (listOf(chapterId) + materialIds).distinct().forEach { parentId ->
                    syncHelper.deleteRemoteAttachmentsForParent(parentId)
                }
                quizIds.distinct().forEach { quizId ->
                    supabase.deleteWhere("quiz_questions", mapOf("quiz_id" to "eq.$quizId"))
                }
                supabase.deleteWhere("quizzes", chapFilter)
                supabase.deleteWhere("study_materials", chapFilter)
                supabase.deleteWhere("chapters", mapOf("id" to "eq.$chapterId"))
            }
            val removedParents = dao.deleteChapterCascade(chapterId)
            removedParents.forEach { AttachmentMediaHelper.deleteParentMediaCache(appContext, it) }
            cloudRefreshTrigger.tryEmit(Unit)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "chapters/$chapterId")
            Result.failure(e)
        }
    }

    suspend fun saveStudyMaterial(item: StudyMaterial, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "mat_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("study_materials", item.toSupabaseJson(docId))
            }
            val prev = dao.getMaterialById(docId)
            dao.upsertMaterials(
                listOf(
                    CachedMaterialEntity(
                        id = docId,
                        classGrade = item.classGrade,
                        subjectId = item.subjectId,
                        subjectName = item.subjectName,
                        chapterId = item.chapterId,
                        chapterTitle = item.chapterTitle,
                        title = item.title,
                        description = item.description,
                        materialType = item.materialType,
                        contentMarkdown = item.contentMarkdown,
                        fileSizeLabel = item.fileSizeLabel,
                        tagsCsv = item.tags.joinToString("|"),
                        important = item.important,
                        premiumOnly = item.premiumOnly,
                        published = item.published,
                        downloadedOffline = prev?.downloadedOffline ?: false,
                        downloadedAtMillis = prev?.downloadedAtMillis ?: 0L
                    )
                )
            )
            if (!isCreate) {
                syncHelper.updateAttachmentsPublishedState(docId, item.published)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "study_materials/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveStudyMaterialWithAttachments(
        item: StudyMaterial,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>
    ): Result<Unit> {
        val docId = item.id.ifBlank { "mat_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            val finalSizeLabel = if (drafts.isNotEmpty()) {
                AttachmentMediaHelper.buildAttachmentSummaryLabel(drafts, item.fileSizeLabel)
            } else if (isCreate) {
                "Notes"
            } else {
                item.fileSizeLabel.ifBlank { "Notes" }
            }
            val finalItem = item.copy(id = docId, fileSizeLabel = finalSizeLabel)
            if (supabase.isConfigured()) {
                supabase.upsertOne("study_materials", finalItem.toSupabaseJson(docId))
            }
            val prev = dao.getMaterialById(docId)
            dao.upsertMaterials(
                listOf(
                    CachedMaterialEntity(
                        id = docId,
                        classGrade = finalItem.classGrade,
                        subjectId = finalItem.subjectId,
                        subjectName = finalItem.subjectName,
                        chapterId = finalItem.chapterId,
                        chapterTitle = finalItem.chapterTitle,
                        title = finalItem.title,
                        description = finalItem.description,
                        materialType = finalItem.materialType,
                        contentMarkdown = finalItem.contentMarkdown,
                        fileSizeLabel = finalItem.fileSizeLabel,
                        tagsCsv = finalItem.tags.joinToString("|"),
                        important = finalItem.important,
                        premiumOnly = finalItem.premiumOnly,
                        published = finalItem.published,
                        downloadedOffline = prev?.downloadedOffline ?: false,
                        downloadedAtMillis = prev?.downloadedAtMillis ?: 0L
                    )
                )
            )
            syncHelper.syncAttachmentsForParent(parentId = docId, published = finalItem.published, drafts = drafts)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "study_materials/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteStudyMaterial(materialId: String): Result<Unit> {
        val res = deleteAdminRow("study_materials", materialId)
        if (res.isSuccess) {
            syncHelper.deleteRemoteAttachmentsForParent(materialId)
            dao.deleteMaterialById(materialId)
            dao.deleteAttachmentsForParent(materialId)
            AttachmentMediaHelper.deleteParentMediaCache(appContext, materialId)
        }
        return res
    }

    suspend fun saveQuiz(item: QuizItem, isCreate: Boolean): Result<String> {
        val docId = item.id.ifBlank { "quiz_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("quizzes", item.toSupabaseJson(docId))
            }
            dao.upsertQuizzes(listOf(CachedQuizEntity(docId, item.classGrade, item.subjectId, item.subjectName, item.chapterId, item.chapterTitle, item.title, item.description, item.durationMinutes, item.questionCount, item.premiumOnly, item.published)))
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "quizzes/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteQuiz(quizId: String): Result<Unit> {
        if (quizId.isBlank()) return Result.failure(IllegalArgumentException("Invalid quiz ID"))
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.deleteWhere("quiz_questions", mapOf("quiz_id" to "eq.$quizId"))
                supabase.deleteWhere("quizzes", mapOf("id" to "eq.$quizId"))
            }
            dao.deleteQuestionsByQuizId(quizId)
            dao.deleteQuizById(quizId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "quizzes/$quizId")
            Result.failure(e)
        }
    }

    suspend fun addQuizQuestion(question: QuizQuestion): Result<Unit> {
        val qId = question.id.ifBlank { "q_${System.currentTimeMillis()}" }
        val path = "quiz_questions/$qId"
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("quiz_questions", question.toSupabaseJson(qId))
            }
            dao.upsertQuestions(
                listOf(
                    CachedQuestionEntity(
                        id = qId,
                        quizId = question.quizId,
                        questionType = question.questionType,
                        prompt = question.prompt,
                        optionsJoined = question.options.joinToString("||"),
                        correctIndicesJoined = question.correctIndices.joinToString(","),
                        correctText = question.correctText,
                        explanation = question.explanation,
                        points = question.points,
                        published = question.published
                    )
                )
            )
            syncQuizQuestionCount(question.quizId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuizQuestion(questionId: String, quizId: String): Result<Unit> {
        if (questionId.isBlank()) return Result.failure(IllegalArgumentException("Invalid question ID"))
        val res = deleteAdminRow("quiz_questions", questionId)
        if (res.isSuccess) {
            dao.deleteQuestionById(questionId)
            syncQuizQuestionCount(quizId)
        }
        return res
    }

    private suspend fun syncQuizQuestionCount(quizId: String) {
        if (quizId.isBlank()) return
        runCatching {
            val count = dao.getQuestionsForQuizOnce(quizId).size.toLong()
            val quiz = dao.observeAllQuizzes().first().firstOrNull { it.id == quizId } ?: return
            dao.upsertQuizzes(listOf(quiz.copy(questionCount = count)))
            if (supabase.isConfigured()) {
                supabase.updateWhere(
                    table = "quizzes",
                    filters = mapOf("id" to "eq.$quizId"),
                    bodyJson = JSONObject().apply {
                        put("question_count", count)
                        put("updated_at", SupabaseMappers.nowIso())
                    }
                )
            }
        }
    }

    suspend fun saveExamPaper(item: ExamPaper, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "paper_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("exam_papers", item.toSupabaseJson(docId))
            }
            val prevPaper = dao.observeAllExamPapers().first().firstOrNull { it.id == docId }
            dao.upsertExamPapers(
                listOf(
                    CachedExamPaperEntity(
                        id = docId,
                        classGrade = item.classGrade,
                        subjectId = item.subjectId,
                        subjectName = item.subjectName,
                        title = item.title,
                        paperCategory = item.paperCategory,
                        year = item.year,
                        fileSizeLabel = item.fileSizeLabel,
                        contentText = item.contentText,
                        published = item.published,
                        downloadedOffline = prevPaper?.downloadedOffline ?: false
                    )
                )
            )
            if (!isCreate) {
                syncHelper.updateAttachmentsPublishedState(docId, item.published)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "exam_papers/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveExamPaperWithAttachments(
        item: ExamPaper,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>
    ): Result<Unit> {
        val docId = item.id.ifBlank { "paper_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            val finalSizeLabel = if (drafts.isNotEmpty()) {
                AttachmentMediaHelper.buildAttachmentSummaryLabel(drafts, item.fileSizeLabel)
            } else if (isCreate || item.fileSizeLabel.contains("Image", true) || item.fileSizeLabel.contains("PDF", true)) {
                "Text Blueprint"
            } else {
                item.fileSizeLabel.ifBlank { "Text Blueprint" }
            }
            val finalItem = item.copy(id = docId, fileSizeLabel = finalSizeLabel)
            if (supabase.isConfigured()) {
                supabase.upsertOne("exam_papers", finalItem.toSupabaseJson(docId))
            }
            val prevPaper = dao.observeAllExamPapers().first().firstOrNull { it.id == docId }
            dao.upsertExamPapers(
                listOf(
                    CachedExamPaperEntity(
                        id = docId,
                        classGrade = finalItem.classGrade,
                        subjectId = finalItem.subjectId,
                        subjectName = finalItem.subjectName,
                        title = finalItem.title,
                        paperCategory = finalItem.paperCategory,
                        year = finalItem.year,
                        fileSizeLabel = finalItem.fileSizeLabel,
                        contentText = finalItem.contentText,
                        published = finalItem.published,
                        downloadedOffline = prevPaper?.downloadedOffline ?: false
                    )
                )
            )
            syncHelper.syncAttachmentsForParent(parentId = docId, published = finalItem.published, drafts = drafts)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "exam_papers/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteExamPaper(paperId: String): Result<Unit> {
        val res = deleteAdminRow("exam_papers", paperId)
        if (res.isSuccess) {
            syncHelper.deleteRemoteAttachmentsForParent(paperId)
            dao.deleteExamPaperById(paperId)
            dao.deleteAttachmentsForParent(paperId)
            AttachmentMediaHelper.deleteParentMediaCache(appContext, paperId)
        }
        return res
    }

    suspend fun loadAttachmentsForParent(
        parentId: String,
        isAdmin: Boolean
    ): Result<List<ResolvedPaperAttachment>> {
        requireUserId()
        val effectiveAdmin = isAdmin && isCurrentUserAdmin()
        return syncHelper.loadAttachmentsForParent(parentId, effectiveAdmin)
    }

    suspend fun sendAnnouncement(item: AnnouncementItem): Result<Unit> {
        val docId = item.id.ifBlank { "ann_${System.currentTimeMillis()}" }
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("announcements", item.toSupabaseJson(docId))
                cloudRefreshTrigger.tryEmit(Unit)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.CREATE, "announcements/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        if (announcementId.isBlank()) return Result.failure(IllegalArgumentException("Invalid announcement ID"))
        val res = deleteAdminRow("announcements", announcementId)
        if (res.isSuccess) {
            cloudRefreshTrigger.tryEmit(Unit)
        }
        return res
    }

    suspend fun saveAppSettings(settings: AppSettings): Result<Unit> {
        val docId = "global"
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.upsertOne("app_settings", settings.toSupabaseJson())
                cloudRefreshTrigger.tryEmit(Unit)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "app_settings/$docId")
            Result.failure(e)
        }
    }

    suspend fun fetchAllUsersForAdmin(): Result<List<UserProfile>> {
        return try {
            requireAdmin()
            if (!supabase.isConfigured()) return Result.success(emptyList())
            val arr = supabase.select("user_profiles", order = "created_at.desc")
            val list = (0 until arr.length()).map { i ->
                SupabaseMappers.userProfileFromJson(arr.getJSONObject(i))
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.LIST, "user_profiles")
            Result.failure(e)
        }
    }

    private suspend fun verifyModifiableStudent(adminUid: String, targetUid: String) {
        require(targetUid.isNotBlank()) { "Invalid student ID." }
        if (targetUid == adminUid) {
            throw SecurityException("You cannot block or delete your own active admin account.")
        }
        if (supabase.isConfigured()) {
            val rows = supabase.select("user_profiles", mapOf("id" to "eq.$targetUid"), limit = 1)
            if (rows.length() > 0) {
                val target = SupabaseMappers.userProfileFromJson(rows.getJSONObject(0))
                if (target.role == "admin" || BOOTSTRAP_ADMIN_EMAILS.any { it.equals(target.email, ignoreCase = true) }) {
                    throw SecurityException("Admin accounts cannot be blocked or deleted.")
                }
            }
        }
    }

    suspend fun adminSetUserBlocked(targetUid: String, blocked: Boolean): Result<Unit> {
        return try {
            val adminUid = requireAdmin()
            verifyModifiableStudent(adminUid, targetUid)
            if (supabase.isConfigured()) {
                val payload = JSONObject().apply {
                    put("blocked", blocked)
                    put("updated_at", SupabaseMappers.nowIso())
                }
                supabase.updateWhere("user_profiles", mapOf("id" to "eq.$targetUid"), payload)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SupabaseApiException) {
            handleCloudError(e, OperationType.UPDATE, "user_profiles/$targetUid")
            if (e.statusCode == 400) {
                Result.failure(IllegalStateException("Run the updated SQL schema in Manage Database → Setup Supabase to add the blocked column."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleCloudError(e, OperationType.UPDATE, "user_profiles/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateStudent(
        targetUid: String,
        displayName: String,
        selectedClass: Long,
        subscriptionTier: String,
        blocked: Boolean
    ): Result<Unit> {
        return try {
            val adminUid = requireAdmin()
            require(targetUid.isNotBlank()) { "Invalid student ID." }
            if (blocked) {
                verifyModifiableStudent(adminUid, targetUid)
            }
            if (supabase.isConfigured()) {
                val payload = SupabaseMappers.adminStudentUpdateJson(
                    displayName = displayName,
                    selectedClass = selectedClass,
                    subscriptionTier = subscriptionTier,
                    blocked = blocked
                )
                supabase.updateWhere("user_profiles", mapOf("id" to "eq.$targetUid"), payload)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SupabaseApiException) {
            handleCloudError(e, OperationType.UPDATE, "user_profiles/$targetUid")
            if (e.statusCode == 400) {
                Result.failure(IllegalStateException("Run the updated SQL schema in Manage Database → Setup Supabase to enable all student fields."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleCloudError(e, OperationType.UPDATE, "user_profiles/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun adminDeleteStudent(targetUid: String): Result<Unit> {
        return try {
            val adminUid = requireAdmin()
            verifyModifiableStudent(adminUid, targetUid)
            if (supabase.isConfigured()) {
                val userFilter = mapOf("user_id" to "eq.$targetUid")
                listOf("bookmarks", "quiz_attempts", "reading_progress", "downloads").forEach { table ->
                    supabase.deleteWhere(table, userFilter)
                }
                supabase.deleteWhere("user_profiles", mapOf("id" to "eq.$targetUid"))
            }
            dao.clearUserBookmarks(targetUid)
            dao.clearUserQuizAttempts(targetUid)
            dao.clearUserReadingProgress(targetUid)
            dao.clearUserDownloads(targetUid)
            dao.clearUserPendingSync(targetUid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "user_profiles/$targetUid")
            Result.failure(e)
        }
    }

    private suspend fun fetchIds(table: String, filters: Map<String, String>): List<String> {
        val arr = supabase.select(table, filters, selectCols = "id")
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun deleteAdminRow(table: String, id: String): Result<Unit> {
        return try {
            requireAdmin()
            if (supabase.isConfigured()) {
                supabase.deleteWhere(table, mapOf("id" to "eq.$id"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.DELETE, "$table/$id")
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "momrooja@gmail.com"
        val BOOTSTRAP_ADMIN_EMAILS = setOf("momrooja@gmail.com", "abced6917@gmail.com")
    }
}
