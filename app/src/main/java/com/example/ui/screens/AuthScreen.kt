package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private enum class ActiveLoginLoading {
    NONE,
    GOOGLE,
    EMAIL,
    EMAIL_RESET
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeLoading by remember { mutableStateOf(ActiveLoginLoading.NONE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var emailSectionExpanded by remember { mutableStateOf(false) }
    var isCreateAccountMode by remember { mutableStateOf(false) }
    var emailNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isBusy = activeLoading != ActiveLoginLoading.NONE

    BackHandler(enabled = emailSectionExpanded && !isBusy) {
        emailSectionExpanded = false
        errorMessage = null
        infoMessage = null
    }

    LaunchedEffect(emailSectionExpanded) {
        if (emailSectionExpanded) {
            scrollState.animateScrollTo(0)
        }
    }

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
            .imePadding()
    ) {
        val mascotSize by animateDpAsState(
            targetValue = if (emailSectionExpanded) 108.dp else 144.dp,
            label = "mascot_size"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (emailSectionExpanded) Arrangement.Top else Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                StudyHubBrandLogo(subtitle = "Learn. Practice. Achieve.", isDarkSurface = true)
                Spacer(modifier = Modifier.height(if (emailSectionExpanded) 14.dp else 22.dp))

                Box(
                    modifier = Modifier
                        .size(mascotSize)
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

                Spacer(modifier = Modifier.height(if (emailSectionExpanded) 12.dp else 18.dp))
                Text(
                    text = "Smart Learning Companion",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (emailSectionExpanded) 21.sp else 25.sp,
                    lineHeight = if (emailSectionExpanded) 27.sp else 31.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(visible = !emailSectionExpanded) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chapter-wise notes, timed quizzes, previous-year exam papers, and offline study packs synced securely across your devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC5CEF0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (emailSectionExpanded) 14.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = emailSectionExpanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = NavyCard.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isCreateAccountMode = false
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("email_signin_tab"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (!isCreateAccountMode) CyanAccent else Color.White.copy(alpha = 0.2f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!isCreateAccountMode) CyanAccent.copy(alpha = 0.16f) else Color.Transparent,
                                        contentColor = if (!isCreateAccountMode) CyanAccent else Color(0xFFC5CEF0)
                                    )
                                ) {
                                    Text("Sign In", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        isCreateAccountMode = true
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("email_signup_tab"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCreateAccountMode) CyanAccent else Color.White.copy(alpha = 0.2f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isCreateAccountMode) CyanAccent.copy(alpha = 0.16f) else Color.Transparent,
                                        contentColor = if (isCreateAccountMode) CyanAccent else Color(0xFFC5CEF0)
                                    )
                                ) {
                                    Text("Create Account", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (isCreateAccountMode) {
                                OutlinedTextField(
                                    value = emailNameInput,
                                    onValueChange = { emailNameInput = it },
                                    label = { Text("Full Name") },
                                    placeholder = { Text("e.g. Aarav Sharma") },
                                    singleLine = true,
                                    enabled = !isBusy,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_name_input")
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email address") },
                                placeholder = { Text("student@example.com") },
                                singleLine = true,
                                enabled = !isBusy,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_address_input")
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                placeholder = { Text(if (isCreateAccountMode) "Minimum 6 characters" else "Enter your password") },
                                singleLine = true,
                                enabled = !isBusy,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.testTag("toggle_password_visibility_button")
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            tint = Color(0xFFC5CEF0)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                colors = authTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_password_input")
                            )

                            AuthStatusBanners(
                                errorMessage = errorMessage,
                                infoMessage = infoMessage,
                                bottomPadding = 0.dp
                            )

                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    activeLoading = ActiveLoginLoading.EMAIL
                                    if (isCreateAccountMode) {
                                        AuthManager.createAccountWithEmail(
                                            displayNameRaw = emailNameInput,
                                            emailRaw = emailInput,
                                            password = passwordInput,
                                            scope = scope,
                                            onAuthSuccess = {
                                                activeLoading = ActiveLoginLoading.NONE
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                activeLoading = ActiveLoginLoading.NONE
                                                errorMessage = msg
                                            }
                                        )
                                    } else {
                                        AuthManager.signInWithEmail(
                                            emailRaw = emailInput,
                                            password = passwordInput,
                                            scope = scope,
                                            onAuthSuccess = {
                                                activeLoading = ActiveLoginLoading.NONE
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                activeLoading = ActiveLoginLoading.NONE
                                                errorMessage = msg
                                            }
                                        )
                                    }
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("email_submit_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = NavyDeep
                                )
                            ) {
                                if (activeLoading == ActiveLoginLoading.EMAIL) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = NavyDeep,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isCreateAccountMode) "Create Account" else "Sign In with Email",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isCreateAccountMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            errorMessage = null
                                            infoMessage = null
                                            activeLoading = ActiveLoginLoading.EMAIL_RESET
                                            AuthManager.sendPasswordReset(
                                                emailRaw = emailInput,
                                                scope = scope,
                                                onSent = { msg ->
                                                    activeLoading = ActiveLoginLoading.NONE
                                                    infoMessage = msg
                                                },
                                                onError = { msg ->
                                                    activeLoading = ActiveLoginLoading.NONE
                                                    errorMessage = msg
                                                }
                                            )
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("forgot_password_button")
                                    ) {
                                        Text(
                                            text = if (activeLoading == ActiveLoginLoading.EMAIL_RESET) "Sending..." else "Forgot password?",
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!emailSectionExpanded) {
                    AuthStatusBanners(
                        errorMessage = errorMessage,
                        infoMessage = infoMessage
                    )
                }

                // Primary login method: Google Sign-In (Jetpack Credential Manager)
                Button(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        activeLoading = ActiveLoginLoading.GOOGLE
                        AuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                activeLoading = ActiveLoginLoading.NONE
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                activeLoading = ActiveLoginLoading.NONE
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                activeLoading = ActiveLoginLoading.NONE
                                errorMessage = "Google Sign-In was cancelled or no Google account is available on this device. You can also sign in with Email."
                            }
                        )
                    },
                    enabled = !isBusy,
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
                    if (activeLoading == ActiveLoginLoading.GOOGLE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.16f)
                    )
                    Text(
                        text = "OR CONTINUE WITH",
                        modifier = Modifier.padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9DA9DA),
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.16f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                SecondaryProviderButton(
                    label = "Continue with Email",
                    icon = Icons.Rounded.Email,
                    selected = emailSectionExpanded,
                    isLoading = false,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_sign_in_button"),
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        emailSectionExpanded = !emailSectionExpanded
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
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
                    val projectId = remember { AuthManager.activeProjectId() }
                    Text(
                        text = if (projectId != null) {
                            "Firebase Auth • $projectId"
                        } else {
                            "Multi-provider Firebase Auth • Role-verified access"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9DA9DA)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthStatusBanners(
    errorMessage: String?,
    infoMessage: String?,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
    if (errorMessage != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            shape = RoundedCornerShape(14.dp),
            color = DangerRed.copy(alpha = 0.18f)
        ) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFC9C9),
                textAlign = TextAlign.Center
            )
        }
    }

    if (infoMessage != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            shape = RoundedCornerShape(14.dp),
            color = GreenStart.copy(alpha = 0.18f)
        ) {
            Text(
                text = infoMessage,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC8FFE0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = Color(0xFFC5CEF0),
    focusedPlaceholderColor = Color(0xFF8B97C7),
    unfocusedPlaceholderColor = Color(0xFF8B97C7),
    cursorColor = CyanAccent
)

@Composable
private fun SecondaryProviderButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) CyanAccent else Color.White.copy(alpha = 0.22f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) CyanAccent.copy(alpha = 0.14f) else NavyCard.copy(alpha = 0.72f),
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CyanAccent else Color(0xFFE7ECFF),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
