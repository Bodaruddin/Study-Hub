package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.ChapterItem
import com.example.data.model.ExamPaper
import com.example.data.model.MaterialType
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.PaperCategory
import com.example.data.model.QuestionType
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SyncStatus
import com.example.data.model.UserProfile
import com.example.data.remote.AttachmentMediaHelper
import com.example.ui.components.AdminAttachmentDraftStrip
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.OrangeEnd
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import kotlinx.coroutines.launch

@Composable
fun AdminSubmoduleScaffold(
    module: AdminModule,
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
    onBackToDashboard: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateModule: (AdminModule) -> Unit,
    onOpenAddClass: () -> Unit,
    onDeleteClass: (String) -> Unit,
    onOpenAddSubject: () -> Unit,
    onSaveChapter: (ChapterItem, Boolean) -> Unit,
    onOpenUploadMaterial: () -> Unit,
    onEditMaterial: (StudyMaterial) -> Unit,
    onTogglePublishMaterial: (StudyMaterial) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onOpenAddMcq: () -> Unit,
    onOpenCreateQuiz: () -> Unit,
    onSaveExamPaper: (ExamPaper, Boolean, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit,
    onTogglePublishExamPaper: (ExamPaper) -> Unit,
    onDeleteExamPaper: (String) -> Unit,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>>,
    onOpenAnnouncementDialog: () -> Unit,
    onSaveAppSettings: (AppSettings) -> Unit,
    onSyncCloudNow: () -> Unit,
    onSeedMissingCloud: () -> Unit,
    onFlushSyncQueue: () -> Unit,
    onResetLocalCache: () -> Unit,
    onSwitchToStudent: () -> Unit
) {
    BackHandler { onBackToDashboard() }

    var showPaperDialog by remember { mutableStateOf(false) }
    var editingPaper by remember { mutableStateOf<ExamPaper?>(null) }

    if (showPaperDialog || editingPaper != null) {
        val target = editingPaper
        AdminExamPaperEditorDialog(
            subjects = subjects,
            initialPaper = target,
            onLoadAttachments = onLoadAttachments,
            onDismiss = {
                showPaperDialog = false
                editingPaper = null
            },
            onSave = { paper, drafts, onDone ->
                onSaveExamPaper(paper, target == null, drafts) { ok ->
                    onDone(ok)
                    if (ok) {
                        showPaperDialog = false
                        editingPaper = null
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("admin_submodule_back")
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
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = module.label,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("admin_submodule_menu")
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
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (module) {
                AdminModule.CLASSES -> {
                    item {
                        AdminActionHeader("Classes (6–12)", "Add Class", onOpenAddClass)
                    }
                    items(classes, key = { it.id }) { c ->
                        AdminDarkRowCard(
                            title = c.name,
                            subtitle = "${c.studentCount} students • ${c.description}",
                            badge = if (c.published) "Published" else "Draft",
                            onDelete = { onDeleteClass(c.id) }
                        )
                    }
                }

                AdminModule.SUBJECTS -> {
                    item {
                        AdminActionHeader("All Subjects (${subjects.size})", "Add Subject", onOpenAddSubject)
                    }
                    items(subjects, key = { it.id }) { s ->
                        AdminDarkRowCard(
                            title = "Class ${s.classGrade} • ${s.name}",
                            subtitle = s.subtitle,
                            badge = if (s.published) "Published" else "Draft"
                        )
                    }
                }

                AdminModule.CHAPTERS -> {
                    item {
                        var showChapDialog by remember { mutableStateOf(false) }
                        if (showChapDialog) {
                            var title by remember { mutableStateOf("") }
                            var summary by remember { mutableStateOf("") }
                            var grade by remember { mutableLongStateOf(7L) }
                            AlertDialog(
                                onDismissRequest = { showChapDialog = false },
                                title = { Text("Add Chapter") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Chapter Title") })
                                        OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") })
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (title.isNotBlank()) {
                                            onSaveChapter(
                                                ChapterItem(
                                                    classGrade = grade,
                                                    subjectId = "sub_english_$grade",
                                                    subjectName = "English",
                                                    chapterNumber = (chapters.size + 1).toLong(),
                                                    title = title.trim(),
                                                    summary = summary.trim(),
                                                    published = true
                                                ),
                                                true
                                            )
                                            showChapDialog = false
                                        }
                                    }) { Text("Save") }
                                },
                                dismissButton = { TextButton(onClick = { showChapDialog = false }) { Text("Cancel") } }
                            )
                        }
                        AdminActionHeader("Chapters (${chapters.size})", "Add Chapter") { showChapDialog = true }
                    }
                    items(chapters, key = { it.id }) { ch ->
                        AdminDarkRowCard(
                            title = "Class ${ch.classGrade} • ${ch.subjectName} — Ch ${ch.chapterNumber}",
                            subtitle = ch.title,
                            badge = if (ch.published) "Published" else "Draft"
                        )
                    }
                }

                AdminModule.MATERIALS -> {
                    item {
                        AdminActionHeader("Study Materials & Publishing (${materials.size})", "Upload Material", onOpenUploadMaterial)
                    }
                    items(materials, key = { it.id }) { m ->
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
                            cornerRadius = 18.dp,
                            depthHeight = 4.dp,
                            floatingLift = 4.dp,
                            glassGlossAlpha = 0.65f,
                            onClick = { onEditMaterial(m) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Class ${m.classGrade} • ${m.subjectName} • ${MaterialType.fromKey(m.materialType).label} • ${m.fileSizeLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9AA5D1)
                                    )
                                }
                                IconButton(
                                    onClick = { onEditMaterial(m) },
                                    modifier = Modifier.testTag("edit_material_${m.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit material",
                                        tint = Color(0xFFC5A6FF)
                                    )
                                }
                                IconButton(onClick = { onTogglePublishMaterial(m) }) {
                                    Icon(
                                        imageVector = if (m.published) Icons.Rounded.Publish else Icons.Rounded.Unpublished,
                                        contentDescription = "Toggle publish",
                                        tint = if (m.published) GreenStart else OrangeStart
                                    )
                                }
                                IconButton(onClick = { onDeleteMaterial(m.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = DangerRed)
                                }
                            }
                        }
                    }
                }

                AdminModule.QUIZZES -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tactile3DCard(
                                faceBrush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                                depthColor = Color(0xFF33088C),
                                shadowColor = PurpleEnd,
                                cornerRadius = 14.dp,
                                depthHeight = 3.5.dp,
                                floatingLift = 3.5.dp,
                                glassGlossAlpha = 1.0f,
                                onClick = onOpenCreateQuiz,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Create Quiz", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Tactile3DCard(
                                faceBrush = Brush.linearGradient(listOf(OrangeStart, OrangeEnd)),
                                depthColor = Color(0xFF9E3C00),
                                shadowColor = OrangeEnd,
                                cornerRadius = 14.dp,
                                depthHeight = 3.5.dp,
                                floatingLift = 3.5.dp,
                                glassGlossAlpha = 1.0f,
                                onClick = onOpenAddMcq,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Add MCQ", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    items(quizzes, key = { it.id }) { q ->
                        val count = questions.count { it.quizId == q.id }
                        AdminDarkRowCard(
                            title = q.title,
                            subtitle = "Class ${q.classGrade} • ${q.subjectName} • $count Questions • ${q.durationMinutes} min",
                            badge = if (q.published) "Live" else "Draft"
                        )
                    }
                }

                AdminModule.PAPERS -> {
                    item {
                        AdminActionHeader("Exam, Model & Sample Papers (${examPapers.size})", "Add Paper") {
                            editingPaper = null
                            showPaperDialog = true
                        }
                    }
                    items(examPapers, key = { it.id }) { ep ->
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
                            cornerRadius = 18.dp,
                            depthHeight = 4.dp,
                            floatingLift = 4.dp,
                            glassGlossAlpha = 0.65f,
                            onClick = { editingPaper = ep },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Class ${ep.classGrade} • ${ep.subjectName} • ${PaperCategory.fromKey(ep.paperCategory).label} • ${ep.year} • ${ep.fileSizeLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9AA5D1)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { editingPaper = ep },
                                    modifier = Modifier.testTag("edit_paper_${ep.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit paper, PDF & images",
                                        tint = Color(0xFFC5A6FF)
                                    )
                                }
                                IconButton(
                                    onClick = { onTogglePublishExamPaper(ep) },
                                    modifier = Modifier.testTag("publish_paper_${ep.id}")
                                ) {
                                    Icon(
                                        imageVector = if (ep.published) Icons.Rounded.Publish else Icons.Rounded.Unpublished,
                                        contentDescription = "Toggle publish",
                                        tint = if (ep.published) GreenStart else OrangeStart
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteExamPaper(ep.id) },
                                    modifier = Modifier.testTag("delete_paper_${ep.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete paper",
                                        tint = DangerRed
                                    )
                                }
                            }
                        }
                    }
                }

                AdminModule.STUDENTS -> {
                    item {
                        Text(
                            text = "Registered Students & Users (${adminUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(adminUsers, key = { it.userId }) { u ->
                        AdminDarkRowCard(
                            title = u.displayName,
                            subtitle = "${u.email} • Class ${u.selectedClass} • ${u.points} pts • ${u.avgScore}% avg",
                            badge = u.role.uppercase()
                        )
                    }
                }

                AdminModule.ANNOUNCEMENTS -> {
                    item {
                        AdminActionHeader("Announcements (${announcements.size})", "Broadcast", onOpenAnnouncementDialog)
                    }
                    items(announcements, key = { it.id }) { a ->
                        AdminDarkRowCard(
                            title = a.title,
                            subtitle = a.message,
                            badge = if (a.priority == "high") "URGENT" else "NORMAL"
                        )
                    }
                }

                AdminModule.ANALYTICS -> {
                    item {
                        AdminDarkRowCard(
                            title = "Curriculum Coverage",
                            subtitle = "${classes.size} Classes • ${subjects.size} Subjects • ${chapters.size} Chapters • ${materials.size} Materials",
                            badge = "ACTIVE"
                        )
                    }
                    item {
                        AdminDarkRowCard(
                            title = "Assessment Bank",
                            subtitle = "${quizzes.size} Quizzes • ${questions.size} Questions • ${examPapers.size} Exam Papers",
                            badge = "VERIFIED"
                        )
                    }
                }

                AdminModule.DATABASE -> adminManageDatabaseItems(
                    classes = classes,
                    subjects = subjects,
                    chapters = chapters,
                    materials = materials,
                    quizzes = quizzes,
                    questions = questions,
                    examPapers = examPapers,
                    announcements = announcements,
                    adminUsers = adminUsers,
                    syncStatus = syncStatus,
                    pendingSyncCount = pendingSyncCount,
                    isDatabaseBusy = isDatabaseBusy,
                    onSyncCloudNow = onSyncCloudNow,
                    onSeedMissingCloud = onSeedMissingCloud,
                    onFlushSyncQueue = onFlushSyncQueue,
                    onResetLocalCache = onResetLocalCache
                )

                AdminModule.ADS -> adminManageAdsItems(
                    appSettings = appSettings,
                    onSaveAppSettings = onSaveAppSettings
                )

                AdminModule.SETTINGS -> {
                    item {
                        var monthly by remember(appSettings) { mutableStateOf(appSettings.premiumMonthlyPrice) }
                        var yearly by remember(appSettings) { mutableStateOf(appSettings.premiumYearlyPrice) }
                        var ads by remember(appSettings) { mutableStateOf(appSettings.adsEnabled) }
                        var maint by remember(appSettings) { mutableStateOf(appSettings.maintenanceMode) }
                        var email by remember(appSettings) { mutableStateOf(appSettings.supportEmail) }

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
                            cornerRadius = 20.dp,
                            depthHeight = 4.5.dp,
                            floatingLift = 4.5.dp,
                            glassGlossAlpha = 0.65f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Monetization & Platform Settings", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Premium Monthly Price") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = yearly, onValueChange = { yearly = it }, label = { Text("Premium Yearly Price") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Family-Safe Ads Banner", color = Color.White)
                                    Switch(checked = ads, onCheckedChange = { ads = it })
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Maintenance Mode", color = Color.White)
                                    Switch(checked = maint, onCheckedChange = { maint = it })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateModule(AdminModule.ADS) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Manage Ads →", color = Color(0xFFC5A6FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(
                                        onClick = { onNavigateModule(AdminModule.DATABASE) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Manage DB →", color = Color(0xFFC5A6FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Tactile3DCard(
                                    faceBrush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                                    depthColor = Color(0xFF33088C),
                                    shadowColor = PurpleEnd,
                                    cornerRadius = 14.dp,
                                    depthHeight = 3.5.dp,
                                    floatingLift = 3.5.dp,
                                    glassGlossAlpha = 1.0f,
                                    onClick = {
                                        onSaveAppSettings(
                                            appSettings.copy(
                                                premiumMonthlyPrice = monthly,
                                                premiumYearlyPrice = yearly,
                                                adsEnabled = ads,
                                                maintenanceMode = maint,
                                                supportEmail = email
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 13.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Save Global Settings", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                AdminModule.SUPPORT -> {
                    item {
                        AdminDarkRowCard(
                            title = "Content Moderation & Reports",
                            subtitle = "0 open student reports • All curriculum materials adhere to age-appropriate educational standards.",
                            badge = "CLEAN"
                        )
                    }
                    item {
                        Tactile3DCard(
                            faceBrush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
                            depthColor = Color(0xFF33088C),
                            shadowColor = PurpleEnd,
                            cornerRadius = 14.dp,
                            depthHeight = 3.5.dp,
                            floatingLift = 3.5.dp,
                            glassGlossAlpha = 1.0f,
                            onClick = onSwitchToStudent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Preview Student App Experience", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                AdminModule.DASHBOARD -> Unit
            }
        }
    }
}

@Composable
private fun AdminActionHeader(title: String, buttonText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Tactile3DCard(
            faceBrush = Brush.linearGradient(listOf(PurpleStart, PurpleEnd)),
            depthColor = Color(0xFF33088C),
            shadowColor = PurpleEnd,
            cornerRadius = 14.dp,
            depthHeight = 3.5.dp,
            floatingLift = 3.dp,
            glassGlossAlpha = 1.0f,
            fillMaxWidth = false,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun AdminDarkRowCard(
    title: String,
    subtitle: String,
    badge: String,
    onDelete: (() -> Unit)? = null
) {
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
        cornerRadius = 18.dp,
        depthHeight = 4.dp,
        floatingLift = 4.dp,
        glassGlossAlpha = 0.65f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA5D1))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF28336D), NavyCard)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC5A6FF),
                    fontWeight = FontWeight.Bold
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = DangerRed)
                }
            }
        }
    }
}

@Composable
fun AdminAddClassDialog(onDismiss: () -> Unit, onSave: (SchoolClass) -> Unit) {
    var gradeText by remember { mutableStateOf("7") }
    var name by remember { mutableStateOf("Class 7") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add / Update Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = gradeText, onValueChange = { gradeText = it }, label = { Text("Grade (6–12)") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Class Name") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val grade = (gradeText.toLongOrNull() ?: 7L).coerceIn(6L, 12L)
                onSave(SchoolClass(id = "class_$grade", grade = grade, name = name.ifBlank { "Class $grade" }, description = desc, studentCount = 1200L, published = true))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminAddSubjectDialog(onDismiss: () -> Unit, onSave: (SubjectItem) -> Unit) {
    var gradeText by remember { mutableStateOf("7") }
    var name by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("Notes • Summary • MCQs") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = gradeText, onValueChange = { gradeText = it }, label = { Text("Class Grade (6–12)") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") })
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val grade = (gradeText.toLongOrNull() ?: 7L).coerceIn(6L, 12L)
                    onSave(SubjectItem(classGrade = grade, name = name.trim(), subtitle = subtitle.trim(), iconKey = "book", colorKey = "blue", chapterCount = 4L, published = true))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminExamPaperEditorDialog(
    subjects: List<SubjectItem>,
    initialPaper: ExamPaper?,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>>,
    onDismiss: () -> Unit,
    onSave: (ExamPaper, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedGrade by remember(initialPaper?.id) { mutableLongStateOf(initialPaper?.classGrade ?: 7L) }
    var selectedCategory by remember(initialPaper?.id) {
        mutableStateOf(
            if (initialPaper != null) PaperCategory.fromKey(initialPaper.paperCategory)
            else PaperCategory.MODEL_PAPER
        )
    }
    var subjectName by remember(initialPaper?.id) {
        mutableStateOf(initialPaper?.subjectName?.ifBlank { "Mathematics" } ?: "Mathematics")
    }
    var yearText by remember(initialPaper?.id) {
        mutableStateOf((initialPaper?.year ?: 2026L).toString())
    }
    var title by remember(initialPaper?.id) { mutableStateOf(initialPaper?.title ?: "") }
    var content by remember(initialPaper?.id) { mutableStateOf(initialPaper?.contentText ?: "") }
    var publishNow by remember(initialPaper?.id) { mutableStateOf(initialPaper?.published ?: true) }
    var drafts by remember(initialPaper?.id) { mutableStateOf<List<PaperAttachmentDraft>>(emptyList()) }
    var loadingAttachments by remember(initialPaper?.id) { mutableStateOf(initialPaper != null) }
    var isProcessingFile by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialPaper?.id) {
        if (initialPaper != null) {
            loadingAttachments = true
            onLoadAttachments(initialPaper.id).onSuccess { list ->
                drafts = list.map { att ->
                    PaperAttachmentDraft(
                        groupId = att.groupId,
                        kind = att.kind,
                        fileName = att.fileName,
                        mimeType = att.mimeType,
                        sizeBytes = att.sizeBytes,
                        previewFile = att.localFile,
                        isExistingRemote = true
                    )
                }
            }
            loadingAttachments = false
        }
    }

    // Zero-permission Photo Picker & SAF PDF Picker (Google Play Policy compliant)
    val multiImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingFile = true
            errorMessage = null
            val added = ArrayList<PaperAttachmentDraft>()
            uris.forEachIndexed { idx, uri ->
                AttachmentMediaHelper.prepareImageDraft(context, uri, drafts.size + idx)
                    .onSuccess { added.add(it) }
                    .onFailure { errorMessage = it.message ?: "Failed to read selected image." }
            }
            if (added.isNotEmpty()) {
                drafts = drafts + added
            }
            isProcessingFile = false
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingFile = true
            errorMessage = null
            AttachmentMediaHelper.preparePdfDraft(context, uri)
                .onSuccess { pdfDraft ->
                    drafts = drafts + pdfDraft
                }
                .onFailure { err ->
                    errorMessage = err.message ?: "Failed to read selected PDF."
                }
            isProcessingFile = false
        }
    }

    val gradeSubjects = remember(subjects, selectedGrade) {
        subjects.filter { it.classGrade == selectedGrade }.map { it.name }.distinct()
            .ifEmpty { listOf("Mathematics", "Science", "English", "Social Science", "Hindi") }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (initialPaper == null) "Add Question / Model Paper" else "Update Question Paper",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Class", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (6L..12L).forEach { g ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedGrade = g }
                                .testTag("paper_class_$g"),
                            color = if (selectedGrade == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Class $g",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (selectedGrade == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaperCategory.entries.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("paper_cat_${cat.key}"),
                            color = if (selectedCategory == cat) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cat.label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (selectedCategory == cat) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gradeSubjects.forEach { subj ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subjectName = subj },
                            color = if (subjectName.equals(subj, ignoreCase = true)) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = subj,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = if (subjectName.equals(subj, ignoreCase = true)) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Paper Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paper_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Year") },
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Paper Blueprint / Questions (Optional if PDF/Images attached)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paper_content_input")
                )

                Text(
                    text = "PDF & Question Paper Images",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                        enabled = !isProcessingFile && !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paper_attach_pdf_button")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attach PDF", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            multiImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingFile && !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paper_attach_images_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Images", maxLines = 1)
                    }
                }

                if (loadingAttachments || isProcessingFile) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (loadingAttachments) "Loading existing attachments..." else "Preparing media...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                AdminAttachmentDraftStrip(
                    drafts = drafts,
                    onRemoveDraft = { groupId ->
                        drafts = drafts.filterNot { it.groupId == groupId }
                    }
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish Immediately")
                    Switch(checked = publishNow, onCheckedChange = { publishNow = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && !isProcessingFile && title.isNotBlank(),
                modifier = Modifier.testTag("save_paper_button"),
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isEmpty()) return@TextButton
                    val cleanSubject = subjectName.trim().ifBlank { "Mathematics" }
                    val matchedSubjectId = subjects.firstOrNull {
                        it.classGrade == selectedGrade && it.name.equals(cleanSubject, ignoreCase = true)
                    }?.id ?: "sub_${cleanSubject.lowercase().replace(" ", "_")}_$selectedGrade"
                    val parsedYear = (yearText.toLongOrNull() ?: 2026L).coerceIn(2000L, 2035L)
                    val summaryContent = content.trim().ifBlank {
                        if (drafts.isNotEmpty()) {
                            "Attached files: ${drafts.joinToString(", ") { it.fileName }}"
                        } else {
                            "Full model paper questions & marking scheme."
                        }
                    }
                    isSaving = true
                    errorMessage = null
                    val paperToSave = (initialPaper ?: ExamPaper()).copy(
                        classGrade = selectedGrade,
                        subjectId = matchedSubjectId,
                        subjectName = cleanSubject,
                        title = cleanTitle,
                        paperCategory = selectedCategory.key,
                        year = parsedYear,
                        contentText = summaryContent,
                        published = publishNow
                    )
                    onSave(paperToSave, drafts) { ok ->
                        isSaving = false
                        if (!ok) {
                            errorMessage = "Upload failed. Check connection or file size and retry."
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...")
                } else {
                    Text(if (initialPaper == null) "Publish" else "Update")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminUploadMaterialDialog(
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    initialMaterial: StudyMaterial? = null,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>> = { Result.success(emptyList()) },
    onDismiss: () -> Unit,
    onSave: (StudyMaterial, List<PaperAttachmentDraft>, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedGrade by remember(initialMaterial?.id) { mutableLongStateOf(initialMaterial?.classGrade ?: 7L) }
    var title by remember(initialMaterial?.id) { mutableStateOf(initialMaterial?.title ?: "") }
    var desc by remember(initialMaterial?.id) { mutableStateOf(initialMaterial?.description ?: "") }
    var content by remember(initialMaterial?.id) { mutableStateOf(initialMaterial?.contentMarkdown ?: "") }
    var tags by remember(initialMaterial?.id) {
        mutableStateOf(
            initialMaterial?.tags?.joinToString(", ")?.ifBlank { "notes, revision" } ?: "notes, revision"
        )
    }
    var selectedType by remember(initialMaterial?.id) {
        mutableStateOf(
            if (initialMaterial != null) MaterialType.fromKey(initialMaterial.materialType)
            else MaterialType.SHORT_NOTES
        )
    }
    var important by remember(initialMaterial?.id) { mutableStateOf(initialMaterial?.important ?: false) }
    var publishNow by remember(initialMaterial?.id) { mutableStateOf(initialMaterial?.published ?: true) }
    var drafts by remember(initialMaterial?.id) { mutableStateOf<List<PaperAttachmentDraft>>(emptyList()) }
    var loadingAttachments by remember(initialMaterial?.id) { mutableStateOf(initialMaterial != null) }
    var isProcessingFile by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialMaterial?.id) {
        if (initialMaterial != null) {
            loadingAttachments = true
            onLoadAttachments(initialMaterial.id).onSuccess { list ->
                drafts = list.map { att ->
                    PaperAttachmentDraft(
                        groupId = att.groupId,
                        kind = att.kind,
                        fileName = att.fileName,
                        mimeType = att.mimeType,
                        sizeBytes = att.sizeBytes,
                        previewFile = att.localFile,
                        isExistingRemote = true
                    )
                }
            }
            loadingAttachments = false
        }
    }

    // Zero-permission Photo Picker & SAF PDF Picker (Google Play Policy compliant)
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingFile = true
            errorMessage = null
            val added = ArrayList<PaperAttachmentDraft>()
            uris.forEachIndexed { idx, uri ->
                AttachmentMediaHelper.prepareImageDraft(context, uri, drafts.size + idx)
                    .onSuccess { added.add(it) }
                    .onFailure { errorMessage = it.message ?: "Failed to read image." }
            }
            if (added.isNotEmpty()) drafts = drafts + added
            isProcessingFile = false
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingFile = true
            errorMessage = null
            AttachmentMediaHelper.preparePdfDraft(context, uri)
                .onSuccess { drafts = drafts + it }
                .onFailure { errorMessage = it.message ?: "Failed to read PDF." }
            isProcessingFile = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = if (initialMaterial == null) "Upload Study Material" else "Update Study Material",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (6L..12L).forEach { g ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedGrade = g },
                            color = if (selectedGrade == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Class $g", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (selectedGrade == g) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MaterialType.entries.take(6).forEach { mt ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedType = mt },
                            color = if (selectedType == mt) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(mt.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (selectedType == mt) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Material Title") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Short Description") })
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Lesson / Notes Content") }, minLines = 3)
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                        enabled = !isProcessingFile && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Attach PDF", maxLines = 1) }
                    OutlinedButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isProcessingFile && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Add Images", maxLines = 1) }
                }
                if (loadingAttachments || isProcessingFile) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(if (loadingAttachments) "Loading attachments..." else "Preparing media...", style = MaterialTheme.typography.labelSmall)
                    }
                }
                AdminAttachmentDraftStrip(
                    drafts = drafts,
                    onRemoveDraft = { groupId -> drafts = drafts.filterNot { it.groupId == groupId } }
                )
                if (errorMessage != null) {
                    Text(text = errorMessage!!, style = MaterialTheme.typography.labelSmall, color = DangerRed)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Mark as Important")
                    Switch(checked = important, onCheckedChange = { important = it })
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Publish Immediately")
                    Switch(checked = publishNow, onCheckedChange = { publishNow = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && !isProcessingFile && title.isNotBlank(),
                onClick = {
                    if (title.isNotBlank()) {
                        val subj = subjects.firstOrNull { it.classGrade == selectedGrade }
                        val chap = chapters.firstOrNull { it.classGrade == selectedGrade }
                        isSaving = true
                        errorMessage = null
                        val mat = (initialMaterial ?: StudyMaterial()).copy(
                            classGrade = selectedGrade,
                            subjectId = initialMaterial?.subjectId?.takeIf { it.isNotBlank() && initialMaterial.classGrade == selectedGrade }
                                ?: (subj?.id ?: "sub_english_$selectedGrade"),
                            subjectName = initialMaterial?.subjectName?.takeIf { it.isNotBlank() && initialMaterial.classGrade == selectedGrade }
                                ?: (subj?.name ?: "English"),
                            chapterId = initialMaterial?.chapterId?.takeIf { it.isNotBlank() && initialMaterial.classGrade == selectedGrade }
                                ?: (chap?.id ?: "chap_eng_${selectedGrade}_1"),
                            chapterTitle = initialMaterial?.chapterTitle?.takeIf { it.isNotBlank() && initialMaterial.classGrade == selectedGrade }
                                ?: (chap?.title ?: "Chapter 1"),
                            title = title.trim(),
                            description = desc.trim().ifBlank { "Educational study guide" },
                            materialType = selectedType.key,
                            contentMarkdown = content.trim().ifBlank { desc.trim().ifBlank { title.trim() } },
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            important = important,
                            premiumOnly = initialMaterial?.premiumOnly ?: false,
                            published = publishNow
                        )
                        onSave(mat, drafts) { ok ->
                            isSaving = false
                            if (!ok) errorMessage = "Failed to save study material."
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...")
                } else {
                    Text(if (initialMaterial == null) (if (publishNow) "Publish" else "Save Draft") else "Update")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminAddMcqDialog(
    quizzes: List<QuizItem>,
    onDismiss: () -> Unit,
    onSave: (QuizQuestion) -> Unit
) {
    var selectedQuizId by remember { mutableStateOf(quizzes.firstOrNull()?.id ?: "quiz_eng_7_tenses") }
    var prompt by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableStateOf("0") }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MCQ Question") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quizzes.forEach { q ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedQuizId = q.id },
                            color = if (selectedQuizId == q.id) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(q.title, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (selectedQuizId == q.id) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Question Prompt") })
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option 1 (Index 0)") })
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option 2 (Index 1)") })
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option 3 (Index 2)") })
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option 4 (Index 3)") })
                OutlinedTextField(value = correctIdx, onValueChange = { correctIdx = it }, label = { Text("Correct Index (0–3)") })
                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val opts = listOf(optA, optB, optC, optD).filter { it.isNotBlank() }
                if (prompt.isNotBlank() && opts.size >= 2) {
                    val idx = (correctIdx.toLongOrNull() ?: 0L).coerceIn(0L, (opts.size - 1).toLong())
                    onSave(
                        QuizQuestion(
                            quizId = selectedQuizId,
                            questionType = QuestionType.MCQ.key,
                            prompt = prompt.trim(),
                            options = opts,
                            correctIndices = listOf(idx),
                            correctText = "",
                            explanation = explanation.ifBlank { "Correct option is #${idx + 1}." },
                            points = 20L,
                            published = true
                        )
                    )
                }
            }) { Text("Add MCQ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminCreateQuizDialog(
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    onDismiss: () -> Unit,
    onSave: (QuizItem, QuizQuestion?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("10") }
    var firstPrompt by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("Option A") }
    var opt2 by remember { mutableStateOf("Option B") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Practice Test") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Test Title") })
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (minutes)") })
                OutlinedTextField(value = firstPrompt, onValueChange = { firstPrompt = it }, label = { Text("First Question (Optional)") })
                OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Choice 1 (Correct)") })
                OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Choice 2") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    val subj = subjects.firstOrNull { it.classGrade == 7L }
                    val chap = chapters.firstOrNull { it.classGrade == 7L }
                    val quiz = QuizItem(
                        classGrade = 7L,
                        subjectId = subj?.id ?: "sub_english_7",
                        subjectName = subj?.name ?: "English",
                        chapterId = chap?.id ?: "chap_eng_7_1",
                        chapterTitle = chap?.title ?: "Chapter 1",
                        title = title.trim(),
                        description = "Admin-created assessment",
                        durationMinutes = (duration.toLongOrNull() ?: 10L).coerceIn(1L, 180L),
                        questionCount = if (firstPrompt.isNotBlank()) 1L else 0L,
                        premiumOnly = false,
                        published = true
                    )
                    val q = if (firstPrompt.isNotBlank()) {
                        QuizQuestion(
                            quizId = "",
                            questionType = QuestionType.MCQ.key,
                            prompt = firstPrompt.trim(),
                            options = listOf(opt1, opt2),
                            correctIndices = listOf(0L),
                            correctText = "",
                            explanation = "Choice 1 is the correct answer.",
                            points = 20L,
                            published = true
                        )
                    } else null
                    onSave(quiz, q)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminAnnouncementDialog(
    onDismiss: () -> Unit,
    onSend: (String, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var highPriority by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Announcement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, minLines = 3)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("High Priority Alert")
                    Switch(checked = highPriority, onCheckedChange = { highPriority = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && message.isNotBlank()) {
                    onSend(title.trim(), message.trim(), 0L, if (highPriority) "high" else "normal")
                }
            }) { Text("Broadcast") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
