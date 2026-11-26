package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.example.data.model.AppSettings
import com.example.data.model.MaterialType
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SyncStatus
import com.example.data.model.UserProfile
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.EthicalAdFreeUpgradeBanner
import com.example.ui.components.StudyHubBrandLogo
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.IndigoHeaderEnd
import com.example.ui.theme.IndigoHeaderStart
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily

@Composable
fun StudentHomeScreen(
    profile: UserProfile,
    subjects: List<SubjectItem>,
    materials: List<StudyMaterial>,
    downloadedIds: Set<String>,
    recentProgress: List<ReadingProgressRecord>,
    announcementCount: Int,
    syncStatus: SyncStatus,
    pendingSyncCount: Int,
    appSettings: AppSettings,
    onSelectClass: (Long) -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenSubject: (SubjectItem) -> Unit,
    onOpenMaterial: (StudyMaterial) -> Unit,
    onOpenQuizzes: () -> Unit,
    onStartDailyChallenge: () -> Unit,
    onOpenExamPapers: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPremium: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = profile.displayName.substringBefore(" ").ifBlank { "Student" }
    val classSubjects = subjects.filter { it.classGrade == profile.selectedClass && it.published }
    val classMaterials = materials.filter { it.classGrade == profile.selectedClass && it.published }
    val continueMaterial = classMaterials.firstOrNull { mat ->
        recentProgress.any { it.materialId == mat.id }
    } ?: classMaterials.firstOrNull()
    val continuePercent = recentProgress.firstOrNull { it.materialId == continueMaterial?.id }?.progressPercent ?: 75L
    val downloadedMaterials = materials.filter { it.id in downloadedIds }
    var classMenuOpen by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenBg = MaterialTheme.colorScheme.background

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header with Deep Navy Top Band + Sweeping Periwinkle Wave + Greeting + Mascot + Continue Learning Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(IndigoHeaderStart, IndigoHeaderEnd, Color(0xFF4752C4))
                        )
                    )
            ) {
                // Sweeping light wave behind greeting & Continue Learning card (matching Screenshot)
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val waveStartY = (118.dp.toPx()).coerceAtMost(h * 0.42f)
                    val path = Path().apply {
                        moveTo(0f, waveStartY + 18.dp.toPx())
                        cubicTo(
                            w * 0.30f, waveStartY - 26.dp.toPx(),
                            w * 0.65f, waveStartY + 14.dp.toPx(),
                            w, waveStartY - 38.dp.toPx()
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    val waveColors = if (isDark) {
                        listOf(Color(0xFF1A2250), Color(0xFF101633))
                    } else {
                        listOf(Color(0xFFD9E3FF), Color(0xFFEEF2FC), screenBg)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = waveColors,
                            startY = waveStartY - 40.dp.toPx(),
                            endY = h
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Top Brand Bar (Logo + Compact Class Pill + Search + Bell with badge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudyHubBrandLogo(
                            subtitle = "Learn. Practice. Achieve.",
                            isDarkSurface = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                val classInteraction = remember { MutableInteractionSource() }
                                val classPressed by classInteraction.collectIsPressedAsState()
                                val classScale by animateFloatAsState(
                                    targetValue = if (classPressed) 0.95f else 1f,
                                    animationSpec = tween(90),
                                    label = "class_pill_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = classScale
                                            scaleY = classScale
                                        }
                                        .drawBehind {
                                            val r = size.height / 2f
                                            val pad = 3.dp.toPx()
                                            drawRoundRect(
                                                color = Color(0xFF090D2E).copy(alpha = if (classPressed) 0.06f else 0.11f),
                                                topLeft = Offset(pad * 0.8f, pad * 0.85f),
                                                size = Size((size.width - pad * 0.7f).coerceAtLeast(1f), (size.height - pad * 0.7f).coerceAtLeast(1f)),
                                                cornerRadius = CornerRadius(r, r)
                                            )
                                        }
                                        .padding(end = 3.dp, bottom = 3.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF34169E), Color(0xFF1D0963)))
                                        )
                                        .clickable(
                                            interactionSource = classInteraction,
                                            indication = androidx.compose.material3.ripple(),
                                            onClick = { classMenuOpen = true }
                                        )
                                        .testTag("home_class_selector")
                                        .padding(end = if (classPressed) 1.dp else 2.dp, bottom = if (classPressed) 1.5.dp else 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF7B4BFF), Color(0xFF4F24DC))
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    listOf(Color.White.copy(alpha = 0.50f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Class ${profile.selectedClass}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Change class",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = classMenuOpen,
                                    onDismissRequest = { classMenuOpen = false }
                                ) {
                                    (6L..12L).forEach { grade ->
                                        DropdownMenuItem(
                                            text = { Text("Class $grade") },
                                            onClick = {
                                                classMenuOpen = false
                                                onSelectClass(grade)
                                            }
                                        )
                                    }
                                }
                            }

                            val searchInteraction = remember { MutableInteractionSource() }
                            val searchPressed by searchInteraction.collectIsPressedAsState()
                            val searchScale by animateFloatAsState(
                                targetValue = if (searchPressed) 0.93f else 1f,
                                animationSpec = tween(90),
                                label = "search_btn_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = searchScale
                                        scaleY = searchScale
                                    }
                                    .shadow(if (searchPressed) 2.dp else 5.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.12f))
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = searchInteraction,
                                        indication = androidx.compose.material3.ripple(),
                                        onClick = onOpenSearch
                                    )
                                    .testTag("home_search_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Global Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box {
                                val bellInteraction = remember { MutableInteractionSource() }
                                val bellPressed by bellInteraction.collectIsPressedAsState()
                                val bellScale by animateFloatAsState(
                                    targetValue = if (bellPressed) 0.93f else 1f,
                                    animationSpec = tween(90),
                                    label = "bell_btn_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .graphicsLayer {
                                            scaleX = bellScale
                                            scaleY = bellScale
                                        }
                                        .shadow(if (bellPressed) 2.dp else 5.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f))
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.12f))
                                            ),
                                            shape = CircleShape
                                        )
                                        .clickable(
                                            interactionSource = bellInteraction,
                                            indication = androidx.compose.material3.ripple(),
                                            onClick = onOpenNotifications
                                        )
                                        .testTag("home_notifications_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (announcementCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                            .shadow(3.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(DangerRed)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = announcementCount.coerceAtMost(9).toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Greeting on Left + Boy Mascot on Right (sitting on the light lavender wave)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .shadow(4.dp, CircleShape, ambientColor = Color(0xFFFFB81C), spotColor = Color(0xFFFFB81C))
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color(0xFFFFE07A), Color(0xFFFFA500))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFF4F2B00),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SyncStatusBadge(
                                    syncStatus = syncStatus,
                                    pendingCount = pendingSyncCount,
                                    onClick = onSyncNow
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hello, $firstName 👋",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = if (isDark) Color.White else Color(0xFF131936)
                            )
                            Text(
                                text = "Keep going! You're doing great!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFD5DCFF) else Color(0xFF3E4668)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    ambientColor = Color(0xFF5D34EB),
                                    spotColor = Color(0xFF5D34EB)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFC9D5FF), Color(0xFF8B9DF2))
                                    )
                                )
                                .border(2.5.dp, Color.White.copy(alpha = 0.92f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_student_avatar),
                                contentDescription = "Student mascot",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue Learning 3D Glassy Hero Card (Layered Lighting + Frosted Book Medallion + Raised Continue Button)
                    if (continueMaterial != null) {
                        Tactile3DCard(
                            faceBrush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF914DFF).copy(alpha = 0.93f),
                                    Color(0xFF5F28E8).copy(alpha = 0.93f),
                                    Color(0xFF391BB8).copy(alpha = 0.95f)
                                )
                            ),
                            depthColor = Color(0xFF210E78).copy(alpha = 0.95f),
                            cornerRadius = 22.dp,
                            depthHeight = 5.dp,
                            shadowElevation = 16.dp,
                            shadowColor = Color(0xFF4720D6),
                            highlightBorderColor = Color.White.copy(alpha = 0.85f),
                            borderEndColor = Color.White.copy(alpha = 0.28f),
                            glassGlossAlpha = 1.15f,
                            floatingLift = 6.dp,
                            onClick = { onOpenMaterial(continueMaterial) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("continue_learning_card")
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    // Soft top-left & top-right ambient lighting
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                                            center = Offset(size.width * 0.14f, size.height * 0.50f),
                                            radius = size.height * 0.75f
                                        ),
                                        center = Offset(size.width * 0.14f, size.height * 0.50f),
                                        radius = size.height * 0.75f
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF4CE8FF).copy(alpha = 0.22f), Color.Transparent),
                                            center = Offset(size.width * 0.88f, size.height * 0.48f),
                                            radius = size.height * 0.72f
                                        ),
                                        center = Offset(size.width * 0.88f, size.height * 0.48f),
                                        radius = size.height * 0.72f
                                    )
                                    // Subtle 4-point sparkle accents around progress ring
                                    listOf(
                                        Offset(size.width * 0.74f, size.height * 0.18f),
                                        Offset(size.width * 0.94f, size.height * 0.16f),
                                        Offset(size.width * 0.93f, size.height * 0.84f)
                                    ).forEach { pt ->
                                        val r = 4.dp.toPx()
                                        drawLine(Color.White.copy(alpha = 0.75f), Offset(pt.x - r, pt.y), Offset(pt.x + r, pt.y), 1.3.dp.toPx())
                                        drawLine(Color.White.copy(alpha = 0.75f), Offset(pt.x, pt.y - r), Offset(pt.x, pt.y + r), 1.3.dp.toPx())
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Concentric 3D Frosted Glass Medallion with Dimensional Book Icon
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f))
                                                )
                                            )
                                            .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .shadow(6.dp, CircleShape, ambientColor = Color(0xFF230E7D), spotColor = Color(0xFF230E7D))
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFB388FF).copy(alpha = 0.92f), Color(0xFF6B38FB).copy(alpha = 0.92f))
                                                    )
                                                )
                                                .border(
                                                    width = 1.2.dp,
                                                    brush = Brush.verticalGradient(
                                                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.20f))
                                                    ),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoStories,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Continue Learning",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Class ${continueMaterial.classGrade} • ${continueMaterial.subjectName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFE3DCFF),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = continueMaterial.chapterTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.96f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        // Raised White 3D Continue Button
                                        val continueInteraction = remember { MutableInteractionSource() }
                                        val continuePressed by continueInteraction.collectIsPressedAsState()
                                        val continueScale by animateFloatAsState(
                                            targetValue = if (continuePressed) 0.96f else 1f,
                                            animationSpec = tween(90),
                                            label = "continue_btn_scale"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = continueScale
                                                    scaleY = continueScale
                                                }
                                                .drawBehind {
                                                    val r = size.height / 2f
                                                    val pad = 3.5.dp.toPx()
                                                    drawRoundRect(
                                                        color = Color(0xFF120647).copy(alpha = if (continuePressed) 0.06f else 0.11f),
                                                        topLeft = Offset(pad * 0.8f, pad * 0.85f),
                                                        size = Size((size.width - pad * 0.7f).coerceAtLeast(1f), (size.height - pad * 0.7f).coerceAtLeast(1f)),
                                                        cornerRadius = CornerRadius(r, r)
                                                    )
                                                }
                                                .padding(end = 3.5.dp, bottom = 3.5.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    Brush.linearGradient(listOf(Color(0xFFD5DDFC), Color(0xFF9EAEEB)))
                                                )
                                                .clickable(
                                                    interactionSource = continueInteraction,
                                                    indication = androidx.compose.material3.ripple(),
                                                    onClick = { onOpenMaterial(continueMaterial) }
                                                )
                                                .padding(end = if (continuePressed) 1.dp else 2.5.dp, bottom = if (continuePressed) 1.5.dp else 3.5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        Brush.verticalGradient(listOf(Color.White, Color(0xFFF2F5FF)))
                                                    )
                                                    .border(1.dp, Color.White, RoundedCornerShape(50))
                                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Continue",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFF28198C),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF28198C),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    CircularProgressRing(
                                        progress = continuePercent / 100f,
                                        label = "${continuePercent}%",
                                        size = 68.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2x2 Floating 3D Quick Action Grid (Materials, Quiz, Exam Papers, Downloads)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentQuickTile(
                        title = "Materials",
                        subtitle = "Notes & PDFs",
                        icon = Icons.Rounded.AutoStories,
                        gradientColors = listOf(Color(0xFF42A8FF), Color(0xFF1A6FF2), Color(0xFF0E49CA)),
                        depthColor = Color(0xFF0A3399),
                        glowTint = Color(0xFFB3E5FC),
                        onClick = onOpenMaterials,
                        modifier = Modifier.weight(1f).testTag("home_materials_tile")
                    )
                    StudentQuickTile(
                        title = "Quiz",
                        subtitle = "Test Your Knowledge",
                        icon = Icons.Rounded.Lightbulb,
                        gradientColors = listOf(Color(0xFF39EC94), Color(0xFF0DC16B), Color(0xFF058F4C)),
                        depthColor = Color(0xFF046636),
                        glowTint = Color(0xFFFFEB3B),
                        onClick = onOpenQuizzes,
                        modifier = Modifier.weight(1f).testTag("home_quiz_tile")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudentQuickTile(
                        title = "Exam Papers",
                        subtitle = "Previous Year",
                        icon = Icons.Rounded.EditNote,
                        gradientColors = listOf(Color(0xFFFFD138), Color(0xFFFF9A14), Color(0xFFE06405)),
                        depthColor = Color(0xFFA84200),
                        glowTint = Color(0xFFFFF59D),
                        onClick = onOpenExamPapers,
                        modifier = Modifier.weight(1f).testTag("home_exam_papers_tile")
                    )
                    StudentQuickTile(
                        title = "Downloads",
                        subtitle = "Study Offline",
                        icon = Icons.Rounded.CloudDownload,
                        gradientColors = listOf(Color(0xFFB569FF), Color(0xFF7B34F2), Color(0xFF4F14C4)),
                        depthColor = Color(0xFF34098C),
                        glowTint = Color(0xFFE1BEE7),
                        onClick = onOpenDownloads,
                        modifier = Modifier.weight(1f).testTag("home_downloads_tile")
                    )
                }
            }
        }

        // Daily Challenge Frosted 3D Glassy Card
        item {
            Tactile3DCard(
                faceBrush = if (isDark) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2B216B).copy(alpha = 0.90f), Color(0xFF472C9E).copy(alpha = 0.88f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFFAEB).copy(alpha = 0.92f), Color(0xFFF4EAFF).copy(alpha = 0.90f), Color(0xFFE3D5FF).copy(alpha = 0.92f))
                    )
                },
                depthColor = if (isDark) Color(0xFF140E3B) else Color(0xFF9F88F5).copy(alpha = 0.92f),
                cornerRadius = 20.dp,
                depthHeight = 4.5.dp,
                shadowElevation = 14.dp,
                shadowColor = Color(0xFF7B3EFF),
                highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.45f else 0.95f),
                borderEndColor = Color.White.copy(alpha = if (isDark) 0.16f else 0.48f),
                glassGlossAlpha = 1.1f,
                floatingLift = 5.dp,
                onClick = onStartDailyChallenge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("daily_challenge_card")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Subtle soft lavender wave accent along the bottom matching screenshot
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val wavePath = Path().apply {
                            moveTo(0f, h * 0.80f)
                            cubicTo(w * 0.35f, h * 0.96f, w * 0.65f, h * 0.68f, w, h * 0.62f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path = wavePath,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFC5A8FF).copy(alpha = 0.25f),
                                    Color(0xFF9C72FF).copy(alpha = 0.32f)
                                )
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color(0xFFFFB81C),
                                    spotColor = Color(0xFFFFB81C)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFFFEBA8), Color(0xFFFFD56B))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_daily_trophy),
                                contentDescription = "Daily Challenge Trophy",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Challenge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF1A1446)
                            )
                            Text(
                                text = "Answer 5 questions today!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFDCD6FF) else Color(0xFF473E78)
                            )
                        }
                        val startInteraction = remember { MutableInteractionSource() }
                        val startPressed by startInteraction.collectIsPressedAsState()
                        val startScale by animateFloatAsState(
                            targetValue = if (startPressed) 0.95f else 1f,
                            animationSpec = tween(90),
                            label = "start_btn_scale"
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = startScale
                                    scaleY = startScale
                                }
                                .drawBehind {
                                    val r = size.height / 2f
                                    val pad = 3.5.dp.toPx()
                                    drawRoundRect(
                                        color = Color(0xFF3E18B5).copy(alpha = if (startPressed) 0.06f else 0.12f),
                                        topLeft = Offset(pad * 0.8f, pad * 0.85f),
                                        size = Size((size.width - pad * 0.7f).coerceAtLeast(1f), (size.height - pad * 0.7f).coerceAtLeast(1f)),
                                        cornerRadius = CornerRadius(r, r)
                                    )
                                }
                                .padding(end = 3.5.dp, bottom = 3.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF4712BD), Color(0xFF28067A)))
                                )
                                .clickable(
                                    interactionSource = startInteraction,
                                    indication = androidx.compose.material3.ripple(),
                                    onClick = onStartDailyChallenge
                                )
                                .padding(end = if (startPressed) 1.dp else 2.5.dp, bottom = if (startPressed) 1.5.dp else 3.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF9648FF), Color(0xFF5D1FE8)))
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subjects Section (Layers Icon + View All Pill + Floating 3D Subject Chips with Icons)
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = Color(0xFF28206E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val viewAllInteraction = remember { MutableInteractionSource() }
                    val viewAllPressed by viewAllInteraction.collectIsPressedAsState()
                    val viewAllScale by animateFloatAsState(
                        targetValue = if (viewAllPressed) 0.95f else 1f,
                        animationSpec = tween(90),
                        label = "view_all_scale"
                    )
                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = viewAllScale
                                scaleY = viewAllScale
                            }
                            .shadow(
                                elevation = if (viewAllPressed) 1.dp else 4.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = ElectricPurple,
                                spotColor = ElectricPurple
                            )
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(50))
                            .clickable(
                                interactionSource = viewAllInteraction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = onOpenMaterials
                            ),
                        shape = RoundedCornerShape(50),
                        color = if (isDark) ElectricPurple.copy(alpha = 0.22f) else Color(0xFFEDE7FF)
                    ) {
                        Text(
                            text = "View All →",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val orderedSubjects = remember(classSubjects) {
                    val priority = mapOf("english" to 0, "mathematics" to 1, "science" to 2, "social science" to 3, "hindi" to 4)
                    classSubjects.sortedBy { priority[it.name.lowercase()] ?: 99 }
                }
                var activeSubjectId by remember(continueMaterial?.subjectId) {
                    mutableStateOf(continueMaterial?.subjectId)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    orderedSubjects.forEach { subj ->
                        val isSelected = subj.id == activeSubjectId
                        val (gradColors, depthCol) = when (subj.colorKey.lowercase()) {
                            "green" -> listOf(Color(0xFF2EE990), Color(0xFF09A65C)) to Color(0xFF046B3B)
                            "orange" -> listOf(Color(0xFFFFCE3B), Color(0xFFF37D06)) to Color(0xFFB34D00)
                            "purple" -> listOf(Color(0xFFFF5895), Color(0xFFC81A5B)) to Color(0xFF8A0B3A)
                            "pink" -> listOf(Color(0xFFA756FF), Color(0xFF631AE6)) to Color(0xFF3E0A9E)
                            else -> listOf(Color(0xFF439DFF), Color(0xFF1755E6)) to Color(0xFF0C3499)
                        }
                        Tactile3DCard(
                            faceBrush = Brush.verticalGradient(
                                gradColors.map { it.copy(alpha = 0.93f) }
                            ),
                            depthColor = depthCol.copy(alpha = 0.94f),
                            cornerRadius = 16.dp,
                            depthHeight = if (isSelected) 4.5.dp else 4.dp,
                            shadowElevation = if (isSelected) 13.dp else 9.dp,
                            shadowColor = depthCol,
                            highlightBorderColor = Color.White.copy(alpha = if (isSelected) 0.96f else 0.80f),
                            borderEndColor = Color.White.copy(alpha = if (isSelected) 0.55f else 0.25f),
                            glassGlossAlpha = 1.1f,
                            floatingLift = if (isSelected) 5.dp else 4.dp,
                            onClick = {
                                activeSubjectId = subj.id
                                onOpenSubject(subj)
                            },
                            modifier = Modifier.width(92.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (subj.name.lowercase()) {
                                        "english" -> Text(
                                            text = "Aa",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        "hindi" -> Text(
                                            text = "अ",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        "mathematics" -> Icon(
                                            imageVector = Icons.Rounded.Calculate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        "science" -> Icon(
                                            imageVector = Icons.Rounded.Science,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = if (subj.name == "Social Science") "SST" else subj.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Learning Goal 3D Glassy Card
        item {
            val goalRatio = (profile.dailyCompletedCount.toFloat() / profile.dailyGoalCount.coerceAtLeast(1L)).coerceIn(0f, 1f)
            Tactile3DCard(
                faceBrush = if (isDark) {
                    Brush.linearGradient(listOf(Color(0xFF212C69).copy(alpha = 0.88f), Color(0xFF151C47).copy(alpha = 0.86f)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEAF0FF).copy(alpha = 0.86f)))
                },
                depthColor = if (isDark) Color(0xFF0D1230) else Color(0xFFB5C4F7).copy(alpha = 0.90f),
                cornerRadius = 20.dp,
                depthHeight = 4.5.dp,
                shadowElevation = 12.dp,
                shadowColor = Color(0xFF4758C9),
                highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.35f else 0.92f),
                borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.40f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                        Brush.linearGradient(listOf(Color(0xFFFFB526), Color(0xFFF56505)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Learning Goal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ElectricPurple.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${profile.dailyCompletedCount}/${profile.dailyGoalCount} tasks",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { goalRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ElectricPurple,
                        trackColor = ElectricPurple.copy(alpha = 0.16f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔥 Streak: ${profile.dayStreak} days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OrangeStart
                        )
                        Text(
                            text = "🎯 Avg Score: ${profile.avgScore}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenStart
                        )
                        Text(
                            text = "⭐ ${profile.points} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Recommended & Recently Downloaded 3D Cards
        if (classMaterials.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Recommended for Class ${profile.selectedClass}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    classMaterials.take(3).forEach { mat ->
                        Tactile3DCard(
                            faceBrush = if (isDark) {
                                Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                            } else {
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                            },
                            depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFBCC9F7).copy(alpha = 0.90f),
                            cornerRadius = 18.dp,
                            depthHeight = 4.dp,
                            shadowElevation = 11.dp,
                            shadowColor = Color(0xFF4C5FD5),
                            highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.35f else 0.90f),
                            borderEndColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.38f),
                            onClick = { onOpenMaterial(mat) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(3.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF4218B8))
                                        .padding(bottom = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFF9353FF), Color(0xFF5D26E8)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoStories,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mat.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${mat.subjectName} • ${MaterialType.fromKey(mat.materialType).label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (mat.id in downloadedIds) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadForOffline,
                                        contentDescription = "Available offline",
                                        tint = GreenStart,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (downloadedMaterials.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        text = "Recently Downloaded (${downloadedMaterials.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        downloadedMaterials.take(5).forEach { mat ->
                            Tactile3DCard(
                                faceBrush = if (isDark) {
                                    Brush.linearGradient(listOf(Color(0xFF183D32).copy(alpha = 0.88f), Color(0xFF0F2922).copy(alpha = 0.86f)))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFE0FAEE).copy(alpha = 0.88f)))
                                },
                                depthColor = if (isDark) Color(0xFF081713) else Color(0xFF88DFB3).copy(alpha = 0.90f),
                                cornerRadius = 18.dp,
                                depthHeight = 4.dp,
                                shadowElevation = 11.dp,
                                shadowColor = GreenStart,
                                highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.35f else 0.90f),
                                borderEndColor = Color.White.copy(alpha = if (isDark) 0.12f else 0.38f),
                                onClick = { onOpenMaterial(mat) },
                                modifier = Modifier.width(194.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = mat.subjectName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreenStart,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mat.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Offline • ${mat.fileSizeLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ethical non-intrusive upgrade / ad-free slot
        item {
            EthicalAdFreeUpgradeBanner(
                adsEnabled = appSettings.adsEnabled && appSettings.adPlacementHome,
                isPremium = profile.subscriptionTier == "premium",
                onUpgradeClick = onOpenPremium,
                title = appSettings.adBannerTitle,
                subtitle = appSettings.adBannerSubtitle,
                ctaLabel = appSettings.adCtaLabel,
                familySafeMode = appSettings.adFamilySafeMode,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun StudentQuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    depthColor: Color,
    glowTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.94f) }),
        depthColor = depthColor.copy(alpha = 0.96f),
        cornerRadius = 20.dp,
        depthHeight = 4.5.dp,
        shadowElevation = 14.dp,
        shadowColor = depthColor,
        highlightBorderColor = Color.White.copy(alpha = 0.86f),
        borderEndColor = Color.White.copy(alpha = 0.28f),
        glassGlossAlpha = 1.15f,
        floatingLift = 5.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 13.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowTint.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.50f, size.height * 0.30f),
                        radius = size.minDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.50f, size.height * 0.30f),
                    radius = size.minDimension * 0.45f
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape, ambientColor = depthColor, spotColor = depthColor)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.16f))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(27.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(26.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.14f))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
