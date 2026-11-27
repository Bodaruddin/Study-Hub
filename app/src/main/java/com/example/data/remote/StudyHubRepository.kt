package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.CachedBookmarkEntity
import com.example.data.local.CachedChapterEntity
import com.example.data.local.CachedClassEntity
import com.example.data.local.CachedDownloadEntity
import com.example.data.local.CachedExamPaperEntity
import com.example.data.local.CachedMaterialEntity
import com.example.data.local.CachedPaperAttachmentEntity
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
import com.example.data.model.PaperAttachmentChunk
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class StudyHubRepository(
    private val db: FirebaseFirestore,
    private val dao: StudyHubDao,
    private val syncManager: OfflineSyncManager,
    private val appContext: Context
) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing StudyHub cloud data.")

    fun isVerifiedAdminEmail(email: String?): Boolean =
        email != null &&
            BOOTSTRAP_ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) } &&
            (auth.currentUser?.isEmailVerified == true)

    suspend fun ensureLocalSeedData() {
        val existingClasses = dao.observeClasses().first()
        if (existingClasses.isEmpty()) {
            dao.upsertClasses(SeedCurriculumData.classes)
            dao.upsertSubjects(SeedCurriculumData.subjects)
            dao.upsertChapters(SeedCurriculumData.chapters)
            dao.upsertMaterials(SeedCurriculumData.materials)
            dao.upsertQuizzes(SeedCurriculumData.quizzes)
            dao.upsertQuestions(SeedCurriculumData.questions)
            dao.upsertExamPapers(SeedCurriculumData.examPapers)
        }
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String
    ): Result<UserProfile> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Authenticated UID mismatch." }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = displayName, email = email)
                val promoted = if (isVerifiedAdminEmail(email) && existing.role != "admin") {
                    docRef.update(
                        mapOf(
                            "role" to "admin",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    existing.copy(role = "admin")
                } else {
                    existing
                }
                Result.success(promoted)
            } else {
                val role = if (isVerifiedAdminEmail(email)) "admin" else "student"
                val initial = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName.ifBlank { "StudyHub Student" },
                    email = email,
                    selectedClass = 7L,
                    role = role,
                    subscriptionTier = "free",
                    avgScore = 87L,
                    dayStreak = 15L,
                    points = 120L,
                    dailyGoalCount = 5L,
                    dailyCompletedCount = 3L,
                    darkThemeEnabled = role == "admin",
                    notificationsEnabled = true,
                    examRemindersEnabled = true,
                    newMaterialsEnabled = true
                )
                docRef.set(initial.toCreateMap()).await()
                Result.success(initial)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val activeUid = requireUserId()
        if (activeUid != uid) return@flow
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserProfileFields(uid: String, fields: Map<String, Any>): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot update another user's profile." }
            val payload = fields.toMutableMap().apply {
                remove("userId")
                remove("createdAt")
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountData(uid: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Cannot delete another user's account." }
            val userRef = db.collection("users").document(uid)
            listOf("bookmarks", "quiz_attempts", "reading_progress", "downloads").forEach { sub ->
                val docs = userRef.collection(sub).get().await()
                docs.documents.forEach { it.reference.delete().await() }
            }
            userRef.delete().await()
            dao.clearUserBookmarks(uid)
            dao.clearUserQuizAttempts(uid)
            dao.clearUserReadingProgress(uid)
            dao.clearUserDownloads(uid)
            dao.clearUserPendingSync(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
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

    fun observeLocalQuestions(quizId: String): Flow<List<QuizQuestion>> =
        dao.observeQuestionsForQuiz(quizId).map { list -> list.map { it.toDomain() } }

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

    // Cloud -> Room Sync Listeners (Auth-gated & query-aligned with published == true for students)
    fun observeCloudAnnouncements(isAdmin: Boolean): Flow<List<AnnouncementItem>> = flow {
        requireUserId()
        val query = if (isAdmin) db.collection("announcements")
        else db.collection("announcements").whereEqualTo("published", true)
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(AnnouncementItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "announcements")
                    emit(SeedCurriculumData.defaultAnnouncements)
                }
        )
    }

    fun observeCloudAppSettings(): Flow<AppSettings> = flow {
        requireUserId()
        emitAll(
            db.collection("app_settings")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .firstOrNull() ?: SeedCurriculumData.defaultAppSettings
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "app_settings")
                    emit(SeedCurriculumData.defaultAppSettings)
                }
        )
    }

    suspend fun syncCurriculumFromCloud(isAdmin: Boolean): Result<Unit> {
        return try {
            requireUserId()
            val classesQuery = if (isAdmin) db.collection("classes") else db.collection("classes").whereEqualTo("published", true)
            val subjectsQuery = if (isAdmin) db.collection("subjects") else db.collection("subjects").whereEqualTo("published", true)
            val chaptersQuery = if (isAdmin) db.collection("chapters") else db.collection("chapters").whereEqualTo("published", true)
            val materialsQuery = if (isAdmin) db.collection("study_materials") else db.collection("study_materials").whereEqualTo("published", true)
            val quizzesQuery = if (isAdmin) db.collection("quizzes") else db.collection("quizzes").whereEqualTo("published", true)
            val papersQuery = if (isAdmin) db.collection("exam_papers") else db.collection("exam_papers").whereEqualTo("published", true)

            val classes = classesQuery.get().await().toObjects(SchoolClass::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val subjects = subjectsQuery.get().await().toObjects(SubjectItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val chapters = chaptersQuery.get().await().toObjects(ChapterItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val materials = materialsQuery.get().await().toObjects(StudyMaterial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val quizzes = quizzesQuery.get().await().toObjects(QuizItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val papers = papersQuery.get().await().toObjects(ExamPaper::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            if (classes.isNotEmpty()) {
                dao.upsertClasses(classes.map { CachedClassEntity(it.id, it.grade, it.name, it.description, it.studentCount, it.published) })
            }
            if (subjects.isNotEmpty()) {
                dao.upsertSubjects(subjects.map { CachedSubjectEntity(it.id, it.classGrade, it.name, it.subtitle, it.iconKey, it.colorKey, it.chapterCount, it.published) })
            }
            if (chapters.isNotEmpty()) {
                dao.upsertChapters(chapters.map { CachedChapterEntity(it.id, it.classGrade, it.subjectId, it.subjectName, it.chapterNumber, it.title, it.summary, it.published) })
            }
            if (materials.isNotEmpty()) {
                val existingDownloaded = dao.observeAllMaterials().first().associateBy { it.id }
                dao.upsertMaterials(materials.map { m ->
                    val prev = existingDownloaded[m.id]
                    CachedMaterialEntity(
                        id = m.id,
                        classGrade = m.classGrade,
                        subjectId = m.subjectId,
                        subjectName = m.subjectName,
                        chapterId = m.chapterId,
                        chapterTitle = m.chapterTitle,
                        title = m.title,
                        description = m.description,
                        materialType = m.materialType,
                        contentMarkdown = m.contentMarkdown,
                        fileSizeLabel = m.fileSizeLabel,
                        tagsCsv = m.tags.joinToString("|"),
                        important = m.important,
                        premiumOnly = m.premiumOnly,
                        published = m.published,
                        downloadedOffline = prev?.downloadedOffline ?: false,
                        downloadedAtMillis = prev?.downloadedAtMillis ?: 0L
                    )
                })
            }
            if (quizzes.isNotEmpty()) {
                dao.upsertQuizzes(quizzes.map { q ->
                    CachedQuizEntity(q.id, q.classGrade, q.subjectId, q.subjectName, q.chapterId, q.chapterTitle, q.title, q.description, q.durationMinutes, q.questionCount, q.premiumOnly, q.published)
                })
                for (q in quizzes) {
                    val qsQuery = if (isAdmin) {
                        db.collection("quizzes").document(q.id).collection("questions")
                    } else {
                        db.collection("quizzes").document(q.id).collection("questions").whereEqualTo("published", true)
                    }
                    val qs = qsQuery.get().await().toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (qs.isNotEmpty()) {
                        dao.upsertQuestions(qs.map { qq ->
                            CachedQuestionEntity(
                                id = qq.id,
                                quizId = qq.quizId.ifBlank { q.id },
                                questionType = qq.questionType,
                                prompt = qq.prompt,
                                optionsJoined = qq.options.joinToString("||"),
                                correctIndicesJoined = qq.correctIndices.joinToString(","),
                                correctText = qq.correctText,
                                explanation = qq.explanation,
                                points = qq.points,
                                published = qq.published
                            )
                        })
                    }
                }
            }
            if (papers.isNotEmpty()) {
                val prevPapers = dao.observeAllExamPapers().first().associateBy { it.id }
                dao.upsertExamPapers(papers.map { p ->
                    CachedExamPaperEntity(
                        id = p.id,
                        classGrade = p.classGrade,
                        subjectId = p.subjectId,
                        subjectName = p.subjectName,
                        title = p.title,
                        paperCategory = p.paperCategory,
                        year = p.year,
                        fileSizeLabel = p.fileSizeLabel,
                        contentText = p.contentText,
                        published = p.published,
                        downloadedOffline = prevPapers[p.id]?.downloadedOffline ?: false
                    )
                })
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "curriculum_sync")
            Result.failure(e)
        }
    }

    suspend fun syncUserSubcollectionsFromCloud(uid: String): Result<Unit> {
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid)
            val userRef = db.collection("users").document(uid)

            val bookmarks = userRef.collection("bookmarks").get().await()
                .toObjects(BookmarkRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (bookmarks.isNotEmpty()) {
                dao.upsertBookmarks(bookmarks.map { b ->
                    CachedBookmarkEntity(
                        id = b.id,
                        userId = uid,
                        materialId = b.materialId,
                        title = b.title,
                        subjectName = b.subjectName,
                        classGrade = b.classGrade,
                        materialType = b.materialType,
                        subtitle = b.subtitle,
                        accentKey = b.accentKey,
                        savedAtMillis = b.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                })
            }

            val attempts = userRef.collection("quiz_attempts").get().await()
                .toObjects(QuizAttemptRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (attempts.isNotEmpty()) {
                dao.upsertQuizAttempts(attempts.map { a ->
                    CachedQuizAttemptEntity(
                        id = a.id,
                        userId = uid,
                        quizId = a.quizId,
                        quizTitle = a.quizTitle,
                        subjectName = a.subjectName,
                        classGrade = a.classGrade,
                        mode = a.mode,
                        totalQuestions = a.totalQuestions,
                        correctAnswers = a.correctAnswers,
                        scorePercent = a.scorePercent,
                        durationSeconds = a.durationSeconds,
                        pointsEarned = a.pointsEarned,
                        attemptedAtMillis = a.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                })
            }

            val progress = userRef.collection("reading_progress").get().await()
                .toObjects(ReadingProgressRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (progress.isNotEmpty()) {
                dao.upsertReadingProgressList(progress.map { p ->
                    CachedReadingProgressEntity(
                        id = p.id,
                        userId = uid,
                        materialId = p.materialId,
                        title = p.title,
                        subjectName = p.subjectName,
                        chapterTitle = p.chapterTitle,
                        classGrade = p.classGrade,
                        progressPercent = p.progressPercent,
                        updatedAtMillis = p.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                })
            }

            val downloads = userRef.collection("downloads").get().await()
                .toObjects(DownloadRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (downloads.isNotEmpty()) {
                dao.upsertDownloads(downloads.map { d ->
                    CachedDownloadEntity(
                        id = d.id,
                        userId = uid,
                        materialId = d.materialId,
                        title = d.title,
                        subjectName = d.subjectName,
                        classGrade = d.classGrade,
                        fileSizeLabel = d.fileSizeLabel,
                        downloadedAtMillis = d.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                })
                downloads.forEach { d ->
                    dao.setMaterialDownloaded(d.materialId, true, System.currentTimeMillis())
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/subcollections")
            Result.failure(e)
        }
    }

    // Student Offline-First Mutations (Immediate Room write + Queued Sync to Firestore)
    suspend fun toggleBookmarkOfflineFirst(uid: String, material: StudyMaterial, currentlyBookmarked: Boolean): Result<Unit> {
        return try {
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
                val subtitle = com.example.data.model.MaterialType.fromKey(material.materialType).label
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
        dao.setExamPaperDownloaded(paper.id, !currentlyDownloaded)
    }

    suspend fun recordReadingProgressOfflineFirst(uid: String, material: StudyMaterial, progressPercent: Long) {
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
        return try {
            requireUserId()
            val existing = db.collection("classes").limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            SeedCurriculumData.classes.forEach { c ->
                val item = c.toDomain()
                db.collection("classes").document(c.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.subjects.forEach { s ->
                val item = s.toDomain()
                db.collection("subjects").document(s.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.chapters.forEach { ch ->
                val item = ch.toDomain()
                db.collection("chapters").document(ch.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.materials.forEach { m ->
                val item = m.toDomain()
                db.collection("study_materials").document(m.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.quizzes.forEach { q ->
                val item = q.toDomain()
                db.collection("quizzes").document(q.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.questions.forEach { qq ->
                val item = qq.toDomain()
                db.collection("quizzes").document(item.quizId).collection("questions")
                    .document(qq.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.examPapers.forEach { ep ->
                val item = ep.toDomain()
                db.collection("exam_papers").document(ep.id).set(item.toWriteMap(true)).await()
            }
            SeedCurriculumData.defaultAnnouncements.forEach { ann ->
                db.collection("announcements").document(ann.id).set(ann.toWriteMap(true)).await()
            }
            db.collection("app_settings").document("global")
                .set(SeedCurriculumData.defaultAppSettings.toWriteMap(true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_curriculum")
            Result.failure(e)
        }
    }

    suspend fun seedOrRepairCloudCurriculum(): Result<Int> {
        return try {
            requireUserId()
            var createdCount = 0

            val existingClasses = db.collection("classes").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.classes.filter { it.id !in existingClasses }.forEach { c ->
                db.collection("classes").document(c.id).set(c.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val existingSubjects = db.collection("subjects").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.subjects.filter { it.id !in existingSubjects }.forEach { s ->
                db.collection("subjects").document(s.id).set(s.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val existingChapters = db.collection("chapters").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.chapters.filter { it.id !in existingChapters }.forEach { ch ->
                db.collection("chapters").document(ch.id).set(ch.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val existingMaterials = db.collection("study_materials").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.materials.filter { it.id !in existingMaterials }.forEach { m ->
                db.collection("study_materials").document(m.id).set(m.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val existingQuizzes = db.collection("quizzes").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.quizzes.filter { it.id !in existingQuizzes }.forEach { q ->
                db.collection("quizzes").document(q.id).set(q.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val questionsByQuiz = SeedCurriculumData.questions.groupBy { it.quizId }
            questionsByQuiz.forEach { (quizId, qs) ->
                val existingQs = db.collection("quizzes").document(quizId).collection("questions")
                    .get().await().documents.map { it.id }.toSet()
                qs.filter { it.id !in existingQs }.forEach { qq ->
                    db.collection("quizzes").document(quizId).collection("questions")
                        .document(qq.id).set(qq.toDomain().toWriteMap(true)).await()
                    createdCount++
                }
            }

            val existingPapers = db.collection("exam_papers").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.examPapers.filter { it.id !in existingPapers }.forEach { ep ->
                db.collection("exam_papers").document(ep.id).set(ep.toDomain().toWriteMap(true)).await()
                createdCount++
            }

            val existingAnnouncements = db.collection("announcements").get().await().documents.map { it.id }.toSet()
            SeedCurriculumData.defaultAnnouncements.filter { it.id !in existingAnnouncements }.forEach { ann ->
                db.collection("announcements").document(ann.id).set(ann.toWriteMap(true)).await()
                createdCount++
            }

            val settingsRef = db.collection("app_settings").document("global")
            if (!settingsRef.get().await().exists()) {
                settingsRef.set(SeedCurriculumData.defaultAppSettings.toWriteMap(true)).await()
                createdCount++
            }

            Result.success(createdCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "repair_curriculum")
            Result.failure(e)
        }
    }

    suspend fun resetLocalCacheAndResync(isAdmin: Boolean): Result<Unit> {
        return try {
            dao.clearCurriculumCache()
            ensureLocalSeedData()
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
            requireUserId()
            val ref = db.collection("classes").document(docId)
            if (isCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
            dao.upsertClasses(listOf(CachedClassEntity(docId, item.grade, item.name, item.description, item.studentCount, item.published)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "classes/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteSchoolClass(classId: String): Result<Unit> {
        val res = deleteAdminDoc("classes", classId)
        if (res.isSuccess) dao.deleteClassById(classId)
        return res
    }

    suspend fun saveSubject(item: SubjectItem, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "sub_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            val ref = db.collection("subjects").document(docId)
            val writeAsCreate = isCreate || !ref.get().await().exists()
            if (writeAsCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
            dao.upsertSubjects(listOf(CachedSubjectEntity(docId, item.classGrade, item.name, item.subtitle, item.iconKey, item.colorKey, item.chapterCount, item.published)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "subjects/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val res = deleteAdminDoc("subjects", subjectId)
        if (res.isSuccess) dao.deleteSubjectById(subjectId)
        return res
    }

    suspend fun saveChapter(item: ChapterItem, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "chap_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            val ref = db.collection("chapters").document(docId)
            if (isCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
            dao.upsertChapters(listOf(CachedChapterEntity(docId, item.classGrade, item.subjectId, item.subjectName, item.chapterNumber, item.title, item.summary, item.published)))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "chapters/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveStudyMaterial(item: StudyMaterial, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "mat_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            val ref = db.collection("study_materials").document(docId)
            val writeAsCreate = isCreate || !ref.get().await().exists()
            if (writeAsCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
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
                updateAttachmentsPublishedState(docId, item.published)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "study_materials/$docId")
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
            requireUserId()
            val finalSizeLabel = if (drafts.isNotEmpty()) {
                AttachmentMediaHelper.buildAttachmentSummaryLabel(drafts, item.fileSizeLabel)
            } else if (isCreate) {
                "Notes"
            } else {
                item.fileSizeLabel.ifBlank { "Notes" }
            }
            val finalItem = item.copy(id = docId, fileSizeLabel = finalSizeLabel)
            val ref = db.collection("study_materials").document(docId)
            val writeAsCreate = isCreate || !ref.get().await().exists()
            if (writeAsCreate) ref.set(finalItem.toWriteMap(true)).await() else ref.update(finalItem.toWriteMap(false)).await()
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
            syncAttachmentsForParent(parentId = docId, published = finalItem.published, drafts = drafts)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "study_materials/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteStudyMaterial(materialId: String): Result<Unit> {
        val res = deleteAdminDoc("study_materials", materialId)
        if (res.isSuccess) {
            deleteRemoteAttachmentsForParent(materialId)
            dao.deleteMaterialById(materialId)
            dao.deleteAttachmentsForParent(materialId)
            AttachmentMediaHelper.deleteParentMediaCache(appContext, materialId)
        }
        return res
    }

    suspend fun saveQuiz(item: QuizItem, isCreate: Boolean): Result<String> {
        val docId = item.id.ifBlank { "quiz_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            val ref = db.collection("quizzes").document(docId)
            if (isCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
            dao.upsertQuizzes(listOf(CachedQuizEntity(docId, item.classGrade, item.subjectId, item.subjectName, item.chapterId, item.chapterTitle, item.title, item.description, item.durationMinutes, item.questionCount, item.premiumOnly, item.published)))
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "quizzes/$docId")
            Result.failure(e)
        }
    }

    suspend fun addQuizQuestion(question: QuizQuestion): Result<Unit> {
        val qId = question.id.ifBlank { "q_${System.currentTimeMillis()}" }
        val path = "quizzes/${question.quizId}/questions/$qId"
        return try {
            requireUserId()
            db.collection("quizzes").document(question.quizId)
                .collection("questions").document(qId)
                .set(question.toWriteMap(true)).await()
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
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveExamPaper(item: ExamPaper, isCreate: Boolean): Result<Unit> {
        val docId = item.id.ifBlank { "paper_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            val ref = db.collection("exam_papers").document(docId)
            val writeAsCreate = isCreate || !ref.get().await().exists()
            if (writeAsCreate) ref.set(item.toWriteMap(true)).await() else ref.update(item.toWriteMap(false)).await()
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
                updateAttachmentsPublishedState(docId, item.published)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "exam_papers/$docId")
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
            requireUserId()
            val finalSizeLabel = if (drafts.isNotEmpty()) {
                AttachmentMediaHelper.buildAttachmentSummaryLabel(drafts, item.fileSizeLabel)
            } else if (isCreate || item.fileSizeLabel.contains("Image", true) || item.fileSizeLabel.contains("PDF", true)) {
                "Text Blueprint"
            } else {
                item.fileSizeLabel.ifBlank { "Text Blueprint" }
            }
            val finalItem = item.copy(id = docId, fileSizeLabel = finalSizeLabel)
            val ref = db.collection("exam_papers").document(docId)
            val writeAsCreate = isCreate || !ref.get().await().exists()
            if (writeAsCreate) ref.set(finalItem.toWriteMap(true)).await() else ref.update(finalItem.toWriteMap(false)).await()
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
            syncAttachmentsForParent(parentId = docId, published = finalItem.published, drafts = drafts)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "exam_papers/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteExamPaper(paperId: String): Result<Unit> {
        val res = deleteAdminDoc("exam_papers", paperId)
        if (res.isSuccess) {
            deleteRemoteAttachmentsForParent(paperId)
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
        if (parentId.isBlank()) return Result.success(emptyList())
        return try {
            requireUserId()
            val query = if (isAdmin) {
                db.collection("paper_attachments").whereEqualTo("parentId", parentId)
            } else {
                db.collection("paper_attachments")
                    .whereEqualTo("parentId", parentId)
                    .whereEqualTo("published", true)
            }
            val chunks = query.get().await()
                .toObjects(PaperAttachmentChunk::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            if (chunks.isEmpty()) {
                dao.deleteAttachmentsForParent(parentId)
                return Result.success(emptyList())
            }

            val grouped = chunks.groupBy { it.groupId }
            val resolved = ArrayList<ResolvedPaperAttachment>(grouped.size)
            val entities = ArrayList<CachedPaperAttachmentEntity>(grouped.size)

            for ((groupId, groupChunks) in grouped) {
                val first = groupChunks.minByOrNull { it.chunkIndex } ?: continue
                val file = AttachmentMediaHelper.decodeChunksToFile(
                    context = appContext,
                    parentId = parentId,
                    groupId = groupId,
                    fileName = first.fileName,
                    chunks = groupChunks
                )
                resolved.add(
                    ResolvedPaperAttachment(
                        groupId = groupId,
                        parentId = parentId,
                        kind = first.kind,
                        fileName = first.fileName,
                        mimeType = first.mimeType,
                        sizeBytes = first.sizeBytes,
                        sortOrder = first.sortOrder,
                        localFile = file
                    )
                )
                entities.add(
                    CachedPaperAttachmentEntity(
                        groupId = groupId,
                        parentId = parentId,
                        kind = first.kind,
                        fileName = first.fileName,
                        mimeType = first.mimeType,
                        sizeBytes = first.sizeBytes,
                        sortOrder = first.sortOrder,
                        localPath = file.absolutePath,
                        published = first.published
                    )
                )
            }

            dao.deleteAttachmentsForParent(parentId)
            dao.upsertPaperAttachments(entities)
            Result.success(resolved.sortedBy { it.sortOrder })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = dao.getAttachmentsForParentOnce(parentId)
                .filter { (isAdmin || it.published) && File(it.localPath).exists() }
                .map { it.toDomain() }
                .sortedBy { it.sortOrder }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                handleFirestoreError(e, OperationType.LIST, "paper_attachments/$parentId")
                Result.failure(e)
            }
        }
    }

    private suspend fun syncAttachmentsForParent(
        parentId: String,
        published: Boolean,
        drafts: List<PaperAttachmentDraft>
    ) {
        val existingRemoteDocs = db.collection("paper_attachments")
            .whereEqualTo("parentId", parentId)
            .get()
            .await()
            .documents

        val keepGroupIds = drafts.map { it.groupId }.toSet()
        for (doc in existingRemoteDocs) {
            val gId = doc.getString("groupId") ?: ""
            if (gId !in keepGroupIds) {
                doc.reference.delete().await()
            }
        }

        val cachedEntities = ArrayList<CachedPaperAttachmentEntity>(drafts.size)
        drafts.forEachIndexed { index, draft ->
            val sortOrder = index.toLong()
            val cachedFile = AttachmentMediaHelper.copyDraftToParentCache(appContext, parentId, draft)
            if (draft.isExistingRemote) {
                val matchingDocs = existingRemoteDocs.filter { it.getString("groupId") == draft.groupId }
                if (matchingDocs.isNotEmpty()) {
                    matchingDocs.forEach { doc ->
                        doc.reference.update(
                            mapOf(
                                "published" to published,
                                "sortOrder" to sortOrder,
                                "fileName" to draft.fileName.take(180),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } else {
                    uploadDraftChunks(parentId, published, sortOrder, draft, cachedFile)
                }
            } else {
                uploadDraftChunks(parentId, published, sortOrder, draft, cachedFile)
            }
            cachedEntities.add(
                CachedPaperAttachmentEntity(
                    groupId = draft.groupId,
                    parentId = parentId,
                    kind = draft.kind,
                    fileName = draft.fileName,
                    mimeType = draft.mimeType,
                    sizeBytes = draft.sizeBytes,
                    sortOrder = sortOrder,
                    localPath = cachedFile.absolutePath,
                    published = published
                )
            )
        }

        dao.getAttachmentsForParentOnce(parentId)
            .filter { it.groupId !in keepGroupIds }
            .forEach { stale -> runCatching { File(stale.localPath).delete() } }
        dao.deleteAttachmentsForParent(parentId)
        if (cachedEntities.isNotEmpty()) {
            dao.upsertPaperAttachments(cachedEntities)
        }
    }

    private suspend fun uploadDraftChunks(
        parentId: String,
        published: Boolean,
        sortOrder: Long,
        draft: PaperAttachmentDraft,
        sourceFile: File
    ) {
        val fileToRead = if (sourceFile.exists()) sourceFile else draft.previewFile
        val chunks = AttachmentMediaHelper.chunkFileToBase64(fileToRead)
        if (chunks.isEmpty()) {
            throw IllegalStateException("Attachment ${draft.fileName} is empty.")
        }
        if (chunks.size > 20) {
            throw IllegalStateException("Attachment ${draft.fileName} exceeds maximum size limit.")
        }
        val totalChunks = chunks.size.toLong()
        chunks.forEachIndexed { idx, base64 ->
            val chunkDocId = "${draft.groupId}_c$idx"
            val chunkObj = PaperAttachmentChunk(
                id = chunkDocId,
                parentId = parentId,
                groupId = draft.groupId,
                kind = draft.kind,
                fileName = draft.fileName,
                mimeType = draft.mimeType,
                sizeBytes = draft.sizeBytes,
                chunkIndex = idx.toLong(),
                totalChunks = totalChunks,
                sortOrder = sortOrder,
                published = published,
                dataBase64 = base64
            )
            db.collection("paper_attachments")
                .document(chunkDocId)
                .set(chunkObj.toWriteMap(isCreate = true))
                .await()
        }
    }

    private suspend fun updateAttachmentsPublishedState(parentId: String, published: Boolean) {
        runCatching {
            val remoteDocs = db.collection("paper_attachments")
                .whereEqualTo("parentId", parentId)
                .get()
                .await()
                .documents
            remoteDocs.forEach { doc ->
                doc.reference.update(
                    mapOf(
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val local = dao.getAttachmentsForParentOnce(parentId)
            if (local.isNotEmpty()) {
                dao.upsertPaperAttachments(local.map { it.copy(published = published) })
            }
        }
    }

    private suspend fun deleteRemoteAttachmentsForParent(parentId: String) {
        runCatching {
            val remoteDocs = db.collection("paper_attachments")
                .whereEqualTo("parentId", parentId)
                .get()
                .await()
                .documents
            remoteDocs.forEach { it.reference.delete().await() }
        }
    }

    suspend fun sendAnnouncement(item: AnnouncementItem): Result<Unit> {
        val docId = item.id.ifBlank { "ann_${System.currentTimeMillis()}" }
        return try {
            requireUserId()
            db.collection("announcements").document(docId).set(item.toWriteMap(true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "announcements/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveAppSettings(settings: AppSettings): Result<Unit> {
        val docId = "global"
        return try {
            requireUserId()
            val docRef = db.collection("app_settings").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(settings.toWriteMap(false)).await()
            } else {
                docRef.set(settings.toWriteMap(true)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_settings/$docId")
            Result.failure(e)
        }
    }

    suspend fun fetchAllUsersForAdmin(): Result<List<UserProfile>> {
        return try {
            requireUserId()
            val snap = db.collection("users").get().await()
            val list = snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    private suspend fun deleteAdminDoc(collection: String, id: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection(collection).document(id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$collection/$id")
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "momrooja@gmail.com"
        val BOOTSTRAP_ADMIN_EMAILS = setOf("momrooja@gmail.com", "abced6917@gmail.com")
    }
}
