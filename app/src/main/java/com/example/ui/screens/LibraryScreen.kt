package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.BookmarkRecord
import com.example.data.model.DownloadRecord
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.StudyMaterial
import com.example.data.model.SyncStatus
import com.example.ui.components.SubjectIconBadge
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.OutfitFontFamily

@Composable
fun LibraryScreen(
    bookmarks: List<BookmarkRecord>,
    downloads: List<DownloadRecord>,
    recentProgress: List<ReadingProgressRecord>,
    allMaterials: List<StudyMaterial>,
    syncStatus: SyncStatus,
    pendingSyncCount: Int,
    initialTab: Int = 0,
    onOpenMaterial: (StudyMaterial) -> Unit,
    onRemoveBookmark: (StudyMaterial) -> Unit,
    onSyncNow: () -> Unit,
    onOpenSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val materialMap = remember(allMaterials) { allMaterials.associateBy { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header matching Screenshot 2 Bottom-Center
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
                text = "My Library",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search library")
            }
        }

        // Tabs: Saved (N) | Downloaded (N) | History
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "Saved (${bookmarks.size})",
                "Downloaded (${downloads.size})",
                "History"
            )
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedTab = idx }
                        .testTag("library_tab_$idx"),
                    shape = RoundedCornerShape(50),
                    color = if (active) ElectricPurple else MaterialTheme.colorScheme.surface,
                    border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    shadowElevation = if (active) 3.dp else 1.dp
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (bookmarks.isEmpty()) {
                        item {
                            EmptyLibraryCard("No saved materials yet. Tap the bookmark icon on any note to save it here!")
                        }
                    } else {
                        items(bookmarks, key = { it.id }) { bm ->
                            val mat = materialMap[bm.materialId]
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
                                onClick = { if (mat != null) onOpenMaterial(mat) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectIconBadge(
                                        iconKey = when (bm.subjectName.lowercase()) {
                                            "science" -> "science"
                                            "mathematics" -> "calculate"
                                            "social science" -> "globe"
                                            else -> "book"
                                        },
                                        colorKey = bm.accentKey,
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${bm.subjectName} • ${bm.subtitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { if (mat != null) onRemoveBookmark(mat) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bookmark,
                                            contentDescription = "Remove bookmark",
                                            tint = ElectricPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (downloads.isEmpty()) {
                        item {
                            EmptyLibraryCard("No offline downloads yet. Download notes or exam papers to study anytime without internet.")
                        }
                    } else {
                        items(downloads, key = { it.id }) { dl ->
                            val mat = materialMap[dl.materialId]
                            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                            Tactile3DCard(
                                faceBrush = if (isDark) {
                                    Brush.linearGradient(listOf(Color(0xFF183D32).copy(alpha = 0.88f), Color(0xFF0F2922).copy(alpha = 0.86f)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFE0FAEE).copy(alpha = 0.88f)))
                                },
                                depthColor = if (isDark) Color(0xFF081713) else Color(0xFFA0E8C2).copy(alpha = 0.85f),
                                cornerRadius = 18.dp,
                                depthHeight = 4.5.dp,
                                shadowElevation = 7.dp,
                                shadowColor = GreenStart,
                                highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                                borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                                onClick = { if (mat != null) onOpenMaterial(mat) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(3.dp, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF056E3D))
                                            .padding(bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(listOf(Color(0xFF20E084), Color(0xFF09A35B)))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DownloadForOffline,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dl.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Class ${dl.classGrade} • ${dl.subjectName} • ${dl.fileSizeLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = GreenStart)
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (recentProgress.isEmpty()) {
                        item {
                            EmptyLibraryCard("Recently opened study materials will appear here automatically.")
                        }
                    } else {
                        items(recentProgress, key = { it.id }) { prog ->
                            val mat = materialMap[prog.materialId]
                            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                            Tactile3DCard(
                                faceBrush = if (isDark) {
                                    Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFF0E8FF).copy(alpha = 0.88f)))
                                },
                                depthColor = if (isDark) Color(0xFF0C102B) else Color(0xFFCBBBF9).copy(alpha = 0.85f),
                                cornerRadius = 18.dp,
                                depthHeight = 4.5.dp,
                                shadowElevation = 7.dp,
                                shadowColor = ElectricPurple,
                                highlightBorderColor = Color.White.copy(alpha = if (isDark) 0.38f else 0.92f),
                                borderEndColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.38f),
                                onClick = { if (mat != null) onOpenMaterial(mat) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(3.dp, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF3F10A8))
                                            .padding(bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(listOf(Color(0xFF9353FF), Color(0xFF5D24E6)))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.History, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prog.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${prog.subjectName} • ${prog.progressPercent}% read",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Green Study Offline & Sync Indicator Vibrant 3D Banner
        Tactile3DCard(
            faceBrush = Brush.linearGradient(
                listOf(Color(0xFF20E084).copy(alpha = 0.94f), Color(0xFF079E57).copy(alpha = 0.94f))
            ),
            depthColor = Color(0xFF046638).copy(alpha = 0.92f),
            cornerRadius = 18.dp,
            depthHeight = 4.5.dp,
            shadowElevation = 9.dp,
            shadowColor = GreenStart,
            highlightBorderColor = Color.White.copy(alpha = 0.82f),
            borderEndColor = Color.White.copy(alpha = 0.28f),
            glassGlossAlpha = 1.15f,
            onClick = onSyncNow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("library_offline_sync_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Offline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Download materials for offline use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
                SyncStatusBadge(
                    syncStatus = syncStatus,
                    pendingCount = pendingSyncCount,
                    onClick = onSyncNow
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 2.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
