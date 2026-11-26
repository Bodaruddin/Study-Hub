package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.remote.AuthManager
import com.example.ui.components.StudyHubBrandLogo
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenStart
import com.example.ui.theme.IndigoHeaderEnd
import com.example.ui.theme.IndigoHeaderStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OutfitFontFamily

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(IndigoHeaderStart, IndigoHeaderEnd, NavyDeep)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                StudyHubBrandLogo(subtitle = "Learn. Practice. Achieve.", isDarkSurface = true)
                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.09f))
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_student_avatar),
                        contentDescription = "StudyHub student mascot",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Classes 6–12\nSmart Learning Companion",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Chapter-wise notes, timed quizzes, previous-year exam papers, and offline study packs synced securely across your devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC5CEF0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AuthFeaturePill(
                        icon = Icons.Rounded.AutoStories,
                        tint = CyanAccent,
                        text = "Structured Notes, Mind Maps & Q&A Sets"
                    )
                    AuthFeaturePill(
                        icon = Icons.Rounded.Quiz,
                        tint = Color(0xFFFFB547),
                        text = "Practice & Exam Mode Quizzes with Explanations"
                    )
                    AuthFeaturePill(
                        icon = Icons.Rounded.CloudDone,
                        tint = GreenStart,
                        text = "Offline Downloads with Automatic Cloud Sync"
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = DangerRed.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = errorMessage!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC9C9),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        AuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                isLoading = false
                                errorMessage = "Google Sign-In was cancelled or no Google account is signed in on this device. Please select or add a Google account to continue."
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricPurple,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF9DA9DA),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Student privacy protected • Role-verified access",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9DA9DA)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeaturePill(
    icon: ImageVector,
    tint: Color,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NavyCard.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE7ECFF)
            )
        }
    }
}
