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
import androidx.compose.material.icons.rounded.Storage
import com.example.data.model.SyncStatus

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
    onSaveClass: (SchoolClass, Boolean) -> Unit,
    onDeleteClass: (String) -> Unit,
    onSaveSubject: (SubjectItem, Boolean) -> Unit,
    onSaveChapter: (ChapterItem, Boolean) -> Unit,
    onSaveMaterial: (StudyMaterial, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishMaterial: (StudyMaterial) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onSaveQuizWithQuestion: (QuizItem, QuizQuestion?) -> Unit,
    onAddQuestionToQuiz: (QuizQuestion) -> Unit,
    onSaveExamPaper: (ExamPaper, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishExamPaper: (ExamPaper) -> Unit,
    onDeleteExamPaper: (String) -> Unit,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>>,
    onSendAnnouncement: (String, String, Long, String) -> Unit,
    onSaveAppSettings: (AppSettings) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSeedMissingCloud: () -> Unit,
    onFlushSyncQueue: () -> Unit,
    onResetLocalCache: () -> Unit,
    onSwitchToStudentView: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeModule by remember { mutableStateOf(AdminModule.DASHBOARD) }
    var activeBottomTab by remember { mutableIntStateOf(0) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showUploadMaterialDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<StudyMaterial?>(null) }
    var showAddMcqDialog by remember { mutableStateOf(false) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showAddClassDialog) {
        AdminAddClassDialog(
            onDismiss = { showAddClassDialog = false },
            onSave = { item ->
                onSaveClass(item, true)
                showAddClassDialog = false
            }
        )
    }
    if (showAddSubjectDialog) {
        AdminAddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onSave = { item ->
                onSaveSubject(item, true)
                showAddSubjectDialog = false
            }
        )
    }
    if (showUploadMaterialDialog || editingMaterial != null) {
        val targetMaterial = editingMaterial
        AdminUploadMaterialDialog(
            subjects = subjects,
            chapters = chapters,
            initialMaterial = targetMaterial,
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
    if (showAddMcqDialog) {
        AdminAddMcqDialog(
            quizzes = quizzes,
            onDismiss = { showAddMcqDialog = false },
            onSave = { q ->
                onAddQuestionToQuiz(q)
                showAddMcqDialog = false
            }
        )
    }
    if (showCreateQuizDialog) {
        AdminCreateQuizDialog(
            subjects = subjects,
            chapters = chapters,
            onDismiss = { showCreateQuizDialog = false },
            onSave = { quiz, firstQ ->
                onSaveQuizWithQuestion(quiz, firstQ)
                showCreateQuizDialog = false
            }
        )
    }
    if (showAnnouncementDialog) {
        AdminAnnouncementDialog(
            onDismiss = { showAnnouncementDialog = false },
            onSend = { title, msg, grade, prio ->
                onSendAnnouncement(title, msg, grade, prio)
                showAnnouncementDialog = false
            }
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
                        materials = materials,
                        quizzes = quizzes,
                        announcements = announcements,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenAnnouncements = { activeModule = AdminModule.ANNOUNCEMENTS },
                        onAddClass = { showAddClassDialog = true },
                        onAddSubject = { showAddSubjectDialog = true },
                        onUploadMaterial = { showUploadMaterialDialog = true },
                        onAddMcqs = { showAddMcqDialog = true },
                        onCreateTest = { showCreateQuizDialog = true },
                        onSendAnnouncement = { showAnnouncementDialog = true },
                        onViewAllQuick = { activeModule = AdminModule.MATERIALS },
                        onViewAllActivity = { activeModule = AdminModule.MATERIALS },
                        onSwitchToStudent = onSwitchToStudentView
                    )

                    else -> AdminSubmoduleScaffold(
                        module = activeModule,
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
                        onBackToDashboard = {
                            activeModule = AdminModule.DASHBOARD
                            activeBottomTab = 0
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateModule = { target -> activeModule = target },
                        onOpenAddClass = { showAddClassDialog = true },
                        onDeleteClass = onDeleteClass,
                        onOpenAddSubject = { showAddSubjectDialog = true },
                        onSaveChapter = onSaveChapter,
                        onOpenUploadMaterial = {
                            editingMaterial = null
                            showUploadMaterialDialog = true
                        },
                        onEditMaterial = { mat -> editingMaterial = mat },
                        onTogglePublishMaterial = onTogglePublishMaterial,
                        onDeleteMaterial = onDeleteMaterial,
                        onOpenAddMcq = { showAddMcqDialog = true },
                        onOpenCreateQuiz = { showCreateQuizDialog = true },
                        onSaveExamPaper = onSaveExamPaper,
                        onTogglePublishExamPaper = onTogglePublishExamPaper,
                        onDeleteExamPaper = onDeleteExamPaper,
                        onLoadAttachments = onLoadAttachments,
                        onOpenAnnouncementDialog = { showAnnouncementDialog = true },
                        onSaveAppSettings = onSaveAppSettings,
                        onSyncCloudNow = onSyncCloudNow,
                        onSeedMissingCloud = onSeedMissingCloud,
                        onFlushSyncQueue = onFlushSyncQueue,
                        onResetLocalCache = onResetLocalCache,
                        onSwitchToStudent = onSwitchToStudentView
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardContent(
    profile: UserProfile,
    classes: List<SchoolClass>,
    materials: List<StudyMaterial>,
    quizzes: List<QuizItem>,
    announcements: List<AnnouncementItem>,
    onOpenDrawer: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onAddClass: () -> Unit,
    onAddSubject: () -> Unit,
    onUploadMaterial: () -> Unit,
    onAddMcqs: () -> Unit,
    onCreateTest: () -> Unit,
    onSendAnnouncement: () -> Unit,
    onViewAllQuick: () -> Unit,
    onViewAllActivity: () -> Unit,
    onSwitchToStudent: () -> Unit
) {
    val totalStudents = classes.sumOf { it.studentCount }.coerceAtLeast(12580L)
    var analyticsRange by remember { mutableStateOf("Last 7 Days") }
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
                                text = announcements.size.coerceAtLeast(1).coerceAtMost(9).toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
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

        // 2x2 Vibrant 3D Glassy Floating KPI Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiCard(
                        icon = Icons.Rounded.Group,
                        value = "%,d".format(totalStudents),
                        label = "Total Students",
                        trend = "↑ +8.5%",
                        colors = listOf(BlueStart, BlueEnd),
                        depthColor = Color(0xFF092E8A),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        icon = Icons.Rounded.MenuBook,
                        value = "${materials.size}",
                        label = "Study Materials",
                        trend = "↑ +12.3%",
                        colors = listOf(GreenStart, GreenEnd),
                        depthColor = Color(0xFF045E34),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminKpiCard(
                        icon = Icons.Rounded.Description,
                        value = "${quizzes.size}",
                        label = "Total Quizzes",
                        trend = "↑ +10.1%",
                        colors = listOf(OrangeStart, OrangeEnd),
                        depthColor = Color(0xFF9E3C00),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        icon = Icons.Rounded.Star,
                        value = "4.8 / 5",
                        label = "Average Rating",
                        trend = "↑ +0.2",
                        colors = listOf(PurpleStart, PurpleEnd),
                        depthColor = Color(0xFF33088C),
                        modifier = Modifier.weight(1f)
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
                                listOf("Last 7 Days", "Last 30 Days", "This Term").forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                                        analyticsRange = opt
                                        rangeMenuOpen = false
                                    })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val studentsPts = if (analyticsRange == "Last 7 Days") {
                        listOf(0.45f, 0.62f, 0.72f, 0.60f, 0.76f, 0.85f, 0.96f)
                    } else {
                        listOf(0.35f, 0.48f, 0.58f, 0.70f, 0.78f, 0.88f, 0.98f)
                    }
                    val quizPts = if (analyticsRange == "Last 7 Days") {
                        listOf(0.24f, 0.38f, 0.48f, 0.39f, 0.52f, 0.65f, 0.75f)
                    } else {
                        listOf(0.20f, 0.32f, 0.44f, 0.54f, 0.61f, 0.72f, 0.84f)
                    }
                    PlatformAnalyticsLineChart(
                        studentsSeries = studentsPts,
                        quizzesSeries = quizPts,
                        peakLabel = if (analyticsRange == "Last 7 Days") "12.5K" else "48.2K"
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

                    materials.take(3).forEachIndexed { idx, mat ->
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
                                Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = Color(0xFFD2BFFF), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mat.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Class ${mat.classGrade} • ${mat.subjectName} • ${if (mat.published) "Published" else "Draft"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9AA5D1)
                                )
                            }
                            Text(
                                text = if (idx == 0) "2m ago" else "${(idx + 1) * 14}m ago",
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

@Composable
private fun AdminKpiCard(
    icon: ImageVector,
    value: String,
    label: String,
    trend: String,
    colors: List<Color>,
    depthColor: Color,
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
