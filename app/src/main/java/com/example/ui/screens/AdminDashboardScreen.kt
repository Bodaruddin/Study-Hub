package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.ChapterItem
import com.example.data.model.ExamPaper
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.remote.SupabaseTestResult
import com.example.ui.components.PlatformAnalyticsLineChart
import com.example.ui.components.StudyHubBrandLogo
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueEnd
import com.example.ui.theme.BlueStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenEnd
import com.example.ui.theme.GreenStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.OrangeEnd
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PinkEnd
import com.example.ui.theme.PinkStart
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealEnd
import com.example.ui.theme.TealStart
import kotlinx.coroutines.launch

import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.LaunchedEffect
import com.example.data.model.SupabaseMappers
import com.example.data.model.SyncStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class AdminModule(val label: String) {
    DASHBOARD("Dashboard"),
    CLASSES("Manage Classes"),
    SUBJECTS("Manage Subjects"),
    CHAPTERS("Manage Chapters"),
    MATERIALS("Upload & Materials"),
    QUIZZES("Quizzes & MCQs"),
    PAPERS("Model & Sample Papers"),
    STUDENTS("Manage Students"),
    ANNOUNCEMENTS("Announcements"),
    ANALYTICS("Platform Analytics"),
    DATABASE("Manage Database"),
    ADS("Manage Ads"),
    SETTINGS("App Settings"),
    SUPPORT("Reports & Support")
}

private fun AdminModule.drawerIcon(): ImageVector = when (this) {
    AdminModule.DASHBOARD -> Icons.Rounded.Dashboard
    AdminModule.CLASSES -> Icons.Rounded.School
    AdminModule.SUBJECTS -> Icons.Rounded.AutoStories
    AdminModule.CHAPTERS -> Icons.Rounded.MenuBook
    AdminModule.MATERIALS -> Icons.Rounded.CloudUpload
    AdminModule.QUIZZES -> Icons.Rounded.Quiz
    AdminModule.PAPERS -> Icons.Rounded.Description
    AdminModule.STUDENTS -> Icons.Rounded.Group
    AdminModule.ANNOUNCEMENTS -> Icons.Rounded.Campaign
    AdminModule.ANALYTICS -> Icons.Rounded.Analytics
    AdminModule.DATABASE -> Icons.Rounded.Storage
    AdminModule.ADS -> Icons.Rounded.AdsClick
    AdminModule.SETTINGS -> Icons.Rounded.Settings
    AdminModule.SUPPORT -> Icons.Rounded.HeadsetMic
}

@Composable
fun AdminShellScreen(
    profile: UserProfile,
    classes: List<SchoolClass>,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    materials: List<StudyMaterial>,
    quizzes: List<QuizItem>,
    questions: List<QuizQuestion>,
    examPapers: List<ExamPaper>,
    announcements: List<AnnouncementItem>,
    adminUsers: List<UserProfile>,
    appSettings: AppSettings,
    syncStatus: SyncStatus,
    pendingSyncCount: Int,
    isDatabaseBusy: Boolean,
    supabaseStatus: SupabaseConnectionStatus,
    onSaveClass: (SchoolClass, Boolean) -> Unit,
    onTogglePublishClass: (SchoolClass) -> Unit,
    onDeleteClass: (String) -> Unit,
    onSaveSubject: (SubjectItem, Boolean) -> Unit,
    onTogglePublishSubject: (SubjectItem) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveChapter: (ChapterItem, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishChapter: (ChapterItem) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onSaveMaterial: (StudyMaterial, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishMaterial: (StudyMaterial) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onSaveQuizWithQuestion: (QuizItem, QuizQuestion?) -> Unit,
    onTogglePublishQuiz: (QuizItem) -> Unit,
    onDeleteQuiz: (String) -> Unit,
    onAddQuestionToQuiz: (QuizQuestion) -> Unit,
    onDeleteQuizQuestion: (String, String) -> Unit,
    onSaveExamPaper: (ExamPaper, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishExamPaper: (ExamPaper) -> Unit,
    onDeleteExamPaper: (String) -> Unit,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>>,
    onSaveAnnouncement: (AnnouncementItem) -> Unit,
    onTogglePublishAnnouncement: (AnnouncementItem) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onRefreshStudents: () -> Unit,
    onUpdateStudent: (UserProfile, String, Long, String, Boolean) -> Unit,
    onSetStudentBlocked: (UserProfile, Boolean) -> Unit,
    onDeleteStudent: (UserProfile) -> Unit,
    onSaveAppSettings: (AppSettings) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSeedMissingCloud: () -> Unit,
    onFlushSyncQueue: () -> Unit,
    onResetLocalCache: () -> Unit,
    onTestSupabaseConnection: suspend (String, String) -> SupabaseTestResult,
    onSaveSupabaseConfig: suspend (String, String) -> Result<SupabaseTestResult>,
    onClearSupabaseConfig: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSwitchToStudentView: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeModule by remember { mutableStateOf(AdminModule.DASHBOARD) }
    var activeBottomTab by remember { mutableIntStateOf(0) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var showUploadMaterialDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<StudyMaterial?>(null) }
    var showAddMcqDialog by remember { mutableStateOf(false) }
    var preselectedQuizIdForMcq by remember { mutableStateOf<String?>(null) }
    var editingQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var editingQuiz by remember { mutableStateOf<QuizItem?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<AnnouncementItem?>(null) }
    var showSupabaseSetupDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeModule, supabaseStatus.configured) {
        if (supabaseStatus.configured && activeModule in setOf(
                AdminModule.DASHBOARD,
                AdminModule.CLASSES,
                AdminModule.STUDENTS,
                AdminModule.ANALYTICS,
                AdminModule.DATABASE,
                AdminModule.SUPPORT
            )
        ) {
            onRefreshStudents()
        }
    }

    if (showAddClassDialog || editingClass != null) {
        val target = editingClass
        AdminClassEditorDialog(
            initialClass = target,
            onDismiss = {
                showAddClassDialog = false
                editingClass = null
            },
            onSave = { item ->
                onSaveClass(item, target == null)
                showAddClassDialog = false
                editingClass = null
            }
        )
    }
    val availableGrades = remember(classes) {
        classes.map { it.grade }.distinct().sorted().ifEmpty { (6L..12L).toList() }
    }

    if (showAddSubjectDialog || editingSubject != null) {
        val target = editingSubject
        AdminSubjectEditorDialog(
            initialSubject = target,
            availableGrades = availableGrades,
            onDismiss = {
                showAddSubjectDialog = false
                editingSubject = null
            },
            onSave = { item ->
                onSaveSubject(item, target == null)
                showAddSubjectDialog = false
                editingSubject = null
            }
        )
    }
    if (showUploadMaterialDialog || editingMaterial != null) {
        val targetMaterial = editingMaterial
        AdminUploadMaterialDialog(
            subjects = subjects,
            chapters = chapters,
            initialMaterial = targetMaterial,
            availableGrades = availableGrades,
            onLoadAttachments = onLoadAttachments,
            onDismiss = {
                showUploadMaterialDialog = false
                editingMaterial = null
            },
            onSave = { mat, drafts, onDone ->
                onSaveMaterial(mat, targetMaterial == null, drafts) { ok ->
                    onDone(ok)
                    if (ok) {
                        showUploadMaterialDialog = false
                        editingMaterial = null
                    }
                }
            }
        )
    }
    if (showAddMcqDialog || editingQuestion != null) {
        val targetQ = editingQuestion
        AdminMcqEditorDialog(
            quizzes = quizzes,
            initialQuestion = targetQ,
            preselectedQuizId = preselectedQuizIdForMcq,
            onDismiss = {
                showAddMcqDialog = false
                preselectedQuizIdForMcq = null
                editingQuestion = null
            },
            onSave = { q ->
                onAddQuestionToQuiz(q)
                showAddMcqDialog = false
                preselectedQuizIdForMcq = null
                editingQuestion = null
            }
        )
    }
    if (showCreateQuizDialog || editingQuiz != null) {
        val targetQuiz = editingQuiz
        AdminQuizEditorDialog(
            subjects = subjects,
            chapters = chapters,
            initialQuiz = targetQuiz,
            availableGrades = availableGrades,
            onDismiss = {
                showCreateQuizDialog = false
                editingQuiz = null
            },
            onSave = { quiz, firstQ ->
                onSaveQuizWithQuestion(quiz, firstQ)
                showCreateQuizDialog = false
                editingQuiz = null
            }
        )
    }
    if (showAnnouncementDialog || editingAnnouncement != null) {
        val targetAnn = editingAnnouncement
        AdminAnnouncementEditorDialog(
            initialAnnouncement = targetAnn,
            onDismiss = {
                showAnnouncementDialog = false
                editingAnnouncement = null
            },
            onSave = { item ->
                onSaveAnnouncement(item)
                showAnnouncementDialog = false
                editingAnnouncement = null
            }
        )
    }
    if (showSupabaseSetupDialog) {
        AdminSupabaseSetupDialog(
            status = supabaseStatus,
            onDismiss = { showSupabaseSetupDialog = false },
            onTestConnection = onTestSupabaseConnection,
            onSaveConfig = onSaveSupabaseConfig,
            onClearConfig = onClearSupabaseConfig,
            onShowMessage = onShowMessage
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NavySurface,
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StudyHubBrandLogo(subtitle = "Admin Management", isDarkSurface = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = NavyCardBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    AdminModule.entries.forEach { mod ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = mod.drawerIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text(mod.label) },
                            selected = activeModule == mod,
                            onClick = {
                                activeModule = mod
                                activeBottomTab = when (mod) {
                                    AdminModule.DASHBOARD -> 0
                                    AdminModule.CLASSES -> 1
                                    AdminModule.ANALYTICS -> 2
                                    AdminModule.SETTINGS -> 3
                                    AdminModule.SUPPORT -> 4
                                    else -> activeBottomTab
                                }
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                                selectedTextColor = Color.White,
                                selectedIconColor = Color.White,
                                unselectedTextColor = Color(0xFFC5CEF0),
                                unselectedIconColor = Color(0xFF9AA5D1)
                            ),
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .testTag("admin_drawer_${mod.name.lowercase()}")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = NavyCardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                    NavigationDrawerItem(
                        label = { Text("Switch to Student App", color = Color(0xFF5CE1A8), fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSwitchToStudentView()
                        },
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = NavyDeep,
            bottomBar = {
                AdminBottomNavigationBar(
                    selectedTab = activeBottomTab,
                    onSelectTab = { idx ->
                        activeBottomTab = idx
                        activeModule = when (idx) {
                            0 -> AdminModule.DASHBOARD
                            1 -> AdminModule.CLASSES
                            2 -> AdminModule.ANALYTICS
                            3 -> AdminModule.SETTINGS
                            else -> AdminModule.SUPPORT
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyDeep)
                    .padding(innerPadding)
            ) {
                when (activeModule) {
                    AdminModule.DASHBOARD -> AdminDashboardContent(
                        profile = profile,
                        classes = classes,
                        subjects = subjects,
                        chapters = chapters,
                        materials = materials,
                        quizzes = quizzes,
                        questions = questions,
                        examPapers = examPapers,
                        announcements = announcements,
                        adminUsers = adminUsers,
                        supabaseStatus = supabaseStatus,
                        onOpenSupabaseSetup = { showSupabaseSetupDialog = true },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenAnnouncements = { activeModule = AdminModule.ANNOUNCEMENTS },
                        onAddClass = {
                            editingClass = null
                            showAddClassDialog = true
                        },
                        onAddSubject = {
                            editingSubject = null
                            showAddSubjectDialog = true
                        },
                        onUploadMaterial = {
                            editingMaterial = null
                            showUploadMaterialDialog = true
                        },
                        onAddMcqs = {
                            editingQuestion = null
                            preselectedQuizIdForMcq = null
                            showAddMcqDialog = true
                        },
                        onCreateTest = {
                            editingQuiz = null
                            showCreateQuizDialog = true
                        },
                        onSendAnnouncement = {
                            editingAnnouncement = null
                            showAnnouncementDialog = true
                        },
                        onOpenManageStudents = {
                            onRefreshStudents()
                            activeModule = AdminModule.STUDENTS
                        },
                        onOpenManageMaterials = { activeModule = AdminModule.MATERIALS },
                        onOpenManageQuizzes = { activeModule = AdminModule.QUIZZES },
                        onOpenManageSubjects = { activeModule = AdminModule.SUBJECTS },
                        onViewAllQuick = { activeModule = AdminModule.MATERIALS },
                        onViewAllActivity = { activeModule = AdminModule.ANALYTICS },
                        onSwitchToStudent = onSwitchToStudentView
                    )

                    else -> AdminSubmoduleScaffold(
                        module = activeModule,
                        currentAdminUid = profile.id.ifBlank { profile.userId },
                        classes = classes,
                        subjects = subjects,
                        chapters = chapters,
                        materials = materials,
                        quizzes = quizzes,
                        questions = questions,
                        examPapers = examPapers,
                        announcements = announcements,
                        adminUsers = adminUsers,
                        appSettings = appSettings,
                        syncStatus = syncStatus,
                        pendingSyncCount = pendingSyncCount,
                        isDatabaseBusy = isDatabaseBusy,
                        supabaseStatus = supabaseStatus,
                        onBackToDashboard = {
                            activeModule = AdminModule.DASHBOARD
                            activeBottomTab = 0
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateModule = { target ->
                            if (target == AdminModule.STUDENTS) onRefreshStudents()
                            activeModule = target
                        },
                        onOpenAddClass = {
                            editingClass = null
                            showAddClassDialog = true
                        },
                        onEditClass = { editingClass = it },
                        onTogglePublishClass = onTogglePublishClass,
                        onDeleteClass = onDeleteClass,
                        onOpenAddSubject = {
                            editingSubject = null
                            showAddSubjectDialog = true
                        },
                        onEditSubject = { editingSubject = it },
                        onTogglePublishSubject = onTogglePublishSubject,
                        onDeleteSubject = onDeleteSubject,
                        onSaveChapter = onSaveChapter,
                        onTogglePublishChapter = onTogglePublishChapter,
                        onDeleteChapter = onDeleteChapter,
                        onOpenUploadMaterial = {
                            editingMaterial = null
                            showUploadMaterialDialog = true
                        },
                        onEditMaterial = { mat -> editingMaterial = mat },
                        onTogglePublishMaterial = onTogglePublishMaterial,
                        onDeleteMaterial = onDeleteMaterial,
                        onOpenAddMcq = { quizId ->
                            editingQuestion = null
                            preselectedQuizIdForMcq = quizId
                            showAddMcqDialog = true
                        },
                        onEditQuestion = { q -> editingQuestion = q },
                        onDeleteQuestion = onDeleteQuizQuestion,
                        onOpenCreateQuiz = {
                            editingQuiz = null
                            showCreateQuizDialog = true
                        },
                        onEditQuiz = { quiz -> editingQuiz = quiz },
                        onTogglePublishQuiz = onTogglePublishQuiz,
                        onDeleteQuiz = onDeleteQuiz,
                        onSaveExamPaper = onSaveExamPaper,
                        onTogglePublishExamPaper = onTogglePublishExamPaper,
                        onDeleteExamPaper = onDeleteExamPaper,
                        onLoadAttachments = onLoadAttachments,
                        onOpenAnnouncementDialog = {
                            editingAnnouncement = null
                            showAnnouncementDialog = true
                        },
                        onEditAnnouncement = { editingAnnouncement = it },
                        onTogglePublishAnnouncement = onTogglePublishAnnouncement,
                        onDeleteAnnouncement = onDeleteAnnouncement,
                        onRefreshStudents = onRefreshStudents,
                        onUpdateStudent = onUpdateStudent,
                        onSetStudentBlocked = onSetStudentBlocked,
                        onDeleteStudent = onDeleteStudent,
                        onSaveAppSettings = onSaveAppSettings,
                        onSyncCloudNow = onSyncCloudNow,
                        onSeedMissingCloud = onSeedMissingCloud,
                        onFlushSyncQueue = onFlushSyncQueue,
                        onResetLocalCache = onResetLocalCache,
                        onOpenSupabaseSetup = { showSupabaseSetupDialog = true },
                        onSwitchToStudent = onSwitchToStudentView
                    )
                }
            }
        }
    }
}

private data class AdminActivityEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val trailingLabel: String,
    val timestampMillis: Long
)

@Composable
private fun AdminDashboardContent(
    profile: UserProfile,
    classes: List<SchoolClass>,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    materials: List<StudyMaterial>,
    quizzes: List<QuizItem>,
    questions: List<QuizQuestion>,
    examPapers: List<ExamPaper>,
    announcements: List<AnnouncementItem>,
    adminUsers: List<UserProfile>,
    supabaseStatus: SupabaseConnectionStatus,
    onOpenSupabaseSetup: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onAddClass: () -> Unit,
    onAddSubject: () -> Unit,
    onUploadMaterial: () -> Unit,
    onAddMcqs: () -> Unit,
    onCreateTest: () -> Unit,
    onSendAnnouncement: () -> Unit,
    onOpenManageStudents: () -> Unit,
    onOpenManageMaterials: () -> Unit,
    onOpenManageQuizzes: () -> Unit,
    onOpenManageSubjects: () -> Unit,
    onViewAllQuick: () -> Unit,
    onViewAllActivity: () -> Unit,
    onSwitchToStudent: () -> Unit
) {
    val studentAccounts = remember(adminUsers) { adminUsers.filter { it.role != "admin" } }
    val totalStudents = studentAccounts.size
    val activeStudents = studentAccounts.count { !it.blocked }
    val blockedStudents = studentAccounts.count { it.blocked }
    var analyticsRange by remember { mutableStateOf("By Class (6–12)") }
    var rangeMenuOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar with 3D glassy controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_menu_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color(0xFF06091F),
                                    spotColor = Color(0xFF06091F)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF212B66), Color(0xFF141B44))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.34f), NavyCardBorder)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = "Open menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    StudyHubBrandLogo(subtitle = "Admin Panel", isDarkSurface = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        IconButton(onClick = onOpenAnnouncements) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF06091F),
                                        spotColor = Color(0xFF06091F)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF212B66), Color(0xFF141B44))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.34f), NavyCardBorder)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    contentDescription = "System alerts",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (announcements.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed)
                                    .border(1.2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = announcements.size.coerceAtMost(9).toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onSwitchToStudent)
                            .testTag("admin_avatar_switch")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_student_avatar),
                            contentDescription = "Admin avatar (tap for Student view)",
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(
                                    elevation = 5.dp,
                                    shape = CircleShape,
                                    ambientColor = ElectricPurple.copy(alpha = 0.55f),
                                    spotColor = ElectricPurple.copy(alpha = 0.55f)
                                )
                                .clip(CircleShape)
                                .border(
                                    width = 1.8.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.85f), Color(0xFF9F7BFF))
                                    ),
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                                .border(1.5.dp, NavyDeep, CircleShape)
                        )
                    }
                }
            }
        }

        // Welcome Hero Banner — 3D glassy floating card
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFF4263EB).copy(alpha = 0.94f),
                        Color(0xFF673AB7).copy(alpha = 0.96f),
                        Color(0xFF4F28E8).copy(alpha = 0.96f)
                    )
                ),
                depthColor = Color(0xFF23107A),
                shadowColor = Color(0xFF4263EB),
                cornerRadius = 22.dp,
                depthHeight = 5.dp,
                floatingLift = 5.dp,
                glassGlossAlpha = 1.1f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFDCE4FF)
                        )
                        Text(
                            text = "${profile.displayName.substringBefore(" ").ifBlank { "Admin" }}! 👋",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Manage. Organize. Empower.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE5EAFF)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_admin_hero),
                        contentDescription = "Admin illustration",
                        modifier = Modifier
                            .width(126.dp)
                            .height(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.2.dp,
                                color = Color.White.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Supabase Database Setup Banner on Admin Dashboard
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    if (supabaseStatus.configured) {
                        listOf(Color(0xFF0F3D3E), Color(0xFF134E4A))
                    } else {
                        listOf(Color(0xFF7C2D12), Color(0xFF9A3412))
                    }
                ),
                depthColor = Color(0xFF071A1C),
                shadowColor = if (supabaseStatus.configured) TealEnd else OrangeEnd,
                cornerRadius = 18.dp,
                depthHeight = 4.dp,
                floatingLift = 4.dp,
                glassGlossAlpha = 0.85f,
                onClick = onOpenSupabaseSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_supabase_setup_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = "Setup Supabase Database",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (supabaseStatus.configured) {
                                "Supabase Connected (${supabaseStatus.hostLabel ?: "Ready"})"
                            } else {
                                "Setup Supabase Database"
                            },
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (supabaseStatus.configured) {
                                "${supabaseStatus.sourceLabel} • Tap to edit credentials or copy SQL"
                            } else {
                                "Tap to enter Project URL, Anon Key & copy SQL schema"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // 2x2 Vibrant 3D Glassy Floating KPI Cards (100% real data)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiCard(
                        icon = Icons.Rounded.Group,
                        value = "%,d".format(totalStudents),
                        label = "Total Students",
                        trend = if (blockedStudents > 0) "$activeStudents active • $blockedStudents blk" else "$activeStudents active",
                        colors = listOf(BlueStart, BlueEnd),
                        depthColor = Color(0xFF092E8A),
                        onClick = onOpenManageStudents,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_manage_students")
                    )
                    AdminKpiCard(
                        icon = Icons.Rounded.MenuBook,
                        value = "${materials.size}",
                        label = "Study Materials",
                        trend = "${materials.count { it.published }} published",
                        colors = listOf(GreenStart, GreenEnd),
                        depthColor = Color(0xFF045E34),
                        onClick = onOpenManageMaterials,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiCard(
                        icon = Icons.Rounded.Description,
                        value = "${quizzes.size}",
                        label = "Total Quizzes",
                        trend = "${questions.size} MCQs",
                        colors = listOf(OrangeStart, OrangeEnd),
                        depthColor = Color(0xFF9E3C00),
                        onClick = onOpenManageQuizzes,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        icon = Icons.Rounded.AutoStories,
                        value = "${subjects.size}",
                        label = "Subjects",
                        trend = "${chapters.size} ch • ${examPapers.size} papers",
                        colors = listOf(PurpleStart, PurpleEnd),
                        depthColor = Color(0xFF33088C),
                        onClick = onOpenManageSubjects,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_manage_subjects")
                    )
                }
            }
        }

        // Platform Analytics Chart Card — Dark 3D Glassy Surface
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1D2761).copy(alpha = 0.94f),
                        Color(0xFF11173D).copy(alpha = 0.96f)
                    )
                ),
                depthColor = Color(0xFF070B21),
                shadowColor = Color(0xFF05081A),
                highlightBorderColor = Color.White.copy(alpha = 0.28f),
                borderEndColor = NavyCardBorder,
                cornerRadius = 22.dp,
                depthHeight = 4.5.dp,
                floatingLift = 4.5.dp,
                glassGlossAlpha = 0.65f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                PurpleStart.copy(alpha = 0.35f),
                                                PurpleEnd.copy(alpha = 0.20f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = null,
                                    tint = Color(0xFFC5A3FF),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Platform Analytics",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Box {
                            Tactile3DCard(
                                faceBrush = Brush.linearGradient(
                                    listOf(Color(0xFF25306E), Color(0xFF17204D))
                                ),
                                depthColor = Color(0xFF080D26),
                                shadowColor = Color(0xFF05081A),
                                highlightBorderColor = Color.White.copy(alpha = 0.34f),
                                borderEndColor = NavyCardBorder,
                                cornerRadius = 50.dp,
                                depthHeight = 2.5.dp,
                                floatingLift = 2.dp,
                                glassGlossAlpha = 0.55f,
                                fillMaxWidth = false,
                                onClick = { rangeMenuOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(analyticsRange, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = rangeMenuOpen, onDismissRequest = { rangeMenuOpen = false }) {
                                listOf("By Class (6–12)", "Last 7 Days", "Last 30 Days").forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                                        analyticsRange = opt
                                        rangeMenuOpen = false
                                    })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val zoneId = remember { ZoneId.systemDefault() }
                    val today = LocalDate.now(zoneId)
                    val chartData = remember(analyticsRange, classes, studentAccounts, materials, quizzes, examPapers, announcements) {
                        when (analyticsRange) {
                            "By Class (6–12)" -> {
                                val grades = classes.map { it.grade }.distinct().sorted().ifEmpty { (6L..12L).toList() }
                                val xLabels = grades.map { "C$it" }
                                val counts1 = grades.map { g -> studentAccounts.count { it.selectedClass == g } }
                                val counts2 = grades.map { g ->
                                    materials.count { it.classGrade == g } +
                                        quizzes.count { it.classGrade == g } +
                                        examPapers.count { it.classGrade == g }
                                }
                                Triple(xLabels, counts1 to counts2, "Students" to "Content")
                            }
                            "Last 7 Days" -> {
                                val days = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
                                val xLabels = days.map { d -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US) }
                                val counts1 = days.map { day ->
                                    studentAccounts.count { u ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(u.createdAt)
                                        ms == null || !Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate().isAfter(day)
                                    }
                                }
                                val counts2 = days.map { day ->
                                    val activeUsersOnDay = studentAccounts.count { u ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(u.updatedAt ?: u.createdAt)
                                        ms != null && Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate() == day
                                    }
                                    val annsOnDay = announcements.count { a ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(a.createdAt)
                                        ms != null && Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate() == day
                                    }
                                    activeUsersOnDay + annsOnDay
                                }
                                Triple(xLabels, counts1 to counts2, "Students" to "Activity")
                            }
                            else -> {
                                val buckets = (6 downTo 0).map { idx -> today.minusDays((idx * 5).toLong()) }
                                val xLabels = buckets.mapIndexed { idx, d ->
                                    if (idx == buckets.lastIndex) "Now" else "-${(buckets.lastIndex - idx) * 5}d"
                                }
                                val counts1 = buckets.map { day ->
                                    studentAccounts.count { u ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(u.createdAt)
                                        ms == null || !Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate().isAfter(day)
                                    }
                                }
                                val counts2 = buckets.mapIndexed { idx, day ->
                                    val prev = if (idx == 0) day.minusDays(5) else buckets[idx - 1]
                                    val activeInWindow = studentAccounts.count { u ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(u.updatedAt ?: u.createdAt)
                                        if (ms == null) false else {
                                            val ld = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
                                            ld.isAfter(prev) && !ld.isAfter(day)
                                        }
                                    }
                                    val annsInWindow = announcements.count { a ->
                                        val ms = SupabaseMappers.parseIsoToMillisOrNull(a.createdAt)
                                        if (ms == null) false else {
                                            val ld = Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
                                            ld.isAfter(prev) && !ld.isAfter(day)
                                        }
                                    }
                                    activeInWindow + annsInWindow
                                }
                                Triple(xLabels, counts1 to counts2, "Students" to "Activity")
                            }
                        }
                    }
                    val xLabels = chartData.first
                    val series1Counts = chartData.second.first
                    val series2Counts = chartData.second.second
                    val legend1 = chartData.third.first
                    val legend2 = chartData.third.second
                    val rawMax = maxOf(series1Counts.maxOrNull() ?: 0, series2Counts.maxOrNull() ?: 0)
                    val yMax = ((maxOf(rawMax, 3) + 2) / 3) * 3
                    val yLabels = listOf(yMax, (yMax * 2) / 3, yMax / 3, 0).map { it.toString() }
                    val studentsPts = series1Counts.map { it.toFloat() / yMax.toFloat() }
                    val secondPts = series2Counts.map { it.toFloat() / yMax.toFloat() }

                    PlatformAnalyticsLineChart(
                        studentsSeries = studentsPts,
                        quizzesSeries = secondPts,
                        days = xLabels,
                        yAxisLabels = yLabels,
                        legend1Label = legend1,
                        legend2Label = legend2,
                        peakLabel = "Peak: $rawMax"
                    )
                }
            }
        }

        // Quick Actions 2x3 Colored 3D Glassy Grid inside Dark 3D Glassy Card
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1D2761).copy(alpha = 0.94f),
                        Color(0xFF11173D).copy(alpha = 0.96f)
                    )
                ),
                depthColor = Color(0xFF070B21),
                shadowColor = Color(0xFF05081A),
                highlightBorderColor = Color.White.copy(alpha = 0.28f),
                borderEndColor = NavyCardBorder,
                cornerRadius = 22.dp,
                depthHeight = 4.5.dp,
                floatingLift = 4.5.dp,
                glassGlossAlpha = 0.65f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(OrangeStart.copy(alpha = 0.22f))
                                    .border(1.dp, OrangeStart.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = OrangeStart, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quick Actions",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View All →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFB394FF),
                            modifier = Modifier.clickable(onClick = onViewAllQuick)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminQuickActionButton("Add Class", Icons.Rounded.Add, listOf(BlueStart, BlueEnd), Color(0xFF092E8A), onAddClass, Modifier.weight(1f))
                        AdminQuickActionButton("Add Subject", Icons.Rounded.AutoStories, listOf(GreenStart, GreenEnd), Color(0xFF045E34), onAddSubject, Modifier.weight(1f))
                        AdminQuickActionButton("Upload\nMaterials", Icons.Rounded.CloudUpload, listOf(PurpleStart, PurpleEnd), Color(0xFF33088C), onUploadMaterial, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminQuickActionButton("Add MCQs", Icons.Rounded.Quiz, listOf(OrangeStart, OrangeEnd), Color(0xFF9E3C00), onAddMcqs, Modifier.weight(1f))
                        AdminQuickActionButton("Create\nTest", Icons.Rounded.Description, listOf(TealStart, TealEnd), Color(0xFF065466), onCreateTest, Modifier.weight(1f))
                        AdminQuickActionButton("Send\nAnnouncement", Icons.Rounded.Campaign, listOf(PinkStart, PinkEnd), Color(0xFF870B3A), onSendAnnouncement, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Tactile3DCard(
                        faceBrush = Brush.linearGradient(listOf(BlueStart.copy(alpha = 0.94f), PurpleEnd.copy(alpha = 0.94f))),
                        depthColor = Color(0xFF1A0F4D),
                        shadowColor = ElectricPurple,
                        cornerRadius = 14.dp,
                        depthHeight = 3.5.dp,
                        floatingLift = 3.dp,
                        glassGlossAlpha = 1.0f,
                        onClick = onOpenManageStudents,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_manage_students_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manage Students (Edit • Block • Delete)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity Card — Dark 3D Glassy Surface
        item {
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1D2761).copy(alpha = 0.94f),
                        Color(0xFF11173D).copy(alpha = 0.96f)
                    )
                ),
                depthColor = Color(0xFF070B21),
                shadowColor = Color(0xFF05081A),
                highlightBorderColor = Color.White.copy(alpha = 0.28f),
                borderEndColor = NavyCardBorder,
                cornerRadius = 22.dp,
                depthHeight = 4.5.dp,
                floatingLift = 4.5.dp,
                glassGlossAlpha = 0.65f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = Color(0xFFC5D0F6), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recent Activity",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View All →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFB394FF),
                            modifier = Modifier.clickable(onClick = onViewAllActivity)
                        )
                    }

                    val activityEntries = remember(studentAccounts, materials, quizzes, examPapers, announcements) {
                        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)
                        val zoneId = ZoneId.systemDefault()
                        fun formatIsoShort(iso: String?, fallback: String): String {
                            val ms = SupabaseMappers.parseIsoToMillisOrNull(iso) ?: return fallback
                            return Instant.ofEpochMilli(ms).atZone(zoneId).format(formatter)
                        }
                        buildList {
                            studentAccounts.forEach { u ->
                                val iso = u.updatedAt ?: u.createdAt
                                add(
                                    AdminActivityEntry(
                                        title = u.displayName,
                                        subtitle = "Student • Class ${u.selectedClass} • ${if (u.blocked) "Blocked" else "${u.points} pts"}",
                                        icon = Icons.Rounded.Person,
                                        trailingLabel = formatIsoShort(iso, "Registered"),
                                        timestampMillis = SupabaseMappers.parseIsoToMillisOrNull(iso) ?: 0L
                                    )
                                )
                            }
                            announcements.forEach { a ->
                                val iso = a.updatedAt ?: a.createdAt
                                add(
                                    AdminActivityEntry(
                                        title = a.title,
                                        subtitle = "Announcement • ${if (a.targetClass == 0L) "All Classes" else "Class ${a.targetClass}"}",
                                        icon = Icons.Rounded.Campaign,
                                        trailingLabel = formatIsoShort(iso, if (a.published) "Live" else "Draft"),
                                        timestampMillis = SupabaseMappers.parseIsoToMillisOrNull(iso) ?: 0L
                                    )
                                )
                            }
                            materials.forEach { m ->
                                add(
                                    AdminActivityEntry(
                                        title = m.title,
                                        subtitle = "Material • Class ${m.classGrade} • ${m.subjectName}",
                                        icon = Icons.Rounded.AutoStories,
                                        trailingLabel = if (m.published) "Published" else "Draft",
                                        timestampMillis = 0L
                                    )
                                )
                            }
                            quizzes.forEach { q ->
                                add(
                                    AdminActivityEntry(
                                        title = q.title,
                                        subtitle = "Quiz • Class ${q.classGrade} • ${q.questionCount} MCQs",
                                        icon = Icons.Rounded.Quiz,
                                        trailingLabel = if (q.published) "Published" else "Draft",
                                        timestampMillis = 0L
                                    )
                                )
                            }
                            examPapers.forEach { ep ->
                                add(
                                    AdminActivityEntry(
                                        title = ep.title,
                                        subtitle = "Paper • Class ${ep.classGrade} • ${ep.year}",
                                        icon = Icons.Rounded.Description,
                                        trailingLabel = if (ep.published) "Published" else "Draft",
                                        timestampMillis = 0L
                                    )
                                )
                            }
                        }.sortedByDescending { it.timestampMillis }.take(4)
                    }

                    if (activityEntries.isEmpty()) {
                        Text(
                            text = "No recent platform activity yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9AA5D1),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        activityEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF192252).copy(alpha = 0.62f))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    ElectricPurple.copy(alpha = 0.42f),
                                                    ElectricPurple.copy(alpha = 0.20f)
                                                )
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(entry.icon, contentDescription = null, tint = Color(0xFFD2BFFF), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = entry.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9AA5D1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.trailingLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9AA5D1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    icon: ImageVector,
    value: String,
    label: String,
    trend: String,
    colors: List<Color>,
    depthColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(colors.map { it.copy(alpha = 0.94f) }),
        depthColor = depthColor,
        shadowColor = colors.last(),
        cornerRadius = 18.dp,
        depthHeight = 4.5.dp,
        floatingLift = 4.5.dp,
        glassGlossAlpha = 1.1f,
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(64.dp)
                    .alpha(0.15f)
            )
            Column {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trend,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD4FFE7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminQuickActionButton(
    title: String,
    icon: ImageVector,
    colors: List<Color>,
    depthColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(colors.map { it.copy(alpha = 0.94f) }),
        depthColor = depthColor,
        shadowColor = colors.last(),
        cornerRadius = 16.dp,
        depthHeight = 4.dp,
        floatingLift = 3.5.dp,
        glassGlossAlpha = 1.1f,
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun AdminBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple("Dashboard", Icons.Rounded.Dashboard, 0),
        Triple("Classes", Icons.Rounded.School, 1),
        Triple("Analytics", Icons.Rounded.Analytics, 2),
        Triple("Settings", Icons.Rounded.Settings, 3),
        Triple("Support", Icons.Rounded.HeadsetMic, 4)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                ambientColor = Color(0xFF04071A),
                spotColor = Color(0xFF04071A)
            ),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = NavySurface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, icon, idx) ->
                val selected = selectedTab == idx
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp, bottom = 2.dp)
                            .drawBehind {
                                val r = 18.dp.toPx()
                                drawRoundRect(
                                    color = ElectricPurple.copy(alpha = 0.14f),
                                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                    cornerRadius = CornerRadius(r, r)
                                )
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF4712BD), Color(0xFF28067A)))
                            )
                            .clickable { onSelectTab(idx) }
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
                                .padding(horizontal = 13.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
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
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTab(idx) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color(0xFF93A0CC),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93A0CC),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
