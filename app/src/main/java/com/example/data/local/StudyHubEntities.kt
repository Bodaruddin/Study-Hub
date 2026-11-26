package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BookmarkRecord
import com.example.data.model.ChapterItem
import com.example.data.model.DownloadRecord
import com.example.data.model.ExamPaper
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem

@Entity(tableName = "cached_classes")
data class CachedClassEntity(
    @PrimaryKey val id: String,
    val grade: Long,
    val name: String,
    val description: String,
    val studentCount: Long,
    val published: Boolean
) {
    fun toDomain() = SchoolClass(id = id, grade = grade, name = name, description = description, studentCount = studentCount, published = published)
}

@Entity(tableName = "cached_subjects")
data class CachedSubjectEntity(
    @PrimaryKey val id: String,
    val classGrade: Long,
    val name: String,
    val subtitle: String,
    val iconKey: String,
    val colorKey: String,
    val chapterCount: Long,
    val published: Boolean
) {
    fun toDomain() = SubjectItem(id = id, classGrade = classGrade, name = name, subtitle = subtitle, iconKey = iconKey, colorKey = colorKey, chapterCount = chapterCount, published = published)
}

@Entity(tableName = "cached_chapters")
data class CachedChapterEntity(
    @PrimaryKey val id: String,
    val classGrade: Long,
    val subjectId: String,
    val subjectName: String,
    val chapterNumber: Long,
    val title: String,
    val summary: String,
    val published: Boolean
) {
    fun toDomain() = ChapterItem(id = id, classGrade = classGrade, subjectId = subjectId, subjectName = subjectName, chapterNumber = chapterNumber, title = title, summary = summary, published = published)
}

@Entity(tableName = "cached_materials")
data class CachedMaterialEntity(
    @PrimaryKey val id: String,
    val classGrade: Long,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterTitle: String,
    val title: String,
    val description: String,
    val materialType: String,
    val contentMarkdown: String,
    val fileSizeLabel: String,
    val tagsCsv: String,
    val important: Boolean,
    val premiumOnly: Boolean,
    val published: Boolean,
    val downloadedOffline: Boolean = false,
    val downloadedAtMillis: Long = 0L
) {
    fun toDomain() = StudyMaterial(
        id = id,
        classGrade = classGrade,
        subjectId = subjectId,
        subjectName = subjectName,
        chapterId = chapterId,
        chapterTitle = chapterTitle,
        title = title,
        description = description,
        materialType = materialType,
        contentMarkdown = contentMarkdown,
        fileSizeLabel = fileSizeLabel,
        tags = tagsCsv.split("|").filter { it.isNotBlank() },
        important = important,
        premiumOnly = premiumOnly,
        published = published
    )
}

@Entity(tableName = "cached_quizzes")
data class CachedQuizEntity(
    @PrimaryKey val id: String,
    val classGrade: Long,
    val subjectId: String,
    val subjectName: String,
    val chapterId: String,
    val chapterTitle: String,
    val title: String,
    val description: String,
    val durationMinutes: Long,
    val questionCount: Long,
    val premiumOnly: Boolean,
    val published: Boolean,
    val downloadedOffline: Boolean = true
) {
    fun toDomain() = QuizItem(
        id = id,
        classGrade = classGrade,
        subjectId = subjectId,
        subjectName = subjectName,
        chapterId = chapterId,
        chapterTitle = chapterTitle,
        title = title,
        description = description,
        durationMinutes = durationMinutes,
        questionCount = questionCount,
        premiumOnly = premiumOnly,
        published = published
    )
}

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val questionType: String,
    val prompt: String,
    val optionsJoined: String,
    val correctIndicesJoined: String,
    val correctText: String,
    val explanation: String,
    val points: Long,
    val published: Boolean
) {
    fun toDomain() = QuizQuestion(
        id = id,
        quizId = quizId,
        questionType = questionType,
        prompt = prompt,
        options = if (optionsJoined.isBlank()) emptyList() else optionsJoined.split("||"),
        correctIndices = if (correctIndicesJoined.isBlank()) emptyList() else correctIndicesJoined.split(",").mapNotNull { it.toLongOrNull() },
        correctText = correctText,
        explanation = explanation,
        points = points,
        published = published
    )
}

@Entity(tableName = "cached_exam_papers")
data class CachedExamPaperEntity(
    @PrimaryKey val id: String,
    val classGrade: Long,
    val subjectId: String,
    val subjectName: String,
    val title: String,
    val paperCategory: String,
    val year: Long,
    val fileSizeLabel: String,
    val contentText: String,
    val published: Boolean,
    val downloadedOffline: Boolean = false
) {
    fun toDomain() = ExamPaper(
        id = id,
        classGrade = classGrade,
        subjectId = subjectId,
        subjectName = subjectName,
        title = title,
        paperCategory = paperCategory,
        year = year,
        fileSizeLabel = fileSizeLabel,
        contentText = contentText,
        published = published
    )
}

@Entity(tableName = "cached_bookmarks")
data class CachedBookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val materialId: String,
    val title: String,
    val subjectName: String,
    val classGrade: Long,
    val materialType: String,
    val subtitle: String,
    val accentKey: String,
    val savedAtMillis: Long
) {
    fun toDomain() = BookmarkRecord(
        id = id,
        materialId = materialId,
        title = title,
        subjectName = subjectName,
        classGrade = classGrade,
        materialType = materialType,
        subtitle = subtitle,
        accentKey = accentKey
    )
}

@Entity(tableName = "cached_quiz_attempts")
data class CachedQuizAttemptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val quizId: String,
    val quizTitle: String,
    val subjectName: String,
    val classGrade: Long,
    val mode: String,
    val totalQuestions: Long,
    val correctAnswers: Long,
    val scorePercent: Long,
    val durationSeconds: Long,
    val pointsEarned: Long,
    val attemptedAtMillis: Long
) {
    fun toDomain() = QuizAttemptRecord(
        id = id,
        quizId = quizId,
        quizTitle = quizTitle,
        subjectName = subjectName,
        classGrade = classGrade,
        mode = mode,
        totalQuestions = totalQuestions,
        correctAnswers = correctAnswers,
        scorePercent = scorePercent,
        durationSeconds = durationSeconds,
        pointsEarned = pointsEarned
    )
}

@Entity(tableName = "cached_reading_progress")
data class CachedReadingProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val materialId: String,
    val title: String,
    val subjectName: String,
    val chapterTitle: String,
    val classGrade: Long,
    val progressPercent: Long,
    val updatedAtMillis: Long
) {
    fun toDomain() = ReadingProgressRecord(
        id = id,
        materialId = materialId,
        title = title,
        subjectName = subjectName,
        chapterTitle = chapterTitle,
        classGrade = classGrade,
        progressPercent = progressPercent
    )
}

@Entity(tableName = "cached_downloads")
data class CachedDownloadEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val materialId: String,
    val title: String,
    val subjectName: String,
    val classGrade: Long,
    val fileSizeLabel: String,
    val downloadedAtMillis: Long
) {
    fun toDomain() = DownloadRecord(
        id = id,
        materialId = materialId,
        title = title,
        subjectName = subjectName,
        classGrade = classGrade,
        fileSizeLabel = fileSizeLabel
    )
}

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey val syncKey: String,
    val userId: String,
    val actionType: String,
    val targetId: String,
    val payloadJson: String,
    val createdAtMillis: Long,
    val retryCount: Int = 0,
    val lastError: String = ""
)
