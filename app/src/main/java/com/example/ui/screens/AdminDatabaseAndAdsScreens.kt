package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PublishedWithChanges
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.ChapterItem
import com.example.data.model.ExamPaper
import com.example.data.model.QuizItem
import com.example.data.model.QuizQuestion
import com.example.data.model.SchoolClass
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.SyncStatus
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseConnectionStatus
import com.example.ui.components.EthicalAdFreeUpgradeBanner
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.BlueEnd
import com.example.ui.theme.BlueStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenEnd
import com.example.ui.theme.GreenStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.OrangeEnd
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealEnd
import com.example.ui.theme.TealStart

fun LazyListScope.adminManageDatabaseItems(
    classes: List<SchoolClass>,
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    materials: List<StudyMaterial>,
    quizzes: List<QuizItem>,
    questions: List<QuizQuestion>,
    examPapers: List<ExamPaper>,
    announcements: List<AnnouncementItem>,
    adminUsers: List<UserProfile>,
    syncStatus: SyncStatus,
    pendingSyncCount: Int,
    isDatabaseBusy: Boolean,
    supabaseStatus: SupabaseConnectionStatus,
    onOpenSupabaseSetup: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onSeedMissingCloud: () -> Unit,
    onFlushSyncQueue: () -> Unit,
    onResetLocalCache: () -> Unit
) {
    item {
        var showResetConfirm by remember { mutableStateOf(false) }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset Local Room Cache?") },
                text = {
                    Text(
                        "This clears local cached curriculum tables and immediately re-syncs fresh records from Supabase."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            onResetLocalCache()
                        }
                    ) {
                        Text("Reset & Re-sync", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Cloud & Local Database Status Hero
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF312E81), Color(0xFF4C1D95))
                ),
                depthColor = Color(0xFF17134D),
                shadowColor = BlueEnd,
                cornerRadius = 20.dp,
                depthHeight = 4.5.dp,
                floatingLift = 4.5.dp,
                glassGlossAlpha = 1.0f,
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Storage, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cloud & Local Database",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (supabaseStatus.configured) {
                                        "Supabase (${supabaseStatus.hostLabel ?: "Connected"}) • ${supabaseStatus.sourceLabel}"
                                    } else {
                                        "Supabase Not Configured • Room Cache Active"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDCE4FF)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (!supabaseStatus.configured || syncStatus == SyncStatus.OFFLINE) {
                                OrangeStart.copy(alpha = 0.25f)
                            } else {
                                SuccessGreen.copy(alpha = 0.24f)
                            }
                        ) {
                            Text(
                                text = if (!supabaseStatus.configured) "SETUP" else syncStatus.label.uppercase(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!supabaseStatus.configured || syncStatus == SyncStatus.OFFLINE) {
                                    Color(0xFFFFD8A8)
                                } else {
                                    Color(0xFF9BF6C6)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatabaseMiniStat(
                            label = "Total Records",
                            value = "${classes.size + subjects.size + chapters.size + materials.size + quizzes.size + questions.size + examPapers.size}",
                            modifier = Modifier.weight(1f)
                        )
                        DatabaseMiniStat(
                            label = "Pending Queue",
                            value = "$pendingSyncCount",
                            modifier = Modifier.weight(1f)
                        )
                        DatabaseMiniStat(
                            label = "Users",
                            value = "${adminUsers.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Database Operations Panel
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Database Operations",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        if (isDatabaseBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ElectricPurple
                            )
                        }
                    }

                    DatabaseActionTile(
                        title = "Setup / Configure Supabase",
                        subtitle = if (supabaseStatus.configured) {
                            "Update URL & Anon Key, test connection, or copy SQL schema"
                        } else {
                            "Enter Supabase URL & Anon Key manually + copy SQL schema"
                        },
                        icon = Icons.Rounded.Storage,
                        colors = listOf(PurpleStart, PurpleEnd),
                        depthColor = Color(0xFF33088C),
                        enabled = !isDatabaseBusy,
                        tag = "db_setup_supabase_btn",
                        onClick = onOpenSupabaseSetup
                    )

                    DatabaseActionTile(
                        title = "Sync Cloud → Device Cache",
                        subtitle = "Pull latest Supabase classes, materials, quizzes & users",
                        icon = Icons.Rounded.CloudSync,
                        colors = listOf(BlueStart, BlueEnd),
                        depthColor = Color(0xFF092E8A),
                        enabled = !isDatabaseBusy,
                        tag = "db_sync_cloud_btn",
                        onClick = onSyncCloudNow
                    )

                    DatabaseActionTile(
                        title = "Seed / Repair Default Curriculum",
                        subtitle = "Insert any missing default classes, chapters, quizzes & settings to Supabase",
                        icon = Icons.Rounded.Backup,
                        colors = listOf(GreenStart, GreenEnd),
                        depthColor = Color(0xFF045E34),
                        enabled = !isDatabaseBusy,
                        tag = "db_seed_cloud_btn",
                        onClick = onSeedMissingCloud
                    )

                    DatabaseActionTile(
                        title = "Flush Offline Sync Queue ($pendingSyncCount)",
                        subtitle = "Push queued student/admin updates to Supabase",
                        icon = Icons.Rounded.Sync,
                        colors = listOf(TealStart, TealEnd),
                        depthColor = Color(0xFF065466),
                        enabled = !isDatabaseBusy,
                        tag = "db_flush_queue_btn",
                        onClick = onFlushSyncQueue
                    )

                    DatabaseActionTile(
                        title = "Reset Local Cache & Re-sync",
                        subtitle = "Clear Room SQLite cache tables and rebuild from Supabase",
                        icon = Icons.Rounded.DeleteSweep,
                        colors = listOf(OrangeStart, OrangeEnd),
                        depthColor = Color(0xFF9E3C00),
                        enabled = !isDatabaseBusy,
                        tag = "db_reset_cache_btn",
                        onClick = { showResetConfirm = true }
                    )
                }
            }

            // Collection Inventory
            Text(
                text = "Supabase Tables & Room Cache",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            val publishedMaterials = materials.count { it.published }
            val draftMaterials = materials.size - publishedMaterials
            val adminCount = adminUsers.count { it.role == "admin" }
            val studentCount = adminUsers.size - adminCount

            CollectionInventoryCard("classes", "${classes.size} grades configured (Classes 6–12)", "${classes.count { it.published }} LIVE")
            CollectionInventoryCard("subjects", "${subjects.size} subjects across classes", "${subjects.count { it.published }} LIVE")
            CollectionInventoryCard("chapters", "${chapters.size} curriculum chapters", "${chapters.count { it.published }} LIVE")
            CollectionInventoryCard("study_materials", "${materials.size} total • $publishedMaterials published • $draftMaterials drafts", "SYNCED")
            CollectionInventoryCard("quizzes & questions", "${quizzes.size} quizzes • ${questions.size} questions", "${quizzes.count { it.published }} LIVE")
            CollectionInventoryCard("exam_papers", "${examPapers.size} previous, model & sample papers", "${examPapers.count { it.published }} LIVE")
            CollectionInventoryCard("announcements", "${announcements.size} active broadcasts", "CLOUD")
            CollectionInventoryCard("users", "${adminUsers.size} accounts ($studentCount students, $adminCount admins)", "AUTH")
        }
    }
}

fun LazyListScope.adminManageAdsItems(
    appSettings: AppSettings,
    onSaveAppSettings: (AppSettings) -> Unit
) {
    item {
        var adsEnabled by remember(appSettings) { mutableStateOf(appSettings.adsEnabled) }
        var familySafeMode by remember(appSettings) { mutableStateOf(appSettings.adFamilySafeMode) }
        var placementHome by remember(appSettings) { mutableStateOf(appSettings.adPlacementHome) }
        var placementMaterials by remember(appSettings) { mutableStateOf(appSettings.adPlacementMaterials) }
        var placementQuizzes by remember(appSettings) { mutableStateOf(appSettings.adPlacementQuizzes) }
        var placementPapers by remember(appSettings) { mutableStateOf(appSettings.adPlacementPapers) }
        var bannerTitle by remember(appSettings) { mutableStateOf(appSettings.adBannerTitle) }
        var bannerSubtitle by remember(appSettings) { mutableStateOf(appSettings.adBannerSubtitle) }
        var ctaLabel by remember(appSettings) { mutableStateOf(appSettings.adCtaLabel) }
        var networkProvider by remember(appSettings) { mutableStateOf(appSettings.adNetworkProvider) }

        val activePlacementsCount = listOf(placementHome, placementMaterials, placementQuizzes, placementPapers).count { it }
        val providers = listOf(
            "StudyHub Direct (Family-Safe)",
            "AdMob Kids-Safe",
            "Sponsor Scholarships"
        )

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = ElectricPurple,
            unfocusedBorderColor = NavyCardBorder,
            focusedLabelColor = Color(0xFFC5A6FF),
            unfocusedLabelColor = Color(0xFF9AA5D1),
            cursorColor = ElectricPurple
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ads Status Summary Hero
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    if (adsEnabled) listOf(Color(0xFF5B21B6), Color(0xFF7C3AED), Color(0xFF4338CA))
                    else listOf(Color(0xFF334155), Color(0xFF1E293B))
                ),
                depthColor = Color(0xFF1A0F4D),
                shadowColor = PurpleEnd,
                cornerRadius = 20.dp,
                depthHeight = 4.5.dp,
                floatingLift = 4.5.dp,
                glassGlossAlpha = 1.0f,
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AdsClick, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Manage Ads & Placements",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = networkProvider,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDCE4FF)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (adsEnabled) SuccessGreen.copy(alpha = 0.25f) else OrangeStart.copy(alpha = 0.28f)
                        ) {
                            Text(
                                text = if (adsEnabled) "ACTIVE" else "PAUSED",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (adsEnabled) Color(0xFF9BF6C6) else Color(0xFFFFD8A8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatabaseMiniStat(
                            label = "Placements",
                            value = "$activePlacementsCount / 4",
                            modifier = Modifier.weight(1f)
                        )
                        DatabaseMiniStat(
                            label = "Child-Safe",
                            value = if (familySafeMode) "ON" else "OFF",
                            modifier = Modifier.weight(1f)
                        )
                        DatabaseMiniStat(
                            label = "Premium Tier",
                            value = "Ad-Free",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Master Controls & Provider
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ad Policy & Network",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Ads Banner", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Shown only to Free-tier students; Premium stays ad-free",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA5D1)
                            )
                        }
                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = { adsEnabled = it },
                            modifier = Modifier.testTag("ads_master_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Family-Safe & COPPA Filter", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "Restrict banners to age-appropriate educational promotions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA5D1)
                            )
                        }
                        Switch(
                            checked = familySafeMode,
                            onCheckedChange = { familySafeMode = it },
                            modifier = Modifier.testTag("ads_family_safe_switch")
                        )
                    }

                    Text(
                        text = "Ad Source / Network",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFC5CEF0)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        providers.forEach { provider ->
                            val selected = networkProvider == provider
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { networkProvider = provider },
                                color = if (selected) ElectricPurple else NavyCard,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = provider,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else Color(0xFFC5CEF0),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Placement Toggles
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
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
                    Text(
                        text = "Screen Placements",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    AdPlacementToggleRow("Home Dashboard", "Bottom slot on student Home feed", placementHome) { placementHome = it }
                    AdPlacementToggleRow("Study Materials", "Bottom slot on Subjects & Notes hub", placementMaterials) { placementMaterials = it }
                    AdPlacementToggleRow("Quizzes & Practice", "Bottom slot on Quiz selection screen", placementQuizzes) { placementQuizzes = it }
                    AdPlacementToggleRow("Exam Papers", "Bottom slot on Model & Sample Papers", placementPapers) { placementPapers = it }
                }
            }

            // Banner Copy & Live Preview
            Tactile3DCard(
                faceBrush = Brush.linearGradient(
                    listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
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
                    Text(
                        text = "Banner Creative & Preview",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = bannerTitle,
                        onValueChange = { bannerTitle = it },
                        label = { Text("Banner Headline") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bannerSubtitle,
                        onValueChange = { bannerSubtitle = it },
                        label = { Text("Banner Description") },
                        colors = fieldColors,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ctaLabel,
                        onValueChange = { ctaLabel = it },
                        label = { Text("CTA Button Label") },
                        colors = fieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live Student Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC5A6FF),
                        fontWeight = FontWeight.Bold
                    )
                    if (adsEnabled) {
                        EthicalAdFreeUpgradeBanner(
                            adsEnabled = true,
                            isPremium = false,
                            onUpgradeClick = {},
                            title = bannerTitle,
                            subtitle = bannerSubtitle,
                            ctaLabel = ctaLabel,
                            familySafeMode = familySafeMode
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NavyCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ads are currently paused for all students.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA5D1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
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
                                    adsEnabled = adsEnabled,
                                    adFamilySafeMode = familySafeMode,
                                    adPlacementHome = placementHome,
                                    adPlacementMaterials = placementMaterials,
                                    adPlacementQuizzes = placementQuizzes,
                                    adPlacementPapers = placementPapers,
                                    adBannerTitle = bannerTitle.trim().ifBlank { "StudyHub Plus • Family-Safe Learning" },
                                    adBannerSubtitle = bannerSubtitle.trim().ifBlank { "Unlock unlimited offline packs, advanced analytics & ad-free mode." },
                                    adCtaLabel = ctaLabel.trim().ifBlank { "Upgrade" },
                                    adNetworkProvider = networkProvider
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_ad_settings_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PublishedWithChanges, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Ad Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFDCE4FF)
        )
    }
}

@Composable
private fun DatabaseActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    depthColor: Color,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(colors.map { it.copy(alpha = if (enabled) 0.92f else 0.45f) }),
        depthColor = depthColor,
        shadowColor = colors.last(),
        cornerRadius = 15.dp,
        depthHeight = 3.5.dp,
        floatingLift = 3.dp,
        glassGlossAlpha = 0.9f,
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
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
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CollectionInventoryCard(
    collectionName: String,
    detail: String,
    badge: String
) {
    Tactile3DCard(
        faceBrush = Brush.linearGradient(
            listOf(Color(0xFF1D2761).copy(alpha = 0.94f), Color(0xFF11173D).copy(alpha = 0.96f))
        ),
        depthColor = Color(0xFF070B21),
        shadowColor = Color(0xFF05081A),
        highlightBorderColor = Color.White.copy(alpha = 0.26f),
        borderEndColor = NavyCardBorder,
        cornerRadius = 16.dp,
        depthHeight = 3.5.dp,
        floatingLift = 3.5.dp,
        glassGlossAlpha = 0.6f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricPurple.copy(alpha = 0.22f))
                    .border(1.dp, ElectricPurple.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = Color(0xFFC5A6FF), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(collectionName, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA5D1))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyCard)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9BF6C6),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdPlacementToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9AA5D1))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
