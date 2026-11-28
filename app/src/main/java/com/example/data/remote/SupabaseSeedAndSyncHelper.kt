package com.example.data.remote

import android.content.Context
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
import com.example.data.local.StudyHubDao
import com.example.data.model.PaperAttachmentChunk
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SupabaseMappers
import com.example.data.model.SupabaseMappers.toSupabaseJson
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class SupabaseSeedAndSyncHelper(
    private val supabase: SupabaseRestClient,
    private val dao: StudyHubDao,
    private val appContext: Context
) {
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun markLocalSeedDone() {
        prefs.edit().putBoolean(KEY_LOCAL_SEED_DONE, true).apply()
    }

    fun markCloudSeedDone() {
        prefs.edit().putBoolean(KEY_CLOUD_SEED_DONE, true).apply()
    }

    suspend fun ensureLocalSeedData(force: Boolean = false) {
        if (!force && prefs.getBoolean(KEY_LOCAL_SEED_DONE, false)) return
        val existingClasses = dao.observeClasses().first()
        if (force || existingClasses.isEmpty()) {
            dao.upsertClasses(SeedCurriculumData.classes)
            dao.upsertSubjects(SeedCurriculumData.subjects)
            dao.upsertChapters(SeedCurriculumData.chapters)
            dao.upsertMaterials(SeedCurriculumData.materials)
            dao.upsertQuizzes(SeedCurriculumData.quizzes)
            dao.upsertQuestions(SeedCurriculumData.questions)
            dao.upsertExamPapers(SeedCurriculumData.examPapers)
        }
        markLocalSeedDone()
    }

    suspend fun syncCurriculumFromCloud(isAdmin: Boolean): Result<Unit> {
        if (!supabase.isConfigured()) return Result.success(Unit)
        return try {
            val publishedFilter = if (isAdmin) emptyMap() else mapOf("published" to "eq.true")

            val classesArr = supabase.select("classes", publishedFilter, order = "grade.asc")
            val subjectsArr = supabase.select("subjects", publishedFilter, order = "class_grade.asc,name.asc")
            val chaptersArr = supabase.select("chapters", publishedFilter, order = "class_grade.asc,chapter_number.asc")
            val materialsArr = supabase.select("study_materials", publishedFilter)
            val quizzesArr = supabase.select("quizzes", publishedFilter)
            val questionsArr = supabase.select("quiz_questions", publishedFilter)
            val papersArr = supabase.select("exam_papers", publishedFilter)

            val classes = (0 until classesArr.length()).map { i ->
                val c = SupabaseMappers.schoolClassFromJson(classesArr.getJSONObject(i))
                CachedClassEntity(c.id, c.grade, c.name, c.description, c.studentCount, c.published)
            }
            val subjects = (0 until subjectsArr.length()).map { i ->
                val s = SupabaseMappers.subjectFromJson(subjectsArr.getJSONObject(i))
                CachedSubjectEntity(s.id, s.classGrade, s.name, s.subtitle, s.iconKey, s.colorKey, s.chapterCount, s.published)
            }
            val chapters = (0 until chaptersArr.length()).map { i ->
                val ch = SupabaseMappers.chapterFromJson(chaptersArr.getJSONObject(i))
                CachedChapterEntity(ch.id, ch.classGrade, ch.subjectId, ch.subjectName, ch.chapterNumber, ch.title, ch.summary, ch.published)
            }
            val prevMaterials = dao.observeAllMaterials().first().associateBy { it.id }
            val materials = (0 until materialsArr.length()).map { i ->
                val m = SupabaseMappers.studyMaterialFromJson(materialsArr.getJSONObject(i))
                val prev = prevMaterials[m.id]
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
            }
            val quizzes = (0 until quizzesArr.length()).map { i ->
                val q = SupabaseMappers.quizFromJson(quizzesArr.getJSONObject(i))
                CachedQuizEntity(q.id, q.classGrade, q.subjectId, q.subjectName, q.chapterId, q.chapterTitle, q.title, q.description, q.durationMinutes, q.questionCount, q.premiumOnly, q.published)
            }
            val questions = (0 until questionsArr.length()).map { i ->
                val qq = SupabaseMappers.quizQuestionFromJson(questionsArr.getJSONObject(i))
                CachedQuestionEntity(
                    id = qq.id,
                    quizId = qq.quizId,
                    questionType = qq.questionType,
                    prompt = qq.prompt,
                    optionsJoined = qq.options.joinToString("||"),
                    correctIndicesJoined = qq.correctIndices.joinToString(","),
                    correctText = qq.correctText,
                    explanation = qq.explanation,
                    points = qq.points,
                    published = qq.published
                )
            }
            val prevPapers = dao.observeAllExamPapers().first().associateBy { it.id }
            val papers = (0 until papersArr.length()).map { i ->
                val p = SupabaseMappers.examPaperFromJson(papersArr.getJSONObject(i))
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
            }

            val hasAnyCloudContent = classes.isNotEmpty() ||
                subjects.isNotEmpty() ||
                chapters.isNotEmpty() ||
                materials.isNotEmpty() ||
                quizzes.isNotEmpty() ||
                questions.isNotEmpty() ||
                papers.isNotEmpty()
            val cloudInitialized = prefs.getBoolean(KEY_CLOUD_SEED_DONE, false) ||
                hasAnyCloudContent ||
                runCatching {
                    supabase.select("app_settings", selectCols = "id", limit = 1).length() > 0 ||
                        supabase.select("announcements", selectCols = "id", limit = 1).length() > 0 ||
                        (!isAdmin && supabase.select("classes", selectCols = "id", limit = 1).length() > 0)
                }.getOrDefault(false)

            if (cloudInitialized) {
                markCloudSeedDone()
                markLocalSeedDone()
                dao.replaceClasses(classes)
                dao.replaceSubjects(subjects)
                dao.replaceChapters(chapters)
                dao.replaceMaterials(materials)
                dao.replaceQuizzes(quizzes)
                dao.replaceQuestions(questions)
                dao.replaceExamPapers(papers)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.LIST, "curriculum_sync")
            Result.failure(e)
        }
    }

    suspend fun syncUserSubcollectionsFromCloud(uid: String): Result<Unit> {
        if (!supabase.isConfigured()) return Result.success(Unit)
        return try {
            val userFilter = mapOf("user_id" to "eq.$uid")
            val hasPendingSync = dao.getPendingSyncOnce(uid).isNotEmpty()

            val bookmarksArr = supabase.select("bookmarks", userFilter)
            val bookmarks = (0 until bookmarksArr.length()).map { i ->
                val b = SupabaseMappers.bookmarkFromJson(bookmarksArr.getJSONObject(i), uid)
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
                    savedAtMillis = SupabaseMappers.parseIsoToMillis(b.updatedAt ?: b.createdAt)
                )
            }
            if (!hasPendingSync) {
                dao.replaceUserBookmarks(uid, bookmarks)
            } else if (bookmarks.isNotEmpty()) {
                dao.upsertBookmarks(bookmarks)
            }

            val attemptsArr = supabase.select("quiz_attempts", userFilter)
            val attempts = (0 until attemptsArr.length()).map { i ->
                val a = SupabaseMappers.quizAttemptFromJson(attemptsArr.getJSONObject(i), uid)
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
                    attemptedAtMillis = a.attemptedAtMillis
                )
            }
            if (!hasPendingSync) {
                dao.replaceUserQuizAttempts(uid, attempts)
            } else if (attempts.isNotEmpty()) {
                dao.upsertQuizAttempts(attempts)
            }

            val progressArr = supabase.select("reading_progress", userFilter)
            val progress = (0 until progressArr.length()).map { i ->
                val p = SupabaseMappers.readingProgressFromJson(progressArr.getJSONObject(i), uid)
                CachedReadingProgressEntity(
                    id = p.id,
                    userId = uid,
                    materialId = p.materialId,
                    title = p.title,
                    subjectName = p.subjectName,
                    chapterTitle = p.chapterTitle,
                    classGrade = p.classGrade,
                    progressPercent = p.progressPercent,
                    updatedAtMillis = p.updatedAtMillis
                )
            }
            if (!hasPendingSync) {
                dao.replaceUserReadingProgress(uid, progress)
            } else if (progress.isNotEmpty()) {
                dao.upsertReadingProgressList(progress)
            }

            val downloadsArr = supabase.select("downloads", userFilter)
            val downloads = (0 until downloadsArr.length()).map { i ->
                val d = SupabaseMappers.downloadFromJson(downloadsArr.getJSONObject(i), uid)
                CachedDownloadEntity(
                    id = d.id,
                    userId = uid,
                    materialId = d.materialId,
                    title = d.title,
                    subjectName = d.subjectName,
                    classGrade = d.classGrade,
                    fileSizeLabel = d.fileSizeLabel,
                    downloadedAtMillis = SupabaseMappers.parseIsoToMillis(d.updatedAt ?: d.createdAt)
                )
            }
            if (!hasPendingSync) {
                dao.replaceUserDownloads(uid, downloads)
            } else if (downloads.isNotEmpty()) {
                dao.upsertDownloads(downloads)
                downloads.forEach { d -> dao.setMaterialDownloaded(d.materialId, true, System.currentTimeMillis()) }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.LIST, "user_subcollections/$uid")
            Result.failure(e)
        }
    }

    suspend fun seedCurriculumToCloudIfEmpty(): Result<Unit> {
        if (!supabase.isConfigured()) return Result.success(Unit)
        if (prefs.getBoolean(KEY_CLOUD_SEED_DONE, false)) return Result.success(Unit)
        return try {
            val existingClasses = supabase.select("classes", selectCols = "id", limit = 1)
            val existingSettings = supabase.select("app_settings", selectCols = "id", limit = 1)
            val existingSubjects = supabase.select("subjects", selectCols = "id", limit = 1)
            if (existingClasses.length() > 0 || existingSettings.length() > 0 || existingSubjects.length() > 0) {
                markCloudSeedDone()
                return Result.success(Unit)
            }
            seedOrRepairCloudCurriculum().map { Unit }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "seed_curriculum")
            Result.failure(e)
        }
    }

    suspend fun seedOrRepairCloudCurriculum(): Result<Int> {
        if (!supabase.isConfigured()) {
            return Result.failure(
                IllegalStateException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.")
            )
        }
        return try {
            var createdCount = 0

            suspend fun seedMissing(table: String, items: List<Pair<String, JSONObject>>) {
                val existingArr = supabase.select(table, selectCols = "id")
                val existingIds = (0 until existingArr.length()).mapNotNull {
                    existingArr.optJSONObject(it)?.optString("id")?.takeIf { id -> id.isNotBlank() }
                }.toSet()
                val missing = JSONArray()
                items.filter { it.first !in existingIds }.forEach { (_, json) ->
                    missing.put(json)
                    createdCount++
                }
                if (missing.length() > 0) {
                    supabase.upsert(table, missing)
                }
            }

            seedMissing("classes", SeedCurriculumData.classes.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("subjects", SeedCurriculumData.subjects.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("chapters", SeedCurriculumData.chapters.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("study_materials", SeedCurriculumData.materials.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("quizzes", SeedCurriculumData.quizzes.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("quiz_questions", SeedCurriculumData.questions.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("exam_papers", SeedCurriculumData.examPapers.map { it.id to it.toDomain().toSupabaseJson() })
            seedMissing("announcements", SeedCurriculumData.defaultAnnouncements.map { it.id to it.toSupabaseJson() })
            seedMissing("app_settings", listOf("global" to SeedCurriculumData.defaultAppSettings.toSupabaseJson()))

            markCloudSeedDone()
            Result.success(createdCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleCloudError(e, OperationType.WRITE, "repair_curriculum")
            Result.failure(e)
        }
    }

    suspend fun loadAttachmentsForParent(
        parentId: String,
        isAdmin: Boolean
    ): Result<List<ResolvedPaperAttachment>> {
        if (parentId.isBlank()) return Result.success(emptyList())
        return try {
            if (!supabase.isConfigured()) {
                val cached = dao.getAttachmentsForParentOnce(parentId)
                    .filter { (isAdmin || it.published) && File(it.localPath).exists() }
                    .map { it.toDomain() }
                    .sortedBy { it.sortOrder }
                return Result.success(cached)
            }
            val filters = buildMap {
                put("parent_id", "eq.$parentId")
                if (!isAdmin) put("published", "eq.true")
            }
            val arr = supabase.select("paper_attachments", filters, order = "sort_order.asc,chunk_index.asc")
            val chunks = (0 until arr.length()).map { i ->
                SupabaseMappers.paperAttachmentChunkFromJson(arr.getJSONObject(i))
            }

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
                handleCloudError(e, OperationType.LIST, "paper_attachments/$parentId")
                Result.failure(e)
            }
        }
    }

    suspend fun syncAttachmentsForParent(
        parentId: String,
        published: Boolean,
        drafts: List<PaperAttachmentDraft>
    ) {
        val existingArr = if (supabase.isConfigured()) {
            supabase.select("paper_attachments", mapOf("parent_id" to "eq.$parentId"), selectCols = "id,group_id")
        } else {
            JSONArray()
        }
        val existingGroups = (0 until existingArr.length())
            .mapNotNull { existingArr.optJSONObject(it)?.optString("group_id")?.takeIf { g -> g.isNotBlank() } }
            .toSet()

        val keepGroupIds = drafts.map { it.groupId }.toSet()
        if (supabase.isConfigured()) {
            existingGroups.filter { it !in keepGroupIds }.forEach { staleGroup ->
                supabase.deleteWhere("paper_attachments", mapOf("group_id" to "eq.$staleGroup"))
            }
        }

        val cachedEntities = ArrayList<CachedPaperAttachmentEntity>(drafts.size)
        drafts.forEachIndexed { index, draft ->
            val sortOrder = index.toLong()
            val cachedFile = AttachmentMediaHelper.copyDraftToParentCache(appContext, parentId, draft)
            if (supabase.isConfigured()) {
                if (draft.isExistingRemote && draft.groupId in existingGroups) {
                    supabase.updateWhere(
                        table = "paper_attachments",
                        filters = mapOf("group_id" to "eq.${draft.groupId}"),
                        bodyJson = JSONObject().apply {
                            put("published", published)
                            put("sort_order", sortOrder)
                            put("file_name", draft.fileName.take(180))
                            put("updated_at", SupabaseMappers.nowIso())
                        }
                    )
                } else {
                    uploadDraftChunks(parentId, published, sortOrder, draft, cachedFile)
                }
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
        val rows = JSONArray()
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
            rows.put(chunkObj.toSupabaseJson(idOverride = chunkDocId))
        }
        supabase.upsert("paper_attachments", rows)
    }

    suspend fun updateAttachmentsPublishedState(parentId: String, published: Boolean) {
        runCatching {
            if (supabase.isConfigured()) {
                supabase.updateWhere(
                    table = "paper_attachments",
                    filters = mapOf("parent_id" to "eq.$parentId"),
                    bodyJson = JSONObject().apply {
                        put("published", published)
                        put("updated_at", SupabaseMappers.nowIso())
                    }
                )
            }
            val local = dao.getAttachmentsForParentOnce(parentId)
            if (local.isNotEmpty()) {
                dao.upsertPaperAttachments(local.map { it.copy(published = published) })
            }
        }
    }

    suspend fun deleteRemoteAttachmentsForParent(parentId: String) {
        runCatching {
            if (supabase.isConfigured()) {
                supabase.deleteWhere("paper_attachments", mapOf("parent_id" to "eq.$parentId"))
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "studyhub_prefs"
        private const val KEY_LOCAL_SEED_DONE = "local_seed_done"
        private const val KEY_CLOUD_SEED_DONE = "cloud_seed_done"
    }
}
