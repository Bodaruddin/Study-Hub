package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SyncStatus(val label: String) {
    ONLINE("Online"),
    OFFLINE("Offline"),
    SYNCING("Syncing"),
    SYNCED("Synced"),
    SYNC_FAILED("Sync failed")
}

enum class MaterialType(val key: String, val label: String) {
    SHORT_NOTES("short_notes", "Short Notes"),
    DETAILED_NOTES("detailed_notes", "Detailed Notes"),
    CHAPTER_SUMMARY("chapter_summary", "Chapter Summary"),
    QA_SET("qa_set", "Q&A Set"),
    IMPORTANT_QUESTIONS("important_questions", "Important Questions"),
    PDF_GUIDE("pdf_guide", "PDF Guide"),
    TEXT_LESSON("text_lesson", "Text Lesson"),
    IMAGE_LESSON("image_lesson", "Image Lesson"),
    REVISION_NOTES("revision_notes", "Revision Notes"),
    STUDY_GUIDE("study_guide", "Study Guide"),
    MIND_MAP("mind_map", "Mind Maps");

    companion object {
        fun fromKey(raw: String): MaterialType =
            entries.firstOrNull { it.key == raw } ?: SHORT_NOTES
    }
}

enum class QuestionType(val key: String, val label: String) {
    MCQ("mcq", "Multiple Choice"),
    MULTI_SELECT("multi_select", "Multiple Answers"),
    TRUE_FALSE("true_false", "True / False"),
    FILL_BLANK("fill_blank", "Fill in the Blank");

    companion object {
        fun fromKey(raw: String): QuestionType =
            entries.firstOrNull { it.key == raw } ?: MCQ
    }
}

enum class PaperCategory(val key: String, val label: String) {
    PREVIOUS_YEAR("previous_year", "Previous Year"),
    MODEL_PAPER("model_paper", "Model Papers"),
    SAMPLE_PAPER("sample_paper", "Sample Papers"),
    IMPORTANT_QUESTIONS("important_questions", "Important Qs");

    companion object {
        fun fromKey(raw: String): PaperCategory =
            entries.firstOrNull { it.key == raw } ?: PREVIOUS_YEAR
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val email: String = "",
    val selectedClass: Long = 7L,
    val role: String = "student",
    val subscriptionTier: String = "free",
    val avgScore: Long = 87L,
    val dayStreak: Long = 15L,
    val points: Long = 120L,
    val dailyGoalCount: Long = 5L,
    val dailyCompletedCount: Long = 3L,
    val darkThemeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val examRemindersEnabled: Boolean = true,
    val newMaterialsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80).ifBlank { "Student" },
        "email" to email.take(160),
        "selectedClass" to selectedClass.coerceIn(6L, 12L),
        "role" to if (role == "admin") "admin" else "student",
        "subscriptionTier" to if (subscriptionTier == "premium") "premium" else "free",
        "avgScore" to avgScore.coerceIn(0L, 100L),
        "dayStreak" to dayStreak.coerceIn(0L, 10000L),
        "points" to points.coerceIn(0L, 10000000L),
        "dailyGoalCount" to dailyGoalCount.coerceIn(1L, 50L),
        "dailyCompletedCount" to dailyCompletedCount.coerceIn(0L, 100L),
        "darkThemeEnabled" to darkThemeEnabled,
        "notificationsEnabled" to notificationsEnabled,
        "examRemindersEnabled" to examRemindersEnabled,
        "newMaterialsEnabled" to newMaterialsEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SchoolClass(
    @DocumentId val id: String = "",
    val grade: Long = 7L,
    val name: String = "Class 7",
    val description: String = "",
    val studentCount: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("grade", grade.coerceIn(6L, 12L))
        put("name", name.take(80).ifBlank { "Class $grade" })
        put("description", description.take(300))
        put("studentCount", studentCount.coerceAtLeast(0L))
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class SubjectItem(
    @DocumentId val id: String = "",
    val classGrade: Long = 7L,
    val name: String = "",
    val subtitle: String = "",
    val iconKey: String = "book",
    val colorKey: String = "blue",
    val chapterCount: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("classGrade", classGrade.coerceIn(6L, 12L))
        put("name", name.take(80).ifBlank { "Subject" })
        put("subtitle", subtitle.take(140))
        put("iconKey", iconKey.take(40).ifBlank { "book" })
        put("colorKey", colorKey.take(40).ifBlank { "blue" })
        put("chapterCount", chapterCount.coerceIn(0L, 200L))
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class ChapterItem(
    @DocumentId val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterNumber: Long = 1L,
    val title: String = "",
    val summary: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("classGrade", classGrade.coerceIn(6L, 12L))
        put("subjectId", subjectId.take(128))
        put("subjectName", subjectName.take(80))
        put("chapterNumber", chapterNumber.coerceIn(1L, 200L))
        put("title", title.take(180))
        put("summary", summary.take(1000))
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class StudyMaterial(
    @DocumentId val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterTitle: String = "",
    val title: String = "",
    val description: String = "",
    val materialType: String = MaterialType.SHORT_NOTES.key,
    val contentMarkdown: String = "",
    val fileSizeLabel: String = "1.2 MB",
    val tags: List<String> = emptyList(),
    val important: Boolean = false,
    val premiumOnly: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("classGrade", classGrade.coerceIn(6L, 12L))
        put("subjectId", subjectId.take(128))
        put("subjectName", subjectName.take(80))
        put("chapterId", chapterId.take(128))
        put("chapterTitle", chapterTitle.take(180))
        put("title", title.take(180))
        put("description", description.take(500))
        put("materialType", MaterialType.fromKey(materialType).key)
        put("contentMarkdown", contentMarkdown.take(30000).ifBlank { description })
        put("fileSizeLabel", fileSizeLabel.take(40).ifBlank { "1.0 MB" })
        put("tags", tags.take(10).map { it.take(32) })
        put("important", important)
        put("premiumOnly", premiumOnly)
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class QuizItem(
    @DocumentId val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterId: String = "",
    val chapterTitle: String = "",
    val title: String = "",
    val description: String = "",
    val durationMinutes: Long = 10L,
    val questionCount: Long = 5L,
    val premiumOnly: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("classGrade", classGrade.coerceIn(6L, 12L))
        put("subjectId", subjectId.take(128))
        put("subjectName", subjectName.take(80))
        put("chapterId", chapterId.take(128))
        put("chapterTitle", chapterTitle.take(180))
        put("title", title.take(180))
        put("description", description.take(500))
        put("durationMinutes", durationMinutes.coerceIn(1L, 240L))
        put("questionCount", questionCount.coerceIn(0L, 200L))
        put("premiumOnly", premiumOnly)
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val questionType: String = QuestionType.MCQ.key,
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndices: List<Long> = emptyList(),
    val correctText: String = "",
    val explanation: String = "",
    val points: Long = 10L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        if (isCreate) put("quizId", quizId.take(128))
        put("questionType", QuestionType.fromKey(questionType).key)
        put("prompt", prompt.take(1000))
        put("options", options.take(6).map { it.take(200) })
        put("correctIndices", correctIndices.take(6))
        put("correctText", correctText.take(200))
        put("explanation", explanation.take(1000))
        put("points", points.coerceIn(1L, 100L))
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class ExamPaper(
    @DocumentId val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val paperCategory: String = PaperCategory.PREVIOUS_YEAR.key,
    val year: Long = 2025L,
    val fileSizeLabel: String = "2.4 MB",
    val contentText: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("classGrade", classGrade.coerceIn(6L, 12L))
        put("subjectId", subjectId.take(128))
        put("subjectName", subjectName.take(80))
        put("title", title.take(180))
        put("paperCategory", PaperCategory.fromKey(paperCategory).key)
        put("year", year.coerceIn(2000L, 2035L))
        put("fileSizeLabel", fileSizeLabel.take(40))
        put("contentText", contentText.take(30000))
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class BookmarkRecord(
    @DocumentId val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val classGrade: Long = 7L,
    val materialType: String = MaterialType.SHORT_NOTES.key,
    val subtitle: String = "",
    val accentKey: String = "blue",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttemptRecord(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val subjectName: String = "",
    val classGrade: Long = 7L,
    val mode: String = "practice",
    val totalQuestions: Long = 0L,
    val correctAnswers: Long = 0L,
    val scorePercent: Long = 0L,
    val durationSeconds: Long = 0L,
    val pointsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReadingProgressRecord(
    @DocumentId val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val chapterTitle: String = "",
    val classGrade: Long = 7L,
    val progressPercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DownloadRecord(
    @DocumentId val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val classGrade: Long = 7L,
    val fileSizeLabel: String = "1.5 MB",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AnnouncementItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetClass: Long = 0L,
    val priority: String = "normal",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("title", title.take(140))
        put("message", message.take(1000))
        put("targetClass", targetClass.coerceIn(0L, 12L))
        put("priority", if (priority == "high") "high" else "normal")
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "global",
    val premiumMonthlyPrice: String = "$4.99/mo",
    val premiumYearlyPrice: String = "$39.99/yr",
    val adsEnabled: Boolean = true,
    val adFamilySafeMode: Boolean = true,
    val adPlacementHome: Boolean = true,
    val adPlacementMaterials: Boolean = true,
    val adPlacementQuizzes: Boolean = false,
    val adPlacementPapers: Boolean = true,
    val adBannerTitle: String = "StudyHub Plus • Family-Safe Learning",
    val adBannerSubtitle: String = "Unlock unlimited offline packs, advanced analytics & ad-free mode.",
    val adCtaLabel: String = "Upgrade",
    val adNetworkProvider: String = "StudyHub Direct (Family-Safe)",
    val maintenanceMode: Boolean = false,
    val supportEmail: String = "support@studyhub.edu",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("premiumMonthlyPrice", premiumMonthlyPrice.trim().take(30).ifBlank { "$4.99/mo" })
        put("premiumYearlyPrice", premiumYearlyPrice.trim().take(30).ifBlank { "$39.99/yr" })
        put("adsEnabled", adsEnabled)
        put("adFamilySafeMode", adFamilySafeMode)
        put("adPlacementHome", adPlacementHome)
        put("adPlacementMaterials", adPlacementMaterials)
        put("adPlacementQuizzes", adPlacementQuizzes)
        put("adPlacementPapers", adPlacementPapers)
        put("adBannerTitle", adBannerTitle.trim().take(120).ifBlank { "StudyHub Plus • Family-Safe Learning" })
        put("adBannerSubtitle", adBannerSubtitle.trim().take(240).ifBlank { "Unlock unlimited offline packs, advanced analytics & ad-free mode." })
        put("adCtaLabel", adCtaLabel.trim().take(30).ifBlank { "Upgrade" })
        put("adNetworkProvider", adNetworkProvider.trim().take(60).ifBlank { "StudyHub Direct (Family-Safe)" })
        put("maintenanceMode", maintenanceMode)
        put("supportEmail", supportEmail.trim().take(120).ifBlank { "support@studyhub.edu" })
        put("published", published)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}
