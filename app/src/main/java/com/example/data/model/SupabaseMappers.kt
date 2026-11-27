package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

object SupabaseMappers {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun nowIso(): String = synchronized(isoFormat) { isoFormat.format(Date()) }

    fun parseIsoToMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        val normalized = raw.substringBefore('.').substringBefore('+').removeSuffix("Z")
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            parser.parse(normalized)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }

    private fun JSONObject.str(key: String, default: String = ""): String =
        if (has(key) && !isNull(key)) optString(key, default) else default

    private fun JSONObject.strNullable(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private fun JSONObject.lng(key: String, default: Long = 0L): Long =
        if (has(key) && !isNull(key)) optLong(key, default) else default

    private fun JSONObject.bool(key: String, default: Boolean = false): Boolean =
        if (has(key) && !isNull(key)) optBoolean(key, default) else default

    private fun JSONObject.strList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private fun JSONObject.longList(key: String): List<Long> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                add(arr.optLong(i))
            }
        }
    }

    fun UserProfile.toSupabaseJson(idOverride: String = id.ifBlank { userId }): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("user_id", userId.ifBlank { idOverride })
        put("display_name", displayName.take(80).ifBlank { "Student" })
        put("email", email.take(160))
        put("selected_class", selectedClass.coerceIn(6L, 12L))
        put("role", if (role == "admin") "admin" else "student")
        put("subscription_tier", if (subscriptionTier == "premium") "premium" else "free")
        put("avg_score", avgScore.coerceIn(0L, 100L))
        put("day_streak", dayStreak.coerceIn(0L, 10000L))
        put("points", points.coerceIn(0L, 10000000L))
        put("daily_goal_count", dailyGoalCount.coerceIn(1L, 50L))
        put("daily_completed_count", dailyCompletedCount.coerceIn(0L, 100L))
        put("dark_theme_enabled", darkThemeEnabled)
        put("notifications_enabled", notificationsEnabled)
        put("exam_reminders_enabled", examRemindersEnabled)
        put("new_materials_enabled", newMaterialsEnabled)
        put("updated_at", nowIso())
    }

    fun userProfileFromJson(j: JSONObject): UserProfile = UserProfile(
        id = j.str("id"),
        userId = j.str("user_id", j.str("id")),
        displayName = j.str("display_name", "Student"),
        email = j.str("email"),
        selectedClass = j.lng("selected_class", 7L),
        role = j.str("role", "student"),
        subscriptionTier = j.str("subscription_tier", "free"),
        avgScore = j.lng("avg_score", 87L),
        dayStreak = j.lng("day_streak", 15L),
        points = j.lng("points", 120L),
        dailyGoalCount = j.lng("daily_goal_count", 5L),
        dailyCompletedCount = j.lng("daily_completed_count", 3L),
        darkThemeEnabled = j.bool("dark_theme_enabled", false),
        notificationsEnabled = j.bool("notifications_enabled", true),
        examRemindersEnabled = j.bool("exam_reminders_enabled", true),
        newMaterialsEnabled = j.bool("new_materials_enabled", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun userProfilePatchToSnakeCase(fields: Map<String, Any>): JSONObject = JSONObject().apply {
        fields.forEach { (key, value) ->
            val col = when (key) {
                "displayName" -> "display_name"
                "selectedClass" -> "selected_class"
                "subscriptionTier" -> "subscription_tier"
                "avgScore" -> "avg_score"
                "dayStreak" -> "day_streak"
                "dailyGoalCount" -> "daily_goal_count"
                "dailyCompletedCount" -> "daily_completed_count"
                "darkThemeEnabled" -> "dark_theme_enabled"
                "notificationsEnabled" -> "notifications_enabled"
                "examRemindersEnabled" -> "exam_reminders_enabled"
                "newMaterialsEnabled" -> "new_materials_enabled"
                else -> key
            }
            if (col != "userId" && col != "user_id" && col != "createdAt" && col != "created_at") {
                put(col, value)
            }
        }
        put("updated_at", nowIso())
    }

    fun SchoolClass.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("grade", grade.coerceIn(6L, 12L))
        put("name", name.take(80).ifBlank { "Class $grade" })
        put("description", description.take(300))
        put("student_count", studentCount.coerceAtLeast(0L))
        put("published", published)
        put("updated_at", nowIso())
    }

    fun schoolClassFromJson(j: JSONObject): SchoolClass = SchoolClass(
        id = j.str("id"),
        grade = j.lng("grade", 7L),
        name = j.str("name", "Class 7"),
        description = j.str("description"),
        studentCount = j.lng("student_count", 0L),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun SubjectItem.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("class_grade", classGrade.coerceIn(6L, 12L))
        put("name", name.take(80).ifBlank { "Subject" })
        put("subtitle", subtitle.take(140))
        put("icon_key", iconKey.take(40).ifBlank { "book" })
        put("color_key", colorKey.take(40).ifBlank { "blue" })
        put("chapter_count", chapterCount.coerceIn(0L, 200L))
        put("published", published)
        put("updated_at", nowIso())
    }

    fun subjectFromJson(j: JSONObject): SubjectItem = SubjectItem(
        id = j.str("id"),
        classGrade = j.lng("class_grade", 7L),
        name = j.str("name"),
        subtitle = j.str("subtitle"),
        iconKey = j.str("icon_key", "book"),
        colorKey = j.str("color_key", "blue"),
        chapterCount = j.lng("chapter_count", 0L),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun ChapterItem.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("class_grade", classGrade.coerceIn(6L, 12L))
        put("subject_id", subjectId.take(128))
        put("subject_name", subjectName.take(80))
        put("chapter_number", chapterNumber.coerceIn(1L, 200L))
        put("title", title.take(180))
        put("summary", summary.take(1000))
        put("published", published)
        put("updated_at", nowIso())
    }

    fun chapterFromJson(j: JSONObject): ChapterItem = ChapterItem(
        id = j.str("id"),
        classGrade = j.lng("class_grade", 7L),
        subjectId = j.str("subject_id"),
        subjectName = j.str("subject_name"),
        chapterNumber = j.lng("chapter_number", 1L),
        title = j.str("title"),
        summary = j.str("summary"),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun StudyMaterial.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("class_grade", classGrade.coerceIn(6L, 12L))
        put("subject_id", subjectId.take(128))
        put("subject_name", subjectName.take(80))
        put("chapter_id", chapterId.take(128))
        put("chapter_title", chapterTitle.take(180))
        put("title", title.take(180))
        put("description", description.take(500))
        put("material_type", MaterialType.fromKey(materialType).key)
        put("content_markdown", contentMarkdown.take(30000).ifBlank { description })
        put("file_size_label", fileSizeLabel.take(40).ifBlank { "1.0 MB" })
        put("tags", JSONArray(tags.take(10).map { it.take(32) }))
        put("important", important)
        put("premium_only", premiumOnly)
        put("published", published)
        put("updated_at", nowIso())
    }

    fun studyMaterialFromJson(j: JSONObject): StudyMaterial = StudyMaterial(
        id = j.str("id"),
        classGrade = j.lng("class_grade", 7L),
        subjectId = j.str("subject_id"),
        subjectName = j.str("subject_name"),
        chapterId = j.str("chapter_id"),
        chapterTitle = j.str("chapter_title"),
        title = j.str("title"),
        description = j.str("description"),
        materialType = j.str("material_type", MaterialType.SHORT_NOTES.key),
        contentMarkdown = j.str("content_markdown"),
        fileSizeLabel = j.str("file_size_label", "1.0 MB"),
        tags = j.strList("tags"),
        important = j.bool("important", false),
        premiumOnly = j.bool("premium_only", false),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun QuizItem.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("class_grade", classGrade.coerceIn(6L, 12L))
        put("subject_id", subjectId.take(128))
        put("subject_name", subjectName.take(80))
        put("chapter_id", chapterId.take(128))
        put("chapter_title", chapterTitle.take(180))
        put("title", title.take(180))
        put("description", description.take(500))
        put("duration_minutes", durationMinutes.coerceIn(1L, 240L))
        put("question_count", questionCount.coerceIn(0L, 200L))
        put("premium_only", premiumOnly)
        put("published", published)
        put("updated_at", nowIso())
    }

    fun quizFromJson(j: JSONObject): QuizItem = QuizItem(
        id = j.str("id"),
        classGrade = j.lng("class_grade", 7L),
        subjectId = j.str("subject_id"),
        subjectName = j.str("subject_name"),
        chapterId = j.str("chapter_id"),
        chapterTitle = j.str("chapter_title"),
        title = j.str("title"),
        description = j.str("description"),
        durationMinutes = j.lng("duration_minutes", 10L),
        questionCount = j.lng("question_count", 5L),
        premiumOnly = j.bool("premium_only", false),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun QuizQuestion.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("quiz_id", quizId.take(128))
        put("question_type", QuestionType.fromKey(questionType).key)
        put("prompt", prompt.take(1000))
        put("options", JSONArray(options.take(6).map { it.take(200) }))
        put("correct_indices", JSONArray(correctIndices.take(6)))
        put("correct_text", correctText.take(200))
        put("explanation", explanation.take(1000))
        put("points", points.coerceIn(1L, 100L))
        put("published", published)
        put("updated_at", nowIso())
    }

    fun quizQuestionFromJson(j: JSONObject): QuizQuestion = QuizQuestion(
        id = j.str("id"),
        quizId = j.str("quiz_id"),
        questionType = j.str("question_type", QuestionType.MCQ.key),
        prompt = j.str("prompt"),
        options = j.strList("options"),
        correctIndices = j.longList("correct_indices"),
        correctText = j.str("correct_text"),
        explanation = j.str("explanation"),
        points = j.lng("points", 10L),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun ExamPaper.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("class_grade", classGrade.coerceIn(6L, 12L))
        put("subject_id", subjectId.take(128))
        put("subject_name", subjectName.take(80))
        put("title", title.take(180))
        put("paper_category", PaperCategory.fromKey(paperCategory).key)
        put("year", year.coerceIn(2000L, 2035L))
        put("file_size_label", fileSizeLabel.take(40))
        put("content_text", contentText.take(30000))
        put("published", published)
        put("updated_at", nowIso())
    }

    fun examPaperFromJson(j: JSONObject): ExamPaper = ExamPaper(
        id = j.str("id"),
        classGrade = j.lng("class_grade", 7L),
        subjectId = j.str("subject_id"),
        subjectName = j.str("subject_name"),
        title = j.str("title"),
        paperCategory = j.str("paper_category", PaperCategory.PREVIOUS_YEAR.key),
        year = j.lng("year", 2025L),
        fileSizeLabel = j.str("file_size_label", "2.4 MB"),
        contentText = j.str("content_text"),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun PaperAttachmentChunk.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("parent_id", parentId.take(128))
        put("group_id", groupId.take(128))
        put("kind", if (kind == "pdf") "pdf" else "image")
        put("file_name", fileName.take(180).ifBlank { "attachment" })
        put("mime_type", mimeType.take(80).ifBlank { "application/octet-stream" })
        put("size_bytes", sizeBytes.coerceIn(1L, 6_000_000L))
        put("chunk_index", chunkIndex.coerceIn(0L, 20L))
        put("total_chunks", totalChunks.coerceIn(1L, 20L))
        put("sort_order", sortOrder.coerceIn(0L, 100L))
        put("published", published)
        put("data_base64", dataBase64.take(750_000))
        put("updated_at", nowIso())
    }

    fun paperAttachmentChunkFromJson(j: JSONObject): PaperAttachmentChunk = PaperAttachmentChunk(
        id = j.str("id"),
        parentId = j.str("parent_id"),
        groupId = j.str("group_id"),
        kind = j.str("kind", "image"),
        fileName = j.str("file_name"),
        mimeType = j.str("mime_type", "image/jpeg"),
        sizeBytes = j.lng("size_bytes", 0L),
        chunkIndex = j.lng("chunk_index", 0L),
        totalChunks = j.lng("total_chunks", 1L),
        sortOrder = j.lng("sort_order", 0L),
        published = j.bool("published", true),
        dataBase64 = j.str("data_base64"),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun AnnouncementItem.toSupabaseJson(idOverride: String = id): JSONObject = JSONObject().apply {
        put("id", idOverride)
        put("title", title.take(140))
        put("message", message.take(1000))
        put("target_class", targetClass.coerceIn(0L, 12L))
        put("priority", if (priority == "high") "high" else "normal")
        put("published", published)
        put("updated_at", nowIso())
    }

    fun announcementFromJson(j: JSONObject): AnnouncementItem = AnnouncementItem(
        id = j.str("id"),
        title = j.str("title"),
        message = j.str("message"),
        targetClass = j.lng("target_class", 0L),
        priority = j.str("priority", "normal"),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun AppSettings.toSupabaseJson(): JSONObject = JSONObject().apply {
        put("id", id.ifBlank { "global" })
        put("premium_monthly_price", premiumMonthlyPrice.trim().take(30).ifBlank { "$4.99/mo" })
        put("premium_yearly_price", premiumYearlyPrice.trim().take(30).ifBlank { "$39.99/yr" })
        put("ads_enabled", adsEnabled)
        put("ad_family_safe_mode", adFamilySafeMode)
        put("ad_placement_home", adPlacementHome)
        put("ad_placement_materials", adPlacementMaterials)
        put("ad_placement_quizzes", adPlacementQuizzes)
        put("ad_placement_papers", adPlacementPapers)
        put("ad_banner_title", adBannerTitle.trim().take(120).ifBlank { "StudyHub Plus • Family-Safe Learning" })
        put("ad_banner_subtitle", adBannerSubtitle.trim().take(240).ifBlank { "Unlock unlimited offline packs, advanced analytics & ad-free mode." })
        put("ad_cta_label", adCtaLabel.trim().take(30).ifBlank { "Upgrade" })
        put("ad_network_provider", adNetworkProvider.trim().take(60).ifBlank { "StudyHub Direct (Family-Safe)" })
        put("maintenance_mode", maintenanceMode)
        put("support_email", supportEmail.trim().take(120).ifBlank { "support@studyhub.edu" })
        put("published", published)
        put("updated_at", nowIso())
    }

    fun appSettingsFromJson(j: JSONObject): AppSettings = AppSettings(
        id = j.str("id", "global"),
        premiumMonthlyPrice = j.str("premium_monthly_price", "$4.99/mo"),
        premiumYearlyPrice = j.str("premium_yearly_price", "$39.99/yr"),
        adsEnabled = j.bool("ads_enabled", true),
        adFamilySafeMode = j.bool("ad_family_safe_mode", true),
        adPlacementHome = j.bool("ad_placement_home", true),
        adPlacementMaterials = j.bool("ad_placement_materials", true),
        adPlacementQuizzes = j.bool("ad_placement_quizzes", false),
        adPlacementPapers = j.bool("ad_placement_papers", true),
        adBannerTitle = j.str("ad_banner_title", "StudyHub Plus • Family-Safe Learning"),
        adBannerSubtitle = j.str("ad_banner_subtitle", "Unlock unlimited offline packs, advanced analytics & ad-free mode."),
        adCtaLabel = j.str("ad_cta_label", "Upgrade"),
        adNetworkProvider = j.str("ad_network_provider", "StudyHub Direct (Family-Safe)"),
        maintenanceMode = j.bool("maintenance_mode", false),
        supportEmail = j.str("support_email", "support@studyhub.edu"),
        published = j.bool("published", true),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun bookmarkFromJson(j: JSONObject, uid: String): BookmarkRecord = BookmarkRecord(
        id = j.str("id").removePrefix("${uid}__"),
        materialId = j.str("material_id"),
        title = j.str("title"),
        subjectName = j.str("subject_name"),
        classGrade = j.lng("class_grade", 7L),
        materialType = j.str("material_type", MaterialType.SHORT_NOTES.key),
        subtitle = j.str("subtitle"),
        accentKey = j.str("accent_key", "blue"),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun quizAttemptFromJson(j: JSONObject, uid: String): QuizAttemptRecord = QuizAttemptRecord(
        id = j.str("id").removePrefix("${uid}__"),
        quizId = j.str("quiz_id"),
        quizTitle = j.str("quiz_title"),
        subjectName = j.str("subject_name"),
        classGrade = j.lng("class_grade", 7L),
        mode = j.str("mode", "practice"),
        totalQuestions = j.lng("total_questions", 0L),
        correctAnswers = j.lng("correct_answers", 0L),
        scorePercent = j.lng("score_percent", 0L),
        durationSeconds = j.lng("duration_seconds", 0L),
        pointsEarned = j.lng("points_earned", 0L),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun readingProgressFromJson(j: JSONObject, uid: String): ReadingProgressRecord = ReadingProgressRecord(
        id = j.str("id").removePrefix("${uid}__"),
        materialId = j.str("material_id"),
        title = j.str("title"),
        subjectName = j.str("subject_name"),
        chapterTitle = j.str("chapter_title"),
        classGrade = j.lng("class_grade", 7L),
        progressPercent = j.lng("progress_percent", 0L),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )

    fun downloadFromJson(j: JSONObject, uid: String): DownloadRecord = DownloadRecord(
        id = j.str("id").removePrefix("${uid}__"),
        materialId = j.str("material_id"),
        title = j.str("title"),
        subjectName = j.str("subject_name"),
        classGrade = j.lng("class_grade", 7L),
        fileSizeLabel = j.str("file_size_label", "1.5 MB"),
        createdAt = j.strNullable("created_at"),
        updatedAt = j.strNullable("updated_at")
    )
}
