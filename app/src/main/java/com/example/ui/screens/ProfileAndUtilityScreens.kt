package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AnnouncementItem
import com.example.data.model.AppSettings
import com.example.data.model.ChapterItem
import com.example.data.model.ExamPaper
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyMaterial
import com.example.data.model.SubjectItem
import com.example.data.model.UserProfile
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
fun ProfileScreen(
    profile: UserProfile,
    isAuthorizedAdmin: Boolean,
    quizAttempts: List<QuizAttemptRecord>,
    onUpdateName: (String) -> Unit,
    onSelectClass: (Long) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateNotificationPrefs: (Boolean, Boolean, Boolean) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenPremium: () -> Unit,
    onSwitchToAdmin: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editNameOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var progressOpen by remember { mutableStateOf(false) }
    var privacyPolicyOpen by remember { mutableStateOf(false) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    if (editNameOpen) {
        var draftName by remember { mutableStateOf(profile.displayName) }
        AlertDialog(
            onDismissRequest = { editNameOpen = false },
            title = { Text("Edit Profile Name") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text("Full Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateName(draftName)
                    editNameOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editNameOpen = false }) { Text("Cancel") }
            }
        )
    }

    if (settingsOpen) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* gracefully respected */ }

        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Deep Navy Dark Mode")
                        Switch(
                            checked = profile.darkThemeEnabled,
                            onCheckedChange = onToggleDarkTheme
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Study Notifications")
                        Switch(
                            checked = profile.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onUpdateNotificationPrefs(
                                    enabled,
                                    profile.examRemindersEnabled,
                                    profile.newMaterialsEnabled
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exam Reminders")
                        Switch(
                            checked = profile.examRemindersEnabled,
                            onCheckedChange = {
                                onUpdateNotificationPrefs(
                                    profile.notificationsEnabled,
                                    it,
                                    profile.newMaterialsEnabled
                                )
                            }
                        )
                    }
                    Text("Selected Class: Class ${profile.selectedClass}", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (6L..12L).forEach { g ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectClass(g) },
                                color = if (profile.selectedClass == g) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Class $g",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (profile.selectedClass == g) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { settingsOpen = false }) { Text("Done") }
            }
        )
    }

    if (progressOpen) {
        AlertDialog(
            onDismissRequest = { progressOpen = false },
            title = { Text("My Study Progress", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Average Quiz Score: ${profile.avgScore}%")
                    Text("Current Learning Streak: ${profile.dayStreak} Days")
                    Text("Total Reward Points: ${profile.points} pts")
                    Text("Quizzes Completed: ${quizAttempts.size}")
                    quizAttempts.take(4).forEach { att ->
                        Text(
                            text = "• ${att.quizTitle}: ${att.scorePercent}% (${att.correctAnswers}/${att.totalQuestions})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { progressOpen = false }) { Text("Close") }
            }
        )
    }

    if (privacyPolicyOpen) {
        AlertDialog(
            onDismissRequest = { privacyPolicyOpen = false },
            title = { Text("Privacy Policy & Terms", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "StudyHub Student Privacy Commitment",
                        fontWeight = FontWeight.Bold,
                        color = ElectricPurple
                    )
                    Text(
                        "• We collect only your Google Sign-In display name, email, selected class (6–12), and study progress.\n" +
                            "• Student records are strictly isolated per user account with Firebase security rules.\n" +
                            "• No personalized behavioral ads are shown to student accounts.\n" +
                            "• You may delete all your cloud and local data anytime from Profile → Delete Account.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { privacyPolicyOpen = false }) { Text("I Understand") }
            }
        )
    }

    if (deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmOpen = false },
            title = { Text("Delete Account & Study Data?") },
            text = {
                Text("This permanently deletes your cloud profile, quiz attempts, bookmarks, and offline sync queue, then signs you out.")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmOpen = false
                    onDeleteAccount()
                }) {
                    Text("Delete Forever", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmOpen = false }) { Text("Cancel") }
            }
        )
    }

    if (aboutOpen) {
        AlertDialog(
            onDismissRequest = { aboutOpen = false },
            title = { Text("About StudyHub", fontWeight = FontWeight.Bold) },
            text = {
                Text("StudyHub v1.0\nLearn. Practice. Achieve.\n\nBuilt for Classes 6–12 students with offline-first notes, interactive quizzes, exam papers, and role-verified admin publishing.")
            },
            confirmButton = {
                TextButton(onClick = { aboutOpen = false }) { Text("OK") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Curved Header matching Screenshot 2 Bottom-Right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(IndigoHeaderStart, IndigoHeaderEnd, ElectricPurple)),
                    shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
                )
                .statusBarsPadding()
                .padding(bottom = 46.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Profile",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_student_avatar),
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.displayName,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC542),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isAuthorizedAdmin) "Admin • Class ${profile.selectedClass}" else "Student • Class ${profile.selectedClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    IconButton(onClick = { editNameOpen = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit name", tint = Color.White)
                    }
                }
            }
        }

        // Overlapping 3-Stat 3D Card (Avg Score, Day Streak, Points)
        val isDarkProfile = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Tactile3DCard(
            faceBrush = if (isDarkProfile) {
                Brush.linearGradient(listOf(Color(0xFF222D6B).copy(alpha = 0.90f), Color(0xFF161E4A).copy(alpha = 0.88f)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.92f), Color(0xFFEEF3FF).copy(alpha = 0.88f)))
            },
            depthColor = if (isDarkProfile) Color(0xFF0C102B) else Color(0xFFCBD6FA).copy(alpha = 0.85f),
            cornerRadius = 20.dp,
            depthHeight = 4.5.dp,
            shadowElevation = 10.dp,
            shadowColor = Color(0xFF4758C9),
            highlightBorderColor = Color.White.copy(alpha = if (isDarkProfile) 0.42f else 0.95f),
            borderEndColor = Color.White.copy(alpha = if (isDarkProfile) 0.16f else 0.42f),
            glassGlossAlpha = 1.1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-28).dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatItem(
                    icon = Icons.Rounded.EmojiEvents,
                    iconTint = OrangeStart,
                    value = "${profile.avgScore}%",
                    label = "Avg Score"
                )
                ProfileStatItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = DangerRed,
                    value = "${profile.dayStreak}",
                    label = "Day Streak"
                )
                ProfileStatItem(
                    icon = Icons.Rounded.Star,
                    iconTint = Color(0xFFF5B400),
                    value = "${profile.points}",
                    label = "Points"
                )
            }
        }

        // Menu List 3D Card
        Tactile3DCard(
            faceBrush = if (isDarkProfile) {
                Brush.linearGradient(listOf(Color(0xFF202A63).copy(alpha = 0.88f), Color(0xFF151C45).copy(alpha = 0.86f)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.92f), Color(0xFFF3F6FF).copy(alpha = 0.88f)))
            },
            depthColor = if (isDarkProfile) Color(0xFF0B0F28) else Color(0xFFD2DCF8).copy(alpha = 0.85f),
            cornerRadius = 20.dp,
            depthHeight = 4.5.dp,
            shadowElevation = 8.dp,
            shadowColor = Color(0xFF4758C9),
            highlightBorderColor = Color.White.copy(alpha = if (isDarkProfile) 0.38f else 0.92f),
            borderEndColor = Color.White.copy(alpha = if (isDarkProfile) 0.14f else 0.40f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-12).dp)
        ) {
            Column {
                ProfileMenuRow(Icons.Rounded.TrendingUp, BlueStart, "My Progress") { progressOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.Bookmark, ElectricPurple, "Bookmarks", onClick = onOpenBookmarks)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.Download, GreenStart, "Downloads", onClick = onOpenDownloads)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(
                    Icons.Rounded.WorkspacePremium,
                    OrangeStart,
                    if (profile.subscriptionTier == "premium") "StudyHub Plus (Active)" else "Upgrade to Premium",
                    onClick = onOpenPremium
                )
                if (isAuthorizedAdmin) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        Icons.Rounded.AdminPanelSettings,
                        ElectricPurple,
                        "Open Admin Dashboard",
                        modifier = Modifier.testTag("switch_to_admin_row"),
                        onClick = onSwitchToAdmin
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.Settings, BlueStart, "Settings") { settingsOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.Policy, ElectricPurple, "Privacy Policy & Terms") { privacyPolicyOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.AutoMirrored.Rounded.HelpOutline, BlueStart, "Help & Support") { privacyPolicyOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.Info, ElectricPurple, "About App") { aboutOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(Icons.Rounded.DeleteForever, DangerRed, "Delete Account & Data") { deleteConfirmOpen = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileMenuRow(
                    Icons.AutoMirrored.Rounded.Logout,
                    DangerRed,
                    "Sign Out",
                    modifier = Modifier.testTag("sign_out_row"),
                    onClick = onSignOut
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileStatItem(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PremiumScreen(
    profile: UserProfile,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(IndigoHeaderStart, ElectricPurple)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "StudyHub Plus",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.White
                )
                Text(
                    text = "Current Plan: ${profile.subscriptionTier.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFD670)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Included Benefits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    listOf(
                        "Exclusive Olympiad & Advanced PDF Guides",
                        "Unlimited Offline Chapter Downloads",
                        "Detailed Quiz Accuracy & Topic Analytics",
                        "100% Ad-Free Distraction-Free Studying"
                    ).forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GreenStart, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Configured Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Monthly Plan: ${appSettings.premiumMonthlyPrice}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Annual Plan: ${appSettings.premiumYearlyPrice} (Best Value)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ElectricPurple)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Subscriptions are managed securely through Google Play Billing once your Play Console product IDs are linked. Restore purchases will automatically sync verified entitlements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalSearchScreen(
    subjects: List<SubjectItem>,
    chapters: List<ChapterItem>,
    materials: List<StudyMaterial>,
    questions: List<QuizQuestion>,
    examPapers: List<ExamPaper>,
    onOpenMaterial: (StudyMaterial) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Materials", "Subjects", "Chapters", "Papers", "Questions")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search notes, chapters, MCQs, papers...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { f ->
                val active = filter == f
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { filter = f },
                    shape = RoundedCornerShape(50),
                    color = if (active) ElectricPurple else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = f,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        val q = query.trim()
        val matchedMaterials = if (filter == "All" || filter == "Materials") {
            materials.filter { it.published && (q.isBlank() || it.title.contains(q, true) || it.description.contains(q, true) || it.subjectName.contains(q, true)) }
        } else emptyList()
        val matchedSubjects = if (filter == "All" || filter == "Subjects") {
            subjects.filter { it.published && (q.isBlank() || it.name.contains(q, true)) }
        } else emptyList()
        val matchedChapters = if (filter == "All" || filter == "Chapters") {
            chapters.filter { it.published && (q.isBlank() || it.title.contains(q, true) || it.summary.contains(q, true)) }
        } else emptyList()
        val matchedPapers = if (filter == "All" || filter == "Papers") {
            examPapers.filter { it.published && (q.isBlank() || it.title.contains(q, true) || it.subjectName.contains(q, true)) }
        } else emptyList()
        val matchedQuestions = if (filter == "All" || filter == "Questions") {
            questions.filter { it.published && (q.isBlank() || it.prompt.contains(q, true)) }
        } else emptyList()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(matchedMaterials.take(15), key = { "m_${it.id}" }) { mat ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenMaterial(mat) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Study Material • Class ${mat.classGrade}", style = MaterialTheme.typography.labelSmall, color = ElectricPurple)
                        Text(mat.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(mat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            items(matchedChapters.take(10), key = { "c_${it.id}" }) { chap ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Chapter • Class ${chap.classGrade} ${chap.subjectName}", style = MaterialTheme.typography.labelSmall, color = BlueStart)
                        Text(chap.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(matchedPapers.take(10), key = { "p_${it.id}" }) { paper ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Exam Paper • ${paper.year}", style = MaterialTheme.typography.labelSmall, color = OrangeStart)
                        Text(paper.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(matchedQuestions.take(10), key = { "q_${it.id}" }) { qq ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Practice Question", style = MaterialTheme.typography.labelSmall, color = GreenStart)
                        Text(qq.prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            items(matchedSubjects.take(8), key = { "s_${it.id}" }) { subj ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Subject • Class ${subj.classGrade}", style = MaterialTheme.typography.labelSmall, color = ElectricPurple)
                        Text(subj.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    announcements: List<AnnouncementItem>,
    profile: UserProfile,
    onUpdatePrefs: (Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
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
            Text(
                text = "Notifications & Updates",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Alert Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("New Materials & Quizzes", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile.newMaterialsEnabled,
                                onCheckedChange = { onUpdatePrefs(profile.notificationsEnabled, profile.examRemindersEnabled, it) }
                            )
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Exam Prep Reminders", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile.examRemindersEnabled,
                                onCheckedChange = { onUpdatePrefs(profile.notificationsEnabled, it, profile.newMaterialsEnabled) }
                            )
                        }
                    }
                }
            }
            items(announcements, key = { it.id }) { ann ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = ElectricPurple)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(ann.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (ann.targetClass == 0L) "All Classes" else "Class ${ann.targetClass}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricPurple
                            )
                        }
                    }
                }
            }
        }
    }
}
