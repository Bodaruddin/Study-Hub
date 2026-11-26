package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.StudyHubDatabase
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.remote.AuthManager
import com.example.data.remote.OfflineSyncManager
import com.example.data.remote.StudyHubRepository
import com.example.ui.components.StudyHubBrandLogo
import com.example.ui.screens.AdminShellScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExamPapersScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudyMaterialsScreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LavenderSurface
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.StudyHubTheme
import com.example.ui.viewmodel.QuizMode
import com.example.ui.viewmodel.StudyHubViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyHubApp()
        }
    }
}

private enum class StudentSubRoute {
    EXAM_PAPERS,
    NOTIFICATIONS,
    SEARCH,
    PREMIUM
}

@Composable
private fun StudyHubApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val firestoreDb = remember {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }
    val roomDb = remember { StudyHubDatabase.getInstance(appContext) }
    val dao = remember { roomDb.studyHubDao() }
    val syncManager = remember { OfflineSyncManager(appContext, dao, firestoreDb) }
    val repository = remember { StudyHubRepository(firestoreDb, dao, syncManager) }

    val authUser by AuthManager.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )
    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true }
            )
        }
    }

    if (!autoSignInChecked) {
        StudyHubTheme(darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyDeep),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StudyHubBrandLogo(subtitle = "Learn. Practice. Achieve.", isDarkSurface = true)
                    Spacer(modifier = Modifier.height(20.dp))
                    CircularProgressIndicator(color = ElectricPurple)
                }
            }
        }
        return
    }

    val currentUser = authUser
    if (currentUser == null) {
        StudyHubTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
        return
    }

    key(currentUser.uid) {
        val viewModel: StudyHubViewModel = viewModel(
            key = currentUser.uid,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudyHubViewModel(
                        repository = repository,
                        syncManager = syncManager,
                        currentUserId = currentUser.uid,
                        currentUserName = currentUser.displayName ?: "Momrooj Ali",
                        currentUserEmail = currentUser.email ?: ""
                    ) as T
                }
            }
        )

        val profile by viewModel.profile.collectAsStateWithLifecycle()
        val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
        val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
        val classes by viewModel.classes.collectAsStateWithLifecycle()
        val subjects by viewModel.subjects.collectAsStateWithLifecycle()
        val chapters by viewModel.chapters.collectAsStateWithLifecycle()
        val materials by viewModel.materials.collectAsStateWithLifecycle()
        val downloadedMaterialIds by viewModel.downloadedMaterialIds.collectAsStateWithLifecycle()
        val quizzes by viewModel.quizzes.collectAsStateWithLifecycle()
        val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()
        val examPapers by viewModel.examPapers.collectAsStateWithLifecycle()
        val downloadedPaperIds by viewModel.downloadedPaperIds.collectAsStateWithLifecycle()
        val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
        val quizAttempts by viewModel.quizAttempts.collectAsStateWithLifecycle()
        val readingProgress by viewModel.readingProgress.collectAsStateWithLifecycle()
        val downloads by viewModel.downloads.collectAsStateWithLifecycle()
        val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
        val announcements by viewModel.announcements.collectAsStateWithLifecycle()
        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
        val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
        val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(snackbarMessage) {
            val msg = snackbarMessage
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearMessage()
            }
        }

        val showAdmin = isAdminMode && viewModel.isAuthorizedAdmin
        StudyHubTheme(darkTheme = profile.darkThemeEnabled || showAdmin) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showAdmin) {
                    BackHandler { viewModel.setAdminMode(false) }
                    AdminShellScreen(
                        profile = profile,
                        classes = classes,
                        subjects = subjects,
                        chapters = chapters,
                        materials = materials,
                        quizzes = quizzes,
                        questions = allQuestions,
                        examPapers = examPapers,
                        announcements = announcements,
                        adminUsers = adminUsers,
                        appSettings = appSettings,
                        onSaveClass = viewModel::saveSchoolClass,
                        onDeleteClass = viewModel::deleteSchoolClass,
                        onSaveSubject = viewModel::saveSubject,
                        onSaveChapter = viewModel::saveChapter,
                        onSaveMaterial = viewModel::saveStudyMaterial,
                        onTogglePublishMaterial = viewModel::toggleMaterialPublish,
                        onDeleteMaterial = viewModel::deleteStudyMaterial,
                        onSaveQuizWithQuestion = viewModel::saveQuizWithQuestion,
                        onAddQuestionToQuiz = viewModel::addQuestionToQuiz,
                        onSaveExamPaper = viewModel::saveExamPaper,
                        onSendAnnouncement = viewModel::sendAnnouncement,
                        onSaveAppSettings = viewModel::saveAppSettings,
                        onSwitchToStudentView = { viewModel.setAdminMode(false) }
                    )
                } else {
                    StudentShellScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onSignOut = {
                            AuthManager.signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onBeforeFirebaseSignOut = { viewModel.cleanupBeforeSignOut() }
                            )
                        },
                        onDeleteAccount = {
                            viewModel.deleteAccountData {
                                AuthManager.signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onBeforeFirebaseSignOut = { viewModel.cleanupBeforeSignOut() }
                                )
                            }
                        }
                    )
                }

                if (showAdmin) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentShellScreen(
    viewModel: StudyHubViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val downloadedMaterialIds by viewModel.downloadedMaterialIds.collectAsStateWithLifecycle()
    val quizzes by viewModel.quizzes.collectAsStateWithLifecycle()
    val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val examPapers by viewModel.examPapers.collectAsStateWithLifecycle()
    val downloadedPaperIds by viewModel.downloadedPaperIds.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val quizAttempts by viewModel.quizAttempts.collectAsStateWithLifecycle()
    val readingProgress by viewModel.readingProgress.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var subRoute by remember { mutableStateOf<StudentSubRoute?>(null) }
    var studyInitialSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var studyInitialMaterial by remember { mutableStateOf<StudyMaterial?>(null) }
    var libraryInitialTab by remember { mutableIntStateOf(0) }

    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.materialId }.toSet() }
    val isPremiumUser = profile.subscriptionTier == "premium"

    if (subRoute != null) {
        BackHandler { subRoute = null }
    } else if (selectedTab != 0) {
        BackHandler { selectedTab = 0 }
    }

    val hideBottomBar = (selectedTab == 2 && activeQuiz != null) ||
        subRoute == StudentSubRoute.NOTIFICATIONS ||
        subRoute == StudentSubRoute.SEARCH ||
        subRoute == StudentSubRoute.PREMIUM

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!hideBottomBar) {
                val activeBottomIdx = if (subRoute == StudentSubRoute.EXAM_PAPERS) 2 else selectedTab
                StudentBottomNavigationBar(
                    selectedTab = activeBottomIdx,
                    onSelectTab = { idx ->
                        subRoute = null
                        if (idx != 1) {
                            studyInitialSubject = null
                            studyInitialMaterial = null
                        }
                        selectedTab = idx
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (subRoute) {
                StudentSubRoute.EXAM_PAPERS -> ExamPapersScreen(
                    selectedClass = profile.selectedClass,
                    examPapers = examPapers,
                    downloadedPaperIds = downloadedPaperIds,
                    onToggleDownloadPaper = viewModel::toggleExamPaperDownload,
                    onSelectClass = viewModel::updateSelectedClass,
                    onBack = { subRoute = null }
                )

                StudentSubRoute.NOTIFICATIONS -> NotificationsScreen(
                    announcements = announcements,
                    profile = profile,
                    onUpdatePrefs = viewModel::updateNotificationPrefs,
                    onBack = { subRoute = null }
                )

                StudentSubRoute.SEARCH -> GlobalSearchScreen(
                    subjects = subjects,
                    chapters = chapters,
                    materials = materials,
                    questions = allQuestions,
                    examPapers = examPapers,
                    onOpenMaterial = { mat ->
                        studyInitialSubject = null
                        studyInitialMaterial = mat
                        subRoute = null
                        selectedTab = 1
                    },
                    onBack = { subRoute = null }
                )

                StudentSubRoute.PREMIUM -> PremiumScreen(
                    profile = profile,
                    appSettings = appSettings,
                    onBack = { subRoute = null }
                )

                null -> when (selectedTab) {
                    0 -> StudentHomeScreen(
                        profile = profile,
                        subjects = subjects,
                        materials = materials,
                        downloadedIds = downloadedMaterialIds,
                        recentProgress = readingProgress,
                        announcementCount = announcements.size,
                        syncStatus = syncStatus,
                        pendingSyncCount = pendingSyncCount,
                        appSettings = appSettings,
                        onSelectClass = viewModel::updateSelectedClass,
                        onOpenMaterials = {
                            studyInitialSubject = null
                            studyInitialMaterial = null
                            selectedTab = 1
                        },
                        onOpenSubject = { subj ->
                            studyInitialSubject = subj
                            studyInitialMaterial = null
                            selectedTab = 1
                        },
                        onOpenMaterial = { mat ->
                            studyInitialSubject = null
                            studyInitialMaterial = mat
                            selectedTab = 1
                        },
                        onOpenQuizzes = { selectedTab = 2 },
                        onStartDailyChallenge = {
                            val challengeQuiz = quizzes.firstOrNull {
                                it.published && it.classGrade == profile.selectedClass
                            } ?: quizzes.firstOrNull { it.published }
                            if (challengeQuiz != null) {
                                viewModel.startQuiz(challengeQuiz, QuizMode.PRACTICE, randomize = false)
                                selectedTab = 2
                            } else {
                                viewModel.showMessage("Daily Challenge quiz is syncing...")
                            }
                        },
                        onOpenExamPapers = { subRoute = StudentSubRoute.EXAM_PAPERS },
                        onOpenDownloads = {
                            libraryInitialTab = 1
                            selectedTab = 3
                        },
                        onOpenNotifications = { subRoute = StudentSubRoute.NOTIFICATIONS },
                        onOpenSearch = { subRoute = StudentSubRoute.SEARCH },
                        onOpenPremium = { subRoute = StudentSubRoute.PREMIUM },
                        onSyncNow = viewModel::triggerManualSync
                    )

                    1 -> StudyMaterialsScreen(
                        selectedClass = profile.selectedClass,
                        subjects = subjects,
                        chapters = chapters,
                        materials = materials,
                        bookmarkedMaterialIds = bookmarkedIds,
                        downloadedMaterialIds = downloadedMaterialIds,
                        isPremiumUser = isPremiumUser,
                        initialSubject = studyInitialSubject,
                        initialMaterial = studyInitialMaterial,
                        onSelectClass = viewModel::updateSelectedClass,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onToggleDownload = viewModel::toggleDownload,
                        onMarkRead = { mat -> viewModel.markMaterialRead(mat, 100L) },
                        onOpenPremium = { subRoute = StudentSubRoute.PREMIUM },
                        onOpenGlobalSearch = { subRoute = StudentSubRoute.SEARCH },
                        onBack = {
                            studyInitialSubject = null
                            studyInitialMaterial = null
                            selectedTab = 0
                        }
                    )

                    2 -> QuizScreen(
                        selectedClass = profile.selectedClass,
                        quizzes = quizzes,
                        quizAttempts = quizAttempts,
                        activeQuiz = activeQuiz,
                        onStartQuiz = viewModel::startQuiz,
                        onSelectSingleOption = viewModel::selectSingleOption,
                        onToggleMultiOption = viewModel::toggleMultiOption,
                        onUpdateFillBlank = viewModel::updateFillBlank,
                        onConfirmPractice = viewModel::confirmPracticeAnswer,
                        onNextQuestion = viewModel::nextQuestion,
                        onPreviousQuestion = viewModel::previousQuestion,
                        onSubmitQuiz = viewModel::submitQuiz,
                        onCloseQuiz = viewModel::closeQuiz,
                        isQuestionCorrect = viewModel::isQuestionCorrect
                    )

                    3 -> LibraryScreen(
                        bookmarks = bookmarks,
                        downloads = downloads,
                        recentProgress = readingProgress,
                        allMaterials = materials,
                        syncStatus = syncStatus,
                        pendingSyncCount = pendingSyncCount,
                        initialTab = libraryInitialTab,
                        onOpenMaterial = { mat ->
                            studyInitialSubject = null
                            studyInitialMaterial = mat
                            selectedTab = 1
                        },
                        onRemoveBookmark = viewModel::toggleBookmark,
                        onSyncNow = viewModel::triggerManualSync,
                        onOpenSearch = { subRoute = StudentSubRoute.SEARCH },
                        onBack = { selectedTab = 0 }
                    )

                    else -> ProfileScreen(
                        profile = profile,
                        isAuthorizedAdmin = viewModel.isAuthorizedAdmin,
                        quizAttempts = quizAttempts,
                        onUpdateName = viewModel::updateDisplayName,
                        onSelectClass = viewModel::updateSelectedClass,
                        onToggleDarkTheme = viewModel::toggleDarkTheme,
                        onUpdateNotificationPrefs = viewModel::updateNotificationPrefs,
                        onOpenBookmarks = {
                            libraryInitialTab = 0
                            selectedTab = 3
                        },
                        onOpenDownloads = {
                            libraryInitialTab = 1
                            selectedTab = 3
                        },
                        onOpenPremium = { subRoute = StudentSubRoute.PREMIUM },
                        onSwitchToAdmin = { viewModel.setAdminMode(true) },
                        onDeleteAccount = onDeleteAccount,
                        onSignOut = onSignOut,
                        onBack = { selectedTab = 0 }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple("Home", Icons.Rounded.Home, 0),
        Triple("Study", Icons.AutoMirrored.Rounded.MenuBook, 1),
        Triple("Practice", Icons.Rounded.TaskAlt, 2),
        Triple("Library", Icons.AutoMirrored.Rounded.LibraryBooks, 3),
        Triple("Profile", Icons.Rounded.Person, 4)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                ambientColor = Color(0xFF3E46A8).copy(alpha = 0.45f),
                spotColor = Color(0xFF3E46A8).copy(alpha = 0.45f)
            ),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, idx) ->
                val selected = selectedTab == idx
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp, bottom = 2.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = ElectricPurple.copy(alpha = 0.55f),
                                spotColor = ElectricPurple.copy(alpha = 0.55f)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF4712BD), Color(0xFF28067A)))
                            )
                            .clickable { onSelectTab(idx) }
                            .testTag("nav_${label.lowercase()}")
                            .padding(end = 2.5.dp, bottom = 3.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF9348FF), Color(0xFF5D1FE8)))
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val tint = MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTab(idx) }
                            .testTag("nav_${label.lowercase()}")
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
