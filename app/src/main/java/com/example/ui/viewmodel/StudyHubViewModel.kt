package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.BookmarkRecord
import com.example.data.model.ChapterItem
import com.example.data.model.DownloadRecord
import com.example.data.model.ExamPaper
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.QuestionType
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SyncStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserStatsCalculator
import com.example.data.remote.OfflineSyncManager
import com.example.data.remote.SeedCurriculumData
import com.example.data.remote.StudyHubRepository
import com.example.data.remote.SupabaseApiException
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.remote.SupabaseTestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
            displayName = currentUserName.ifBlank { "StudyHub Student" },
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

    private val _isLoadingAdminUsers = MutableStateFlow(false)
    val isLoadingAdminUsers: StateFlow<Boolean> = _isLoadingAdminUsers.asStateFlow()

    private val _adminUsersError = MutableStateFlow<String?>(null)
    val adminUsersError: StateFlow<String?> = _adminUsersError.asStateFlow()

    private val _activeQuiz = MutableStateFlow<ActiveQuizState?>(null)
    val activeQuiz: StateFlow<ActiveQuizState?> = _activeQuiz.asStateFlow()

    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy.asStateFlow()

    private val _supabaseStatus = MutableStateFlow(repository.getSupabaseConnectionStatus())
    val supabaseStatus: StateFlow<SupabaseConnectionStatus> = _supabaseStatus.asStateFlow()

    private val _subcollectionsSynced = MutableStateFlow(!repository.isSupabaseConfigured())
    private val _initialCurriculumSynced = MutableStateFlow(!repository.isSupabaseConfigured())
    private var profileSyncedToCloud: Boolean = false
    private var lastCloudSyncMillis: Long = 0L

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val readingProgress: StateFlow<List<ReadingProgressRecord>> = repository.observeLocalReadingProgress(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloads: StateFlow<List<DownloadRecord>> = repository.observeLocalDownloads(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.observePendingSyncCount(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val announcements: StateFlow<List<AnnouncementItem>> = repository.observeCloudAnnouncements(
        isAdmin = repository.isVerifiedAdminEmail(currentUserEmail)
    )
        .catch { error ->
            if (error is CancellationException) throw error
            emit(SeedCurriculumData.defaultAnnouncements)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedCurriculumData.defaultAnnouncements)

    val appSettings: StateFlow<AppSettings> = repository.observeCloudAppSettings()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(SeedCurriculumData.defaultAppSettings)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedCurriculumData.defaultAppSettings)

    init {
        viewModelScope.launch {
            repository.ensureLocalSeedData()
            syncManager.attachUser(currentUserId)
            syncManager.flushPendingSync(currentUserId)
            val ensured = repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
            ensured.onSuccess { p ->
                if (repository.isSupabaseConfigured()) {
                    profileSyncedToCloud = true
                }
                _profile.value = mergeComputedStatsIfReady(p)
                if (p.role == "admin" || repository.isVerifiedAdminEmail(currentUserEmail)) {
                    if (!hasAutoRoutedAdmin) {
                        hasAutoRoutedAdmin = true
                        _isAdminMode.value = true
                    }
                }
            }.onFailure { err ->
                if (isAuthorizedAdmin) {
                    _adminUsersError.value = formatAdminUsersError(err)
                }
            }
            if (isAuthorizedAdmin) {
                repository.seedCurriculumToCloudIfEmpty()
                loadAdminUsers(showFeedback = false)
            }
            repository.syncCurriculumFromCloud(_isAdminMode.value && isAuthorizedAdmin)
            lastCloudSyncMillis = System.currentTimeMillis()
            _initialCurriculumSynced.value = true
            val subSync = repository.syncUserSubcollectionsFromCloud(currentUserId)
            if (subSync.isSuccess || !repository.isSupabaseConfigured()) {
                _subcollectionsSynced.value = true
            }
        }
        viewModelScope.launch {
            combine(classes, _isAdminMode, _initialCurriculumSynced) { classList, adminMode, synced ->
                Triple(classList, adminMode, synced)
            }.collect { (classList, adminMode, synced) ->
                if (!synced || adminMode) return@collect
                val publishedGrades = classList.filter { it.published }.map { it.grade }.distinct().sorted()
                if (publishedGrades.isNotEmpty() && _profile.value.selectedClass !in publishedGrades) {
                    val current = _profile.value.selectedClass
                    val fallback = publishedGrades.minBy { kotlin.math.abs(it - current) }
                    _profile.update { it.copy(selectedClass = fallback) }
                    repository.updateUserProfileFields(currentUserId, mapOf("selectedClass" to fallback))
                }
            }
        }
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        val publishedGrades = classes.value.filter { it.published }.map { it.grade }.distinct().sorted()
                        val resolvedProfile = if (!_isAdminMode.value &&
                            _initialCurriculumSynced.value &&
                            publishedGrades.isNotEmpty() &&
                            remoteProfile.selectedClass !in publishedGrades
                        ) {
                            remoteProfile.copy(
                                selectedClass = publishedGrades.minBy { kotlin.math.abs(it - remoteProfile.selectedClass) }
                            )
                        } else {
                            remoteProfile
                        }
                        _profile.value = mergeComputedStatsIfReady(resolvedProfile)
                        if (!hasAutoRoutedAdmin && remoteProfile.role == "admin") {
                            hasAutoRoutedAdmin = true
                            _isAdminMode.value = true
                            loadAdminUsers()
                        }
                    }
                }
        }
        viewModelScope.launch {
            combine(quizAttempts, readingProgress, _subcollectionsSynced) { attempts, progress, synced ->
                Triple(attempts, progress, synced)
            }.collect { (attempts, progress, synced) ->
                val stats = UserStatsCalculator.compute(attempts, progress)
                val hasLocalActivity = attempts.isNotEmpty() || progress.isNotEmpty()
                if (synced || hasLocalActivity) {
                    val current = _profile.value
                    val changed = current.avgScore != stats.avgScore ||
                        current.dayStreak != stats.dayStreak ||
                        current.points != stats.points ||
                        current.dailyCompletedCount != stats.dailyCompletedCount
                    if (changed) {
                        _profile.update {
                            it.copy(
                                avgScore = stats.avgScore,
                                dayStreak = stats.dayStreak,
                                points = stats.points,
                                dailyCompletedCount = stats.dailyCompletedCount
                            )
                        }
                    }
                    if (synced && changed) {
                        repository.updateUserProfileFields(
                            currentUserId,
                            mapOf(
                                "avgScore" to stats.avgScore,
                                "dayStreak" to stats.dayStreak,
                                "points" to stats.points,
                                "dailyCompletedCount" to stats.dailyCompletedCount
                            )
                        )
                    }
                }
            }
        }
    }

    private fun mergeComputedStatsIfReady(base: UserProfile): UserProfile {
        val attempts = quizAttempts.value
        val progress = readingProgress.value
        if (!_subcollectionsSynced.value && attempts.isEmpty() && progress.isEmpty()) {
            return base
        }
        val stats = UserStatsCalculator.compute(attempts, progress)
        return base.copy(
            avgScore = stats.avgScore,
            dayStreak = stats.dayStreak,
            points = stats.points,
            dailyCompletedCount = stats.dailyCompletedCount
        )
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
        viewModelScope.launch {
            if (repository.isSupabaseConfigured() && !profileSyncedToCloud) {
                repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
                    .onSuccess { profileSyncedToCloud = true }
            }
            repository.syncCurriculumFromCloud(enabled && isAuthorizedAdmin)
            lastCloudSyncMillis = System.currentTimeMillis()
            _initialCurriculumSynced.value = true
            if (enabled) {
                loadAdminUsers(showFeedback = false)
            }
        }
    }

    fun refreshOnResume(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCloudSyncMillis < 5_000L) return
        lastCloudSyncMillis = now
        viewModelScope.launch {
            if (repository.isSupabaseConfigured() && !profileSyncedToCloud) {
                repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
                    .onSuccess { p ->
                        profileSyncedToCloud = true
                        _profile.value = mergeComputedStatsIfReady(p)
                    }
            }
            syncManager.flushPendingSync(currentUserId)
            repository.syncCurriculumFromCloud(_isAdminMode.value && isAuthorizedAdmin)
            _initialCurriculumSynced.value = true
            if (_isAdminMode.value && isAuthorizedAdmin) {
                loadAdminUsers(showFeedback = false)
            }
        }
    }

    fun updateSelectedClass(grade: Long) {
        val clamped = grade.coerceIn(6L, 12L)
        val publishedGrades = classes.value.filter { it.published }.map { it.grade }.distinct().sorted()
        val resolved = if (!_isAdminMode.value && publishedGrades.isNotEmpty() && clamped !in publishedGrades) {
            publishedGrades.minBy { kotlin.math.abs(it - clamped) }
        } else {
            clamped
        }
        _profile.update { it.copy(selectedClass = resolved) }
        viewModelScope.launch {
            repository.updateUserProfileFields(currentUserId, mapOf("selectedClass" to resolved))
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
            if (repository.isSupabaseConfigured() && !profileSyncedToCloud) {
                repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
                    .onSuccess { profileSyncedToCloud = true }
            }
            val res = syncManager.flushPendingSync(currentUserId)
            repository.syncCurriculumFromCloud(_isAdminMode.value && isAuthorizedAdmin)
            lastCloudSyncMillis = System.currentTimeMillis()
            _initialCurriculumSynced.value = true
            val subSync = repository.syncUserSubcollectionsFromCloud(currentUserId)
            if (subSync.isSuccess || !repository.isSupabaseConfigured()) {
                _subcollectionsSynced.value = true
            }
            if (_isAdminMode.value && isAuthorizedAdmin) loadAdminUsers(showFeedback = false)
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
    fun loadAdminUsers(showFeedback: Boolean = false) {
        if (!isAuthorizedAdmin) return
        if (!repository.isSupabaseConfigured()) {
            _adminUsers.value = emptyList()
            val msg = "Supabase is not configured. Open Manage Database → Setup Supabase Database."
            _adminUsersError.value = msg
            if (showFeedback) _snackbarMessage.value = msg
            return
        }
        viewModelScope.launch {
            _isLoadingAdminUsers.value = true
            _adminUsersError.value = null
            var ensureErr: String? = null
            try {
                if (!profileSyncedToCloud) {
                    repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
                        .onSuccess {
                            profileSyncedToCloud = true
                        }
                        .onFailure { err ->
                            ensureErr = formatAdminUsersError(err)
                        }
                }
                repository.fetchAllUsersForAdmin()
                    .onSuccess { users ->
                        _adminUsers.value = users
                        val diagnostic = when {
                            users.isNotEmpty() -> null
                            ensureErr != null -> ensureErr
                            profileSyncedToCloud ->
                                "Supabase returned 0 rows from user_profiles. Disable Row Level Security (RLS) or add SELECT grants in Manage Database → Setup Supabase."
                            else -> null
                        }
                        _adminUsersError.value = diagnostic
                        if (showFeedback) {
                            _snackbarMessage.value = when {
                                users.isNotEmpty() -> "Synced ${users.size} user(s)"
                                diagnostic != null -> diagnostic
                                else -> "No registered users found in Supabase yet"
                            }
                        }
                    }
                    .onFailure { err ->
                        val msg = formatAdminUsersError(err)
                        _adminUsersError.value = msg
                        if (showFeedback) _snackbarMessage.value = msg
                    }
            } finally {
                _isLoadingAdminUsers.value = false
            }
        }
    }

    private fun formatAdminUsersError(err: Throwable): String {
        if (err is SupabaseApiException) {
            return when (err.statusCode) {
                401, 403 -> "Supabase denied access to user_profiles (HTTP ${err.statusCode}). Run the SQL grants & disable-RLS script in Manage Database → Setup Supabase."
                404 -> "Table user_profiles is missing in Supabase (404). Run the SQL schema in Manage Database → Setup Supabase."
                400 -> "user_profiles schema mismatch (400). Re-run the latest SQL schema in Manage Database → Setup Supabase."
                else -> err.message ?: "Supabase error (${err.statusCode}) while syncing students."
            }
        }
        return err.message ?: "Unable to sync students. Check your connection."
    }

    fun saveSchoolClass(item: SchoolClass, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveSchoolClass(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Class saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save class" }
        }
    }

    fun toggleSchoolClassPublish(item: SchoolClass) {
        saveSchoolClass(item.copy(published = !item.published), isCreate = false)
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
                .onSuccess { _snackbarMessage.value = if (isCreate) "Subject saved" else "Subject updated" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save subject" }
        }
    }

    fun toggleSubjectPublish(item: SubjectItem) {
        saveSubject(item.copy(published = !item.published), isCreate = false)
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
                .onSuccess { _snackbarMessage.value = "Subject deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun saveChapter(item: ChapterItem, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveChapter(item, isCreate)
                .onSuccess { _snackbarMessage.value = "Chapter saved" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save chapter" }
        }
    }

    fun saveChapterWithAttachments(
        item: ChapterItem,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveChapterWithAttachments(item, isCreate, drafts)
                .onSuccess {
                    _snackbarMessage.value = if (isCreate) "Chapter saved" else "Chapter updated"
                    onResult(true)
                }
                .onFailure {
                    _snackbarMessage.value = it.message ?: "Failed to save chapter"
                    onResult(false)
                }
        }
    }

    fun toggleChapterPublish(item: ChapterItem) {
        saveChapter(item.copy(published = !item.published), isCreate = false)
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
                .onSuccess { _snackbarMessage.value = "Chapter deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun saveStudyMaterial(item: StudyMaterial, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveStudyMaterial(item, isCreate)
                .onSuccess { _snackbarMessage.value = if (item.published) "Material published" else "Saved as draft" }
                .onFailure { _snackbarMessage.value = it.message ?: "Upload failed" }
        }
    }

    fun saveStudyMaterialWithAttachments(
        item: StudyMaterial,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveStudyMaterialWithAttachments(item, isCreate, drafts)
                .onSuccess {
                    _snackbarMessage.value = if (item.published) "Material published" else "Saved as draft"
                    onResult(true)
                }
                .onFailure {
                    _snackbarMessage.value = it.message ?: "Failed to save material"
                    onResult(false)
                }
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
        val isCreate = quiz.id.isBlank()
        viewModelScope.launch {
            repository.saveQuiz(quiz, isCreate = isCreate)
                .onSuccess { quizId ->
                    if (question != null) {
                        repository.addQuizQuestion(question.copy(quizId = quizId))
                            .onSuccess { _snackbarMessage.value = "Quiz & question saved" }
                            .onFailure { _snackbarMessage.value = it.message ?: "Quiz saved, but failed to add question" }
                    } else {
                        _snackbarMessage.value = if (isCreate) "Quiz saved" else "Quiz updated"
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save quiz" }
        }
    }

    fun toggleQuizPublish(quiz: QuizItem) {
        saveQuizWithQuestion(quiz.copy(published = !quiz.published), null)
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            repository.deleteQuiz(quizId)
                .onSuccess { _snackbarMessage.value = "Quiz deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun addQuestionToQuiz(question: QuizQuestion) {
        val isEdit = question.id.isNotBlank()
        viewModelScope.launch {
            repository.addQuizQuestion(question)
                .onSuccess { _snackbarMessage.value = if (isEdit) "Question updated" else "MCQ added to quiz" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save question" }
        }
    }

    fun deleteQuizQuestion(questionId: String, quizId: String) {
        viewModelScope.launch {
            repository.deleteQuizQuestion(questionId, quizId)
                .onSuccess { _snackbarMessage.value = "Question deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun saveExamPaper(item: ExamPaper, isCreate: Boolean) {
        viewModelScope.launch {
            repository.saveExamPaper(item, isCreate)
                .onSuccess { _snackbarMessage.value = if (item.published) "Exam paper published" else "Exam paper saved as draft" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save exam paper" }
        }
    }

    fun saveExamPaperWithAttachments(
        item: ExamPaper,
        isCreate: Boolean,
        drafts: List<PaperAttachmentDraft>,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveExamPaperWithAttachments(item, isCreate, drafts)
                .onSuccess {
                    _snackbarMessage.value = if (isCreate) "Question paper published" else "Question paper updated"
                    onResult(true)
                }
                .onFailure {
                    _snackbarMessage.value = it.message ?: "Failed to save question paper"
                    onResult(false)
                }
        }
    }

    fun toggleExamPaperPublish(paper: ExamPaper) {
        saveExamPaper(paper.copy(published = !paper.published), isCreate = false)
    }

    fun deleteExamPaper(paperId: String) {
        viewModelScope.launch {
            repository.deleteExamPaper(paperId)
                .onSuccess { _snackbarMessage.value = "Question paper deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    suspend fun loadAttachmentsForParent(parentId: String): Result<List<ResolvedPaperAttachment>> =
        repository.loadAttachmentsForParent(parentId, isAdmin = isAuthorizedAdmin)

    fun sendAnnouncement(title: String, message: String, targetClass: Long, priority: String) {
        saveAnnouncement(
            AnnouncementItem(
                title = title,
                message = message,
                targetClass = targetClass,
                priority = priority,
                published = true
            )
        )
    }

    fun saveAnnouncement(item: AnnouncementItem) {
        val isCreate = item.id.isBlank()
        viewModelScope.launch {
            repository.sendAnnouncement(item)
                .onSuccess { _snackbarMessage.value = if (isCreate) "Announcement broadcast!" else "Announcement updated" }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to save announcement" }
        }
    }

    fun toggleAnnouncementPublish(item: AnnouncementItem) {
        saveAnnouncement(item.copy(published = !item.published))
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
                .onSuccess { _snackbarMessage.value = "Announcement deleted" }
                .onFailure { _snackbarMessage.value = it.message ?: "Delete failed" }
        }
    }

    fun adminSetStudentBlocked(user: UserProfile, blocked: Boolean) {
        val targetUid = user.id.ifBlank { user.userId }
        viewModelScope.launch {
            repository.adminSetUserBlocked(targetUid, blocked)
                .onSuccess {
                    _adminUsers.update { list ->
                        list.map { existing ->
                            if ((existing.id.ifBlank { existing.userId }) == targetUid) {
                                existing.copy(blocked = blocked)
                            } else existing
                        }
                    }
                    _snackbarMessage.value = if (blocked) {
                        "${user.displayName} blocked"
                    } else {
                        "${user.displayName} unblocked"
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to update student status" }
        }
    }

    fun adminUpdateStudent(
        user: UserProfile,
        displayName: String,
        selectedClass: Long,
        subscriptionTier: String,
        blocked: Boolean
    ) {
        val targetUid = user.id.ifBlank { user.userId }
        val cleanName = displayName.trim().take(80).ifBlank { user.displayName }
        val cleanTier = if (subscriptionTier == "premium") "premium" else "free"
        val cleanClass = selectedClass.coerceIn(6L, 12L)
        viewModelScope.launch {
            repository.adminUpdateStudent(targetUid, cleanName, cleanClass, cleanTier, blocked)
                .onSuccess {
                    _adminUsers.update { list ->
                        list.map { existing ->
                            if ((existing.id.ifBlank { existing.userId }) == targetUid) {
                                existing.copy(
                                    displayName = cleanName,
                                    selectedClass = cleanClass,
                                    subscriptionTier = cleanTier,
                                    blocked = blocked
                                )
                            } else existing
                        }
                    }
                    _snackbarMessage.value = "Student updated"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to update student" }
        }
    }

    fun adminDeleteStudent(user: UserProfile) {
        val targetUid = user.id.ifBlank { user.userId }
        viewModelScope.launch {
            repository.adminDeleteStudent(targetUid)
                .onSuccess {
                    _adminUsers.update { list ->
                        list.filterNot { (it.id.ifBlank { it.userId }) == targetUid }
                    }
                    _snackbarMessage.value = "Student ${user.displayName} deleted"
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to delete student" }
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

    suspend fun testSupabaseConnection(url: String, anonKey: String): SupabaseTestResult =
        repository.testSupabaseConnection(url, anonKey)

    suspend fun saveManualSupabaseConfig(url: String, anonKey: String): Result<SupabaseTestResult> {
        val result = repository.saveManualSupabaseConfig(url, anonKey)
        _supabaseStatus.value = repository.getSupabaseConnectionStatus()
        result.onSuccess { test ->
            if (test.connected && test.schemaReady) {
                profileSyncedToCloud = false
                repository.ensureUserProfile(currentUserId, currentUserName, currentUserEmail)
                    .onSuccess { profileSyncedToCloud = true }
                syncManager.flushPendingSync(currentUserId)
                repository.syncCurriculumFromCloud(isAuthorizedAdmin)
                val subSync = repository.syncUserSubcollectionsFromCloud(currentUserId)
                if (subSync.isSuccess) _subcollectionsSynced.value = true
                loadAdminUsers(showFeedback = false)
                _snackbarMessage.value = "Supabase connected and synced!"
            } else {
                _snackbarMessage.value = "Supabase credentials saved"
            }
        }.onFailure { err ->
            _snackbarMessage.value = err.message ?: "Failed to save Supabase configuration"
        }
        return result
    }

    fun clearManualSupabaseConfig() {
        runCatching { repository.clearManualSupabaseConfig() }
            .onSuccess { status ->
                _supabaseStatus.value = status
                profileSyncedToCloud = false
                if (!status.configured) {
                    _adminUsers.value = emptyList()
                    _adminUsersError.value = "Supabase is not configured. Open Manage Database → Setup Supabase Database."
                } else {
                    loadAdminUsers(showFeedback = false)
                }
                _snackbarMessage.value = "Manual Supabase configuration cleared"
            }
            .onFailure { err ->
                _snackbarMessage.value = err.message ?: "Unable to clear Supabase configuration"
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
