package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.BookmarkRecord
import com.example.data.model.ChapterItem
import com.example.data.model.DownloadRecord
import com.example.data.model.ExamPaper
import com.example.data.model.QuestionType
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SyncStatus
import com.example.data.model.UserProfile
import com.example.data.remote.OfflineSyncManager
import com.example.data.remote.SeedCurriculumData
import com.example.data.remote.StudyHubRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuizMode(val key: String, val label: String) {
    PRACTICE("practice", "Practice Mode"),
    EXAM("exam", "Exam Mode")
}

data class ActiveQuizState(
    val quiz: QuizItem,
    val questions: List<QuizQuestion>,
    val mode: QuizMode,
    val currentIndex: Int = 0,
    val selectedSingleIndices: Map<String, Int> = emptyMap(),
    val selectedMultiIndices: Map<String, Set<Int>> = emptyMap(),
    val fillBlankAnswers: Map<String, String> = emptyMap(),
    val confirmedPracticeQuestionIds: Set<String> = emptySet(),
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val isFinished: Boolean = false,
    val correctCount: Int = 0,
    val pointsEarned: Int = 0
)

class StudyHubViewModel(
    private val repository: StudyHubRepository,
    private val syncManager: OfflineSyncManager,
    val currentUserId: String,
    private val currentUserName: String,
    private val currentUserEmail: String
) : ViewModel() {

    private val _profile = MutableStateFlow(
        UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = currentUserName.ifBlank { "Momrooj Ali" },
            email = currentUserEmail,
            selectedClass = 7L,
            role = if (repository.isVerifiedAdminEmail(currentUserEmail)) "admin" else "student"
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val initialAdminVerified = repository.isVerifiedAdminEmail(currentUserEmail)
    private var hasAutoRoutedAdmin = initialAdminVerified

    private val _isAdminMode = MutableStateFlow(initialAdminVerified)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    val isAuthorizedAdmin: Boolean
        get() = _profile.value.role == "admin" || repository.isVerifiedAdminEmail(currentUserEmail)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _activeQuiz = MutableStateFlow<ActiveQuizState?>(null)
    val activeQuiz: StateFlow<ActiveQuizState?> = _activeQuiz.asStateFlow()

    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy.asStateFlow()

    private var timerJob: Job? = null

    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    val classes: StateFlow<List<SchoolClass>> = repository.observeLocalClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val subjects: StateFlow<List<SubjectItem>> = repository.observeLocalSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chapters: StateFlow<List<ChapterItem>> = repository.observeLocalChapters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val materials: StateFlow<List<StudyMaterial>> = repository.observeLocalMaterials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloadedMaterialIds: StateFlow<Set<String>> = repository.observeDownloadedMaterialIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val quizzes: StateFlow<List<QuizItem>> = repository.observeLocalQuizzes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allQuestions: StateFlow<List<QuizQuestion>> = repository.observeAllLocalQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val examPapers: StateFlow<List<ExamPaper>> = repository.observeLocalExamPapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloadedPaperIds: StateFlow<Set<String>> = repository.observeDownloadedPaperIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val bookmarks: StateFlow<List<BookmarkRecord>> = repository.observeLocalBookmarks(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quizAttempts: StateFlow<List<QuizAttemptRecord>> = repository.observeLocalQuizAttempts(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val readingProgress: StateFlow<List<ReadingProgressRecord>> = repository.observeLocalReadingProgress(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloads: StateFlow<List<DownloadRecord>> = repository.observeLocalDownloads(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.observePendingSyncCount(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val announcements: StateFlow<List<AnnouncementItem>> = repository.observeCloudAnnouncements(
        isAdmin = repository.isVerifiedAdminEmail(currentUserEmail)
    )
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(SeedCurriculumData.defaultAnnouncements)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedCurriculumData.defaultAnnouncements)

    val appSettings: StateFlow<AppSettings> = repository.observeCloudAppSettings()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(SeedCurriculumData.defaultAppSettings)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedCurriculumData.defaultAppSettings)

    init {
        viewModelScope.launch {
            repository.ensureLocalSeedData()
            syncManager.attachUser(currentUserId)
            val ensured = repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
            ensured.onSuccess { p ->
                _profile.value = p
                if (p.role == "admin" || repository.isVerifiedAdminEmail(currentUserEmail)) {
                    if (!hasAutoRoutedAdmin) {
                        hasAutoRoutedAdmin = true
                        _isAdminMode.value = true
                    }
                    repository.seedCurriculumToCloudIfEmpty()
                    loadAdminUsers()
                }
            }
            repository.syncCurriculumFromCloud(isAuthorizedAdmin)
            repository.syncUserSubcollectionsFromCloud(currentUserId)
        }
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        _profile.value = remoteProfile
                        if (!hasAutoRoutedAdmin && remoteProfile.role == "admin") {
                            hasAutoRoutedAdmin = true
                            _isAdminMode.value = true
                            loadAdminUsers()
                        }
                    }
                }
        }
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setAdminMode(enabled: Boolean) {
        if (enabled && !isAuthorizedAdmin) {
            _snackbarMessage.value = "Admin privileges are verified by backend role assignment."
            return
        }
        _isAdminMode.value = enabled
        if (enabled) {
            loadAdminUsers()
        }
    }

    fun updateSelectedClass(grade: Long) {
        val clamped = grade.coerceIn(6L, 12L)
        _profile.update { it.copy(selectedClass = clamped) }
        viewModelScope.launch {
            repository.updateUserProfileFields(currentUserId, mapOf("selectedClass" to clamped))
        }
    }

    fun updateDisplayName(name: String) {
        val clean = name.trim().take(80)
        if (clean.isBlank()) return
        _profile.update { it.copy(displayName = clean) }
        viewModelScope.launch {
            repository.updateUserProfileFields(currentUserId, mapOf("displayName" to clean))
                .onSuccess { _snackbarMessage.value = "Profile updated" }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _profile.update { it.copy(darkThemeEnabled = enabled) }
        viewModelScope.launch {
            repository.updateUserProfileFields(currentUserId, mapOf("darkThemeEnabled" to enabled))
        }
    }

    fun updateNotificationPrefs(
        notificationsEnabled: Boolean,
        examRemindersEnabled: Boolean,
        newMaterialsEnabled: Boolean
    ) {
        _profile.update {
            it.copy(
                notificationsEnabled = notificationsEnabled,
                examRemindersEnabled = examRemindersEnabled,
                newMaterialsEnabled = newMaterialsEnabled
            )
        }
        viewModelScope.launch {
            repository.updateUserProfileFields(
                currentUserId,
                mapOf(
                    "notificationsEnabled" to notificationsEnabled,
                    "examRemindersEnabled" to examRemindersEnabled,
                    "newMaterialsEnabled" to newMaterialsEnabled
                )
            )
        }
    }

    fun toggleBookmark(material: StudyMaterial) {
        val currentlyBookmarked = bookmarks.value.any { it.materialId == material.id }
        viewModelScope.launch {
            repository.toggleBookmarkOfflineFirst(currentUserId, material, currentlyBookmarked)
            _snackbarMessage.value = if (currentlyBookmarked) "Removed from bookmarks" else "Saved to bookmarks"
        }
    }

    fun toggleDownload(material: StudyMaterial) {
        val currentlyDownloaded = downloadedMaterialIds.value.contains(material.id)
        viewModelScope.launch {
            repository.toggleDownloadOfflineFirst(currentUserId, material, currentlyDownloaded)
            _snackbarMessage.value = if (currentlyDownloaded) "Removed offline copy" else "Downloaded for offline study"
        }
    }

    fun toggleExamPaperDownload(paper: ExamPaper) {
        val current = downloadedPaperIds.value.contains(paper.id)
        viewModelScope.launch {
            repository.toggleExamPaperDownload(paper, current)
            _snackbarMessage.value = if (current) "Removed offline PDF" else "Exam paper saved offline"
        }
    }

    fun markMaterialRead(material: StudyMaterial, progressPercent: Long = 100L) {
        viewModelScope.launch {
            repository.recordReadingProgressOfflineFirst(currentUserId, material, progressPercent)
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val res = syncManager.flushPendingSync(currentUserId)
            repository.syncCurriculumFromCloud(isAuthorizedAdmin)
            res.onSuccess { count ->
                _snackbarMessage.value = if (count > 0) "Synced $count offline record(s)" else "Everything is up to date"
            }.onFailure {
                _snackbarMessage.value = "Sync deferred — working offline"
            }
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUserAccountData(currentUserId)
                .onSuccess {
                    _snackbarMessage.value = "Account data deleted"
                    onComplete()
                }
                .onFailure {
                    _snackbarMessage.value = "Could not delete cloud data while offline"
                }
        }
    }

    // Quiz Engine
    fun startQuiz(quiz: QuizItem, mode: QuizMode, randomize: Boolean) {
        val rawQuestions = allQuestions.value.filter { it.quizId == quiz.id && it.published }
        val finalQuestions = if (randomize) rawQuestions.shuffled() else rawQuestions
        if (finalQuestions.isEmpty()) {
            _snackbarMessage.value = "No questions available in this quiz yet."
            return
        }
        timerJob?.cancel()
        val totalSecs = (quiz.durationMinutes.toInt().coerceAtLeast(1)) * 60
        _activeQuiz.value = ActiveQuizState(
            quiz = quiz,
            questions = finalQuestions,
            mode = mode,
            remainingSeconds = totalSecs
        )
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _activeQuiz.value ?: break
                if (state.isFinished) break
                val nextRem = (state.remainingSeconds - 1).coerceAtLeast(0)
                _activeQuiz.update {
                    it?.copy(remainingSeconds = nextRem, elapsedSeconds = it.elapsedSeconds + 1)
                }
                if (nextRem == 0 && state.mode == QuizMode.EXAM) {
                    submitQuiz()
                    break
                }
            }
        }
    }

    fun selectSingleOption(questionId: String, index: Int) {
        _activeQuiz.update { state ->
            state ?: return@update null
            if (state.isFinished) return@update state
            val updated = state.selectedSingleIndices + (questionId to index)
            val confirmed = if (state.mode == QuizMode.PRACTICE) {
                state.confirmedPracticeQuestionIds + questionId
            } else state.confirmedPracticeQuestionIds
            state.copy(selectedSingleIndices = updated, confirmedPracticeQuestionIds = confirmed)
        }
    }

    fun toggleMultiOption(questionId: String, index: Int) {
        _activeQuiz.update { state ->
            state ?: return@update null
            if (state.isFinished) return@update state
            val current = state.selectedMultiIndices[questionId] ?: emptySet()
            val next = if (index in current) current - index else current + index
            state.copy(selectedMultiIndices = state.selectedMultiIndices + (questionId to next))
        }
    }

    fun updateFillBlank(questionId: String, answer: String) {
        _activeQuiz.update { state ->
            state ?: return@update null
            if (state.isFinished) return@update state
            state.copy(fillBlankAnswers = state.fillBlankAnswers + (questionId to answer))
        }
    }

    fun confirmPracticeAnswer(questionId: String) {
        _activeQuiz.update { state ->
            state?.copy(confirmedPracticeQuestionIds = state.confirmedPracticeQuestionIds + questionId)
        }
    }

    fun nextQuestion() {
        _activeQuiz.update { state ->
            state ?: return@update null
            if (state.currentIndex < state.questions.lastIndex) {
                state.copy(currentIndex = state.currentIndex + 1)
            } else state
        }
    }

    fun previousQuestion() {
        _activeQuiz.update { state ->
            state ?: return@update null
            if (state.currentIndex > 0) {
                state.copy(currentIndex = state.currentIndex - 1)
            } else state
        }
    }

    fun submitQuiz() {
        val state = _activeQuiz.value ?: return
        if (state.isFinished) return
        timerJob?.cancel()

        var correct = 0
        var points = 0
        for (q in state.questions) {
            val isCorrect = isQuestionCorrect(state, q)
            if (isCorrect) {
                correct++
                points += q.points.toInt()
            }
        }
        _activeQuiz.value = state.copy(
            isFinished = true,
            correctCount = correct,
            pointsEarned = points
        )

        viewModelScope.launch {
            repository.saveQuizAttemptOfflineFirst(
                uid = currentUserId,
                quiz = state.quiz,
                mode = state.mode.key,
                totalQuestions = state.questions.size,
                correctAnswers = correct,
                durationSeconds = state.elapsedSeconds,
                pointsEarned = points
            )
            val newPoints = _profile.value.points + points
            val newCompleted = (_profile.value.dailyCompletedCount + 1).coerceAtMost(50L)
            val scorePct = if (state.questions.isNotEmpty()) ((correct * 100) / state.questions.size).toLong() else 0L
            val attempts = quizAttempts.value
            val avg = if (attempts.isEmpty()) scorePct else ((attempts.sumOf { it.scorePercent } + scorePct) / (attempts.size + 1))
            _profile.update { it.copy(points = newPoints, dailyCompletedCount = newCompleted, avgScore = avg) }
            repository.updateUserProfileFields(
                currentUserId,
                mapOf("points" to newPoints, "dailyCompletedCount" to newCompleted, "avgScore" to avg)
            )
        }
    }

    fun closeQuiz() {
        timerJob?.cancel()
        _activeQuiz.value = null
    }

    fun isQuestionCorrect(state: ActiveQuizState, q: QuizQuestion): Boolean {
        return when (QuestionType.fromKey(q.questionType)) {
            QuestionType.MCQ, QuestionType.TRUE_FALSE -> {
                val chosen = state.selectedSingleIndices[q.id] ?: return false
                q.correctIndices.contains(chosen.toLong())
            }
            QuestionType.MULTI_SELECT -> {
                val chosen = state.selectedMultiIndices[q.id]?.map { it.toLong() }?.toSet() ?: return false
                chosen.isNotEmpty() && chosen == q.correctIndices.toSet()
            }
            QuestionType.FILL_BLANK -> {
                val typed = state.fillBlankAnswers[q.id]?.trim() ?: return false
                typed.equals(q.correctText.trim(), ignoreCase = true)
            }
        }
    }

    // Admin Management Operations
    fun loadAdminUsers() {
        if (!isAuthorizedAdmin) return
        viewModelScope.launch {
            repository.fetchAllUsersForAdmin().onSuccess { _adminUsers.value = it }
        }
    }

    fun saveSchoolClass(item: SchoolClass, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveSchoolClass(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Class saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save class" }
        }
    }

    fun deleteSchoolClass(id: String) {
        viewModelScope.launch {
            repository.deleteSchoolClass(id)
                .onSuccess { _snackbarMessage.value = "Class deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun saveSubject(item: SubjectItem, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveSubject(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Subject saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save subject" }
        }
    }

    fun saveChapter(item: ChapterItem, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveChapter(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Chapter saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save chapter" }
        }
    }

    fun saveStudyMaterial(item: StudyMaterial, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveStudyMaterial(item, isCreate)
                .onSuccess { _snackbarMessage.value = if (item.published) "Material published" else "Saved as draft" }
                .onFailure { _snackbarMessage.value = it.message ?: "Upload failed" }
        }
    }

    fun toggleMaterialPublish(item: StudyMaterial) {
        saveStudyMaterial(item.copy(published = !item.published), isCreate = false)
    }

    fun deleteStudyMaterial(id: String) {
        viewModelScope.launch {
            repository.deleteStudyMaterial(id)
                .onSuccess { _snackbarMessage.value = "Material deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun saveQuizWithQuestion(quiz: QuizItem, question: QuizQuestion?) {
        viewModelScope.launch {
            repository.saveQuiz(quiz, isCreate = quiz.id.isBlank())
                .onSuccess { quizId ->
                    if (question != null) {
                        repository.addQuizQuestion(question.copy(quizId = quizId))
                    }
                    _snackbarMessage.value = "Quiz saved"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save quiz" }
        }
    }

    fun addQuestionToQuiz(question: QuizQuestion) {
        viewModelScope.launch {
            repository.addQuizQuestion(question)
                .onSuccess { _snackbarMessage.value = "MCQ added to quiz" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to add question" }
        }
    }

    fun saveExamPaper(item: ExamPaper, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveExamPaper(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Exam paper saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save exam paper" }
        }
    }

    fun sendAnnouncement(title: String, message: String, targetClass: Long, priority: String) {
        viewModelScope.launch {
            repository.sendAnnouncement(
                AnnouncementItem(
                    title = title,
                    message = message,
                    targetClass = targetClass,
                    priority = priority,
                    published = true
                )
            )
                .onSuccess { _snackbarMessage.value = "Announcement broadcast!" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to send announcement" }
        }
    }

    fun saveAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveAppSettings(settings)
                .onSuccess { _snackbarMessage.value = "Settings updated" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to update settings" }
        }
    }

    fun syncCloudDatabaseNow() {
        if (_isDatabaseBusy.value) return
        viewModelScope.launch {
            _isDatabaseBusy.value = true
            try {
                syncManager.flushPendingSync(currentUserId)
                val res = repository.syncCurriculumFromCloud(isAuthorizedAdmin)
                loadAdminUsers()
                res.onSuccess {
                    _snackbarMessage.value = "Cloud database synced to local cache"
                }.onFailure {
                    _snackbarMessage.value = it.message ?: "Cloud sync failed"
                }
            } finally {
                _isDatabaseBusy.value = false
            }
        }
    }

    fun seedMissingCloudData() {
        if (_isDatabaseBusy.value) return
        viewModelScope.launch {
            _isDatabaseBusy.value = true
            try {
                repository.ensureLocalSeedData()
                val res = repository.seedOrRepairCloudCurriculum()
                repository.syncCurriculumFromCloud(isAuthorizedAdmin)
                res.onSuccess { count ->
                    _snackbarMessage.value = if (count > 0) {
                        "Seeded $count missing cloud record(s)"
                    } else {
                        "Cloud database already contains all default curriculum"
                    }
                }.onFailure {
                    _snackbarMessage.value = it.message ?: "Cloud seed failed"
                }
            } finally {
                _isDatabaseBusy.value = false
            }
        }
    }

    fun resetLocalCacheAndResync() {
        if (_isDatabaseBusy.value) return
        viewModelScope.launch {
            _isDatabaseBusy.value = true
            try {
                val res = repository.resetLocalCacheAndResync(isAuthorizedAdmin)
                res.onSuccess {
                    _snackbarMessage.value = "Local cache reset and re-synced from Cloud"
                }.onFailure {
                    _snackbarMessage.value = it.message ?: "Cache reset failed"
                }
            } finally {
                _isDatabaseBusy.value = false
            }
        }
    }

    fun flushPendingSyncQueue() {
        if (_isDatabaseBusy.value) return
        viewModelScope.launch {
            _isDatabaseBusy.value = true
            try {
                syncManager.flushPendingSync(currentUserId)
                    .onSuccess { count ->
                        _snackbarMessage.value = if (count > 0) "Flushed $count queued sync item(s)" else "Sync queue is already empty"
                    }
                    .onFailure {
                        _snackbarMessage.value = it.message ?: "Flush sync failed"
                    }
            } finally {
                _isDatabaseBusy.value = false
            }
        }
    }

    fun cleanupBeforeSignOut() {
        timerJob?.cancel()
        syncManager.detachUser()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
