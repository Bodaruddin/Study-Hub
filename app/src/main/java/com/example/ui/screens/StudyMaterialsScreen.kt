package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.ChapterItem
import com.example.data.model.MaterialType
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.ui.components.EthicalAdFreeUpgradeBanner
import com.example.ui.components.SubjectIconBadge
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueStart
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily

@Composable
fun StudyMaterialsScreen(
    selectedClass: Long,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    materials: List<StudyMaterial>,
    bookmarkedMaterialIds: Set<String>,
    downloadedMaterialIds: Set<String>,
    isPremiumUser: Boolean,
    initialSubject: SubjectItem? = null,
    initialMaterial: StudyMaterial? = null,
    onSelectClass: (Long) -> Unit,
    onToggleBookmark: (StudyMaterial) -> Unit,
    onToggleDownload: (StudyMaterial) -> Unit,
    onMarkRead: (StudyMaterial) -> Unit,
    onOpenPremium: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    appSettings: AppSettings = AppSettings()
) {
    var activeSubject by remember(initialSubject) { mutableStateOf(initialSubject) }
    var activeChapter by remember { mutableStateOf<ChapterItem?>(null) }
    var activeQuickType by remember { mutableStateOf<MaterialType?>(null) }
    var readingMaterial by remember(initialMaterial) { mutableStateOf(initialMaterial) }
    var searchQuery by remember { mutableStateOf("") }
    var classDropdownOpen by remember { mutableStateOf(false) }
    var subjectDropdownOpen by remember { mutableStateOf(false) }

    if (readingMaterial != null) {
        BackHandler { readingMaterial = null }
        MaterialReaderView(
            material = readingMaterial!!,
            isBookmarked = readingMaterial!!.id in bookmarkedMaterialIds,
            isDownloaded = readingMaterial!!.id in downloadedMaterialIds,
            isPremiumUser = isPremiumUser,
            onToggleBookmark = { onToggleBookmark(readingMaterial!!) },
            onToggleDownload = { onToggleDownload(readingMaterial!!) },
            onMarkComplete = {
                onMarkRead(readingMaterial!!)
                readingMaterial = null
            },
            onOpenPremium = onOpenPremium,
            onBack = { readingMaterial = null }
        )
        return
    }

    if (activeChapter != null || activeQuickType != null) {
        BackHandler {
            activeChapter = null
            activeQuickType = null
        }
    } else if (activeSubject != null) {
        BackHandler { activeSubject = null }
    }

    val classSubjects = subjects.filter { it.classGrade == selectedClass && it.published }
    val filteredMaterials = materials.filter { m ->
        m.classGrade == selectedClass &&
            m.published &&
            (activeSubject == null || m.subjectId == activeSubject!!.id) &&
            (activeChapter == null || m.chapterId == activeChapter!!.id) &&
            (activeQuickType == null || m.materialType == activeQuickType!!.key) &&
            (searchQuery.isBlank() || m.title.contains(searchQuery, true) || m.description.contains(searchQuery, true))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header matching Screenshot 2 Top-Center
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                when {
                    activeChapter != null -> activeChapter = null
                    activeQuickType != null -> activeQuickType = null
                    activeSubject != null -> activeSubject = null
                    else -> onBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = when {
                    activeChapter != null -> activeChapter!!.title
                    activeQuickType != null -> activeQuickType!!.label
                    activeSubject != null -> activeSubject!!.name
                    else -> "Study Materials"
                },
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onOpenGlobalSearch,
                modifier = Modifier.testTag("materials_search_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search materials"
                )
            }
        }

        // Filter Pills Row: Class X v  &  All Subjects v
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .clickable { classDropdownOpen = true }
                        .testTag("materials_class_filter"),
                    shape = RoundedCornerShape(50),
                    color = ElectricPurple
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Class $selectedClass",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Select class",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = classDropdownOpen,
                    onDismissRequest = { classDropdownOpen = false }
                ) {
                    (6L..12L).forEach { grade ->
                        DropdownMenuItem(
                            text = { Text("Class $grade") },
                            onClick = {
                                classDropdownOpen = false
                                activeSubject = null
                                activeChapter = null
                                onSelectClass(grade)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        .clickable { subjectDropdownOpen = true }
                        .testTag("materials_subject_filter"),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeSubject?.name ?: "All Subjects",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Select subject",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = subjectDropdownOpen,
                    onDismissRequest = { subjectDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Subjects") },
                        onClick = {
                            subjectDropdownOpen = false
                            activeSubject = null
                            activeChapter = null
                        }
                    )
                    classSubjects.forEach { subj ->
                        DropdownMenuItem(
                            text = { Text(subj.name) },
                            onClick = {
                                subjectDropdownOpen = false
                                activeSubject = subj
                                activeChapter = null
                            }
                        )
                    }
                }
            }
        }

        when {
            // Chapter Materials / Quick Access Material List
            activeChapter != null || activeQuickType != null -> {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter notes, summaries, Q&A...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Material Type Quick Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeQuickType == null,
                        onClick = { activeQuickType = null },
                        label = { Text("All Types") }
                    )
                    MaterialType.entries.forEach { type ->
                        FilterChip(
                            selected = activeQuickType == type,
                            onClick = { activeQuickType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredMaterials.isEmpty()) {
                        item {
                            EmptyMaterialsState("No study materials found for this filter yet.")
                        }
                    } else {
                        items(filteredMaterials, key = { it.id }) { mat ->
                            MaterialRowCard(
                                material = mat,
                                isBookmarked = mat.id in bookmarkedMaterialIds,
                                isDownloaded = mat.id in downloadedMaterialIds,
                                onClick = { readingMaterial = mat },
                                onToggleBookmark = { onToggleBookmark(mat) },
                                onToggleDownload = { onToggleDownload(mat) }
                            )
                        }
                    }
                }
            }

            // Subject Selected -> Show Chapters + Subject Materials
            activeSubject != null -> {
                val subjectChapters = chapters.filter {
                    it.classGrade == selectedClass && it.subjectId == activeSubject!!.id && it.published
                }.sortedBy { it.chapterNumber }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Chapters (${subjectChapters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(subjectChapters, key = { it.id }) { chap ->
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        Tactile3DCard(
                            faceBrush = if (isDark) {
                                Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                            } else {
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                            },
                            depthColor = if (isDark) Color(0xFF0B0F28) else Color(0xFFCED8FA).copy(alpha = 0.85f),
                            cornerRadius = 18.dp,
                            depthHeight = 4.5.dp,
                            shadowElevation = 7.dp,
                            shadowColor = Color(0xFF4758C9),
                            highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                            borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                            onClick = { activeChapter = chap },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(3.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF3E10A6))
                                        .padding(bottom = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFF8C48FF), Color(0xFF5D24E6)))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${chap.chapterNumber}",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chap.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = chap.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Open chapter"
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All ${activeSubject!!.name} Materials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredMaterials, key = { it.id }) { mat ->
                        MaterialRowCard(
                            material = mat,
                            isBookmarked = mat.id in bookmarkedMaterialIds,
                            isDownloaded = mat.id in downloadedMaterialIds,
                            onClick = { readingMaterial = mat },
                            onToggleBookmark = { onToggleBookmark(mat) },
                            onToggleDownload = { onToggleDownload(mat) }
                        )
                    }
                }
            }

            // Default Study Materials Root (Matching Screenshot 2 Top-Center!)
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(classSubjects, key = { it.id }) { subj ->
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val (tintEnd, depthCol) = when (subj.colorKey.lowercase()) {
                            "green" -> Color(0xFFE7FFF2) to Color(0xFF9EE8C2)
                            "orange" -> Color(0xFFFFF2DF) to Color(0xFFFFCA8C)
                            "purple" -> Color(0xFFF3EBFF) to Color(0xFFCBB6FF)
                            "pink" -> Color(0xFFFFEBF2) to Color(0xFFFFB5D0)
                            else -> Color(0xFFEAF1FF) to Color(0xFFB5CCFF)
                        }
                        Tactile3DCard(
                            faceBrush = if (isDark) {
                                Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                            } else {
                                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.90f), tintEnd.copy(alpha = 0.88f)))
                            },
                            depthColor = if (isDark) Color(0xFF0C102B) else depthCol.copy(alpha = 0.85f),
                            cornerRadius = 20.dp,
                            depthHeight = 4.5.dp,
                            shadowElevation = 8.dp,
                            shadowColor = if (isDark) Color(0xFF070A1C) else depthCol,
                            highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                            borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.42f),
                            onClick = { activeSubject = subj },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_card_${subj.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubjectIconBadge(
                                    iconKey = subj.iconKey,
                                    colorKey = subj.colorKey,
                                    size = 50.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subj.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = subj.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Open ${subj.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Quick Access Section (4 Vibrant 3D Square Tiles)
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Quick Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "View All →",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { activeQuickType = MaterialType.SHORT_NOTES }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAccessSquareTile(
                                title = "Short\nNotes",
                                icon = Icons.Rounded.NoteAlt,
                                gradientColors = listOf(Color(0xFF3E91FF), Color(0xFF1755EB)),
                                depthColor = Color(0xFF0C349E),
                                onClick = { activeQuickType = MaterialType.SHORT_NOTES },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAccessSquareTile(
                                title = "Important\nQuestions",
                                icon = Icons.Rounded.Star,
                                gradientColors = listOf(Color(0xFFFFB526), Color(0xFFF56505)),
                                depthColor = Color(0xFFB33E00),
                                onClick = { activeQuickType = MaterialType.IMPORTANT_QUESTIONS },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAccessSquareTile(
                                title = "Chapter\nSummary",
                                icon = Icons.Rounded.FormatListBulleted,
                                gradientColors = listOf(Color(0xFF20DF83), Color(0xFF08A85E)),
                                depthColor = Color(0xFF046E3C),
                                onClick = { activeQuickType = MaterialType.CHAPTER_SUMMARY },
                                modifier = Modifier.weight(1f)
                            )
                            QuickAccessSquareTile(
                                title = "Mind\nMaps",
                                icon = Icons.Rounded.Hub,
                                gradientColors = listOf(Color(0xFFA44DFF), Color(0xFF661CE8)),
                                depthColor = Color(0xFF3E0A9E),
                                onClick = { activeQuickType = MaterialType.MIND_MAP },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        EthicalAdFreeUpgradeBanner(
                            adsEnabled = appSettings.adsEnabled && appSettings.adPlacementMaterials,
                            isPremium = isPremiumUser,
                            onUpgradeClick = onOpenPremium,
                            title = appSettings.adBannerTitle,
                            subtitle = appSettings.adBannerSubtitle,
                            ctaLabel = appSettings.adCtaLabel,
                            familySafeMode = appSettings.adFamilySafeMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessSquareTile(
    title: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    depthColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.94f) }),
        depthColor = depthColor.copy(alpha = 0.92f),
        cornerRadius = 18.dp,
        depthHeight = 4.5.dp,
        shadowElevation = 8.dp,
        shadowColor = depthColor,
        highlightBorderColor = Color.White.copy(alpha = 0.82f),
        borderEndColor = Color.White.copy(alpha = 0.28f),
        glassGlossAlpha = 1.15f,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.24f))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title.replace("\n", " "),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun MaterialRowCard(
    material: StudyMaterial,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleDownload: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Tactile3DCard(
        faceBrush = if (isDark) {
            Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
        },
        depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCFD9FA).copy(alpha = 0.85f),
        cornerRadius = 18.dp,
        depthHeight = 4.5.dp,
        shadowElevation = 7.dp,
        shadowColor = Color(0xFF4758C9),
        highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
        borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricPurple.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = MaterialType.fromKey(material.materialType).label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (material.important) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OrangeStart.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Important",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC76A00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (material.premiumOnly) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Premium content",
                            tint = OrangeStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = material.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${material.subjectName} • ${material.fileSizeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) ElectricPurple else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleDownload) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadForOffline,
                        contentDescription = "Download offline",
                        tint = if (isDownloaded) GreenStart else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialReaderView(
    material: StudyMaterial,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    isPremiumUser: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleDownload: () -> Unit,
    onMarkComplete: () -> Unit,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit
) {
    val locked = material.premiumOnly && !isPremiumUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Class ${material.classGrade} • ${material.subjectName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) ElectricPurple else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onToggleDownload) {
                Icon(
                    imageVector = Icons.Rounded.DownloadForOffline,
                    contentDescription = "Download",
                    tint = if (isDownloaded) GreenStart else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (locked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = OrangeStart,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Premium Study Resource",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This Olympiad & advanced workbook is part of StudyHub Plus. Upgrade anytime to unlock exclusive PDFs and analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenPremium,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("View Premium Plans")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (isDownloaded) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GreenStart.copy(alpha = 0.14f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text(
                                    text = "Available Offline • ${material.fileSizeLabel}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenStart,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        material.contentMarkdown.lines().forEach { line ->
                            when {
                                line.startsWith("# ") -> Text(
                                    text = line.removePrefix("# "),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                line.startsWith("## ") -> Text(
                                    text = line.removePrefix("## "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPurple,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                                line.isBlank() -> Spacer(modifier = Modifier.height(6.dp))
                                else -> Text(
                                    text = line.replace("**", "").replace("*", ""),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onMarkComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Chapter Studied", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyMaterialsState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.NoteAlt,
                contentDescription = null,
                tint = ElectricPurple,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
