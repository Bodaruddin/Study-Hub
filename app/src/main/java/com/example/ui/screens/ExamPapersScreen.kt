package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.ExamPaper
import com.example.data.model.PaperCategory
import com.example.ui.components.EthicalAdFreeUpgradeBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.OutfitFontFamily

@Composable
fun ExamPapersScreen(
    selectedClass: Long,
    examPapers: List<ExamPaper>,
    downloadedPaperIds: Set<String>,
    onToggleDownloadPaper: (ExamPaper) -> Unit,
    onSelectClass: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    appSettings: AppSettings = AppSettings(),
    isPremiumUser: Boolean = false,
    onOpenPremium: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(PaperCategory.PREVIOUS_YEAR) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var viewingPaper by remember { mutableStateOf<ExamPaper?>(null) }

    val categories = listOf(
        PaperCategory.PREVIOUS_YEAR,
        PaperCategory.MODEL_PAPER,
        PaperCategory.SAMPLE_PAPER
    )
    val availableSubjects = listOf("All") + examPapers.map { it.subjectName }.distinct()
    val filteredPapers = examPapers.filter { p ->
        p.published &&
            p.classGrade == selectedClass &&
            p.paperCategory == selectedCategory.key &&
            (selectedSubjectFilter == "All" || p.subjectName == selectedSubjectFilter)
    }

    if (viewingPaper != null) {
        AlertDialog(
            onDismissRequest = { viewingPaper = null },
            title = { Text(viewingPaper!!.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "${viewingPaper!!.subjectName} • Year ${viewingPaper!!.year} • ${viewingPaper!!.fileSizeLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricPurple
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = viewingPaper!!.contentText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onToggleDownloadPaper(viewingPaper!!)
                    viewingPaper = null
                }) {
                    Text(if (viewingPaper!!.id in downloadedPaperIds) "Remove Offline" else "Save Offline")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingPaper = null }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header matching Screenshot 2 Bottom-Left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Exam Papers",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Box {
                IconButton(
                    onClick = { filterMenuOpen = true },
                    modifier = Modifier.testTag("exam_papers_filter_button")
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "Filter papers")
                }
                DropdownMenu(
                    expanded = filterMenuOpen,
                    onDismissRequest = { filterMenuOpen = false }
                ) {
                    (6L..12L).forEach { grade ->
                        DropdownMenuItem(
                            text = { Text("Class $grade${if (grade == selectedClass) " ✓" else ""}") },
                            onClick = {
                                filterMenuOpen = false
                                onSelectClass(grade)
                            }
                        )
                    }
                    availableSubjects.forEach { subj ->
                        DropdownMenuItem(
                            text = { Text("Subject: $subj") },
                            onClick = {
                                filterMenuOpen = false
                                selectedSubjectFilter = subj
                            }
                        )
                    }
                }
            }
        }

        // Segmented Category Tabs (Previous Year / Model Papers / Sample Papers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = selectedCategory == cat
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedCategory = cat }
                        .testTag("exam_tab_${cat.key}"),
                    shape = RoundedCornerShape(50),
                    color = if (active) ElectricPurple else MaterialTheme.colorScheme.surface,
                    border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    shadowElevation = if (active) 3.dp else 1.dp
                ) {
                    Text(
                        text = cat.label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredPapers.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "No ${selectedCategory.label} found for Class $selectedClass yet. Try switching class or category above.",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredPapers, key = { it.id }) { paper ->
                    val downloaded = paper.id in downloadedPaperIds
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Tactile3DCard(
                        faceBrush = if (isDark) {
                            Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFEEF3FF).copy(alpha = 0.86f)))
                        },
                        depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCCD7F9).copy(alpha = 0.85f),
                        cornerRadius = 18.dp,
                        depthHeight = 4.5.dp,
                        shadowElevation = 7.dp,
                        shadowColor = Color(0xFF4758C9),
                        highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                        borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                        onClick = { viewingPaper = paper },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(4.dp, RoundedCornerShape(13.dp))
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color(0xFF9E1120))
                                    .padding(bottom = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFFFF5E66), Color(0xFFDC1F32)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = "PDF paper",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = paper.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = paper.fileSizeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(3.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(if (downloaded) Color(0xFF076D3C) else Color(0xFF103799))
                                    .clickable { onToggleDownloadPaper(paper) }
                                    .padding(bottom = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                if (downloaded) {
                                                    listOf(Color(0xFF22E588), Color(0xFF08A85E))
                                                } else {
                                                    listOf(Color(0xFF568CFF), Color(0xFF2850E8))
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (downloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                                        contentDescription = if (downloaded) "Downloaded" else "Download PDF",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Score High Vibrant 3D Motivation Banner
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Tactile3DCard(
                    faceBrush = Brush.linearGradient(
                        listOf(Color(0xFF4B7BFF).copy(alpha = 0.94f), Color(0xFF6B38FB).copy(alpha = 0.94f))
                    ),
                    depthColor = Color(0xFF31149E).copy(alpha = 0.92f),
                    cornerRadius = 20.dp,
                    depthHeight = 4.5.dp,
                    shadowElevation = 10.dp,
                    shadowColor = Color(0xFF562FE8),
                    highlightBorderColor = Color.White.copy(alpha = 0.82f),
                    borderEndColor = Color.White.copy(alpha = 0.28f),
                    glassGlossAlpha = 1.15f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Score High!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Prepare well for your exams.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            item {
                EthicalAdFreeUpgradeBanner(
                    adsEnabled = appSettings.adsEnabled && appSettings.adPlacementPapers,
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
