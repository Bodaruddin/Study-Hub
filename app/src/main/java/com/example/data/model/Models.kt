package com.example.data.model

import androidx.annotation.Keep

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
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val email: String = "",
    val selectedClass: Long = 7L,
    val role: String = "student",
    val subscriptionTier: String = "free",
    val avgScore: Long = 0L,
    val dayStreak: Long = 0L,
    val points: Long = 0L,
    val dailyGoalCount: Long = 5L,
    val dailyCompletedCount: Long = 0L,
    val darkThemeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val examRemindersEnabled: Boolean = true,
    val newMaterialsEnabled: Boolean = true,
    val blocked: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class SchoolClass(
    val id: String = "",
    val grade: Long = 7L,
    val name: String = "Class 7",
    val description: String = "",
    val studentCount: Long = 0L,
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class SubjectItem(
    val id: String = "",
    val classGrade: Long = 7L,
    val name: String = "",
    val subtitle: String = "",
    val iconKey: String = "book",
    val colorKey: String = "blue",
    val chapterCount: Long = 0L,
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class ChapterItem(
    val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterNumber: Long = 1L,
    val title: String = "",
    val summary: String = "",
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class StudyMaterial(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class QuizItem(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class QuizQuestion(
    val id: String = "",
    val quizId: String = "",
    val questionType: String = QuestionType.MCQ.key,
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndices: List<Long> = emptyList(),
    val correctText: String = "",
    val explanation: String = "",
    val points: Long = 10L,
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class ExamPaper(
    val id: String = "",
    val classGrade: Long = 7L,
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val paperCategory: String = PaperCategory.PREVIOUS_YEAR.key,
    val year: Long = 2025L,
    val fileSizeLabel: String = "2.4 MB",
    val contentText: String = "",
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class BookmarkRecord(
    val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val classGrade: Long = 7L,
    val materialType: String = MaterialType.SHORT_NOTES.key,
    val subtitle: String = "",
    val accentKey: String = "blue",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class QuizAttemptRecord(
    val id: String = "",
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
    val attemptedAtMillis: Long = 0L,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class ReadingProgressRecord(
    val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val chapterTitle: String = "",
    val classGrade: Long = 7L,
    val progressPercent: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class DownloadRecord(
    val id: String = "",
    val materialId: String = "",
    val title: String = "",
    val subjectName: String = "",
    val classGrade: Long = 7L,
    val fileSizeLabel: String = "1.5 MB",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class AnnouncementItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetClass: Long = 0L,
    val priority: String = "normal",
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class AppSettings(
    val id: String = "global",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class PaperAttachmentChunk(
    val id: String = "",
    val parentId: String = "",
    val groupId: String = "",
    val kind: String = "image",
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long = 0L,
    val chunkIndex: Long = 0L,
    val totalChunks: Long = 1L,
    val sortOrder: Long = 0L,
    val published: Boolean = true,
    val dataBase64: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class PaperAttachmentDraft(
    val groupId: String,
    val kind: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val previewFile: java.io.File,
    val isExistingRemote: Boolean = false
)

data class ResolvedPaperAttachment(
    val groupId: String,
    val parentId: String,
    val kind: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sortOrder: Long,
    val localFile: java.io.File
)
