package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Sms
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
import com.example.data.remote.OAuthLoginProvider
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
import com.google.firebase.auth.PhoneAuthProvider

private enum class ActiveLoginLoading {
    NONE,
    GOOGLE,
    EMAIL,
    EMAIL_RESET,
    PHONE_SEND,
    PHONE_VERIFY,
    GITHUB,
    MICROSOFT
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

    var phoneSectionExpanded by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    val isBusy = activeLoading != ActiveLoginLoading.NONE

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
                Spacer(modifier = Modifier.height(8.dp))
                StudyHubBrandLogo(subtitle = "Learn. Practice. Achieve.", isDarkSurface = true)
                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(144.dp)
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

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Classes 6–12\nSmart Learning Companion",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!emailSectionExpanded && !phoneSectionExpanded) {
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
                                errorMessage = "Google Sign-In was cancelled or no Google account is available on this device. You can also sign in with Phone OTP, GitHub, or Microsoft below."
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryProviderButton(
                            label = "Email",
                            icon = Icons.Rounded.Email,
                            selected = emailSectionExpanded,
                            isLoading = false,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("email_sign_in_button"),
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                emailSectionExpanded = !emailSectionExpanded
                                if (emailSectionExpanded) {
                                    phoneSectionExpanded = false
                                }
                            }
                        )
                        SecondaryProviderButton(
                            label = "Phone OTP",
                            icon = Icons.Rounded.PhoneAndroid,
                            selected = phoneSectionExpanded,
                            isLoading = false,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_sign_in_button"),
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                phoneSectionExpanded = !phoneSectionExpanded
                                if (phoneSectionExpanded) {
                                    emailSectionExpanded = false
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryProviderButton(
                            label = "GitHub",
                            icon = Icons.Rounded.Code,
                            selected = false,
                            isLoading = activeLoading == ActiveLoginLoading.GITHUB,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("github_sign_in_button"),
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                activeLoading = ActiveLoginLoading.GITHUB
                                AuthManager.onOAuthProviderClicked(
                                    context = context,
                                    provider = OAuthLoginProvider.GITHUB,
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
                        )
                        SecondaryProviderButton(
                            label = "Microsoft",
                            icon = Icons.Rounded.GridView,
                            selected = false,
                            isLoading = activeLoading == ActiveLoginLoading.MICROSOFT,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("microsoft_sign_in_button"),
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                activeLoading = ActiveLoginLoading.MICROSOFT
                                AuthManager.onOAuthProviderClicked(
                                    context = context,
                                    provider = OAuthLoginProvider.MICROSOFT,
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
                        )
                    }
                }

                AnimatedVisibility(visible = emailSectionExpanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
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

                AnimatedVisibility(visible = phoneSectionExpanded) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = NavyCard.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Sms,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (verificationId == null) {
                                        "Sign in with Phone Number"
                                    } else {
                                        "Verify 6-Digit SMS Code"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (verificationId == null) {
                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("Phone number with country code") },
                                    placeholder = { Text("+91 9876543210") },
                                    singleLine = true,
                                    enabled = !isBusy,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("phone_number_input")
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
                                        activeLoading = ActiveLoginLoading.PHONE_SEND
                                        AuthManager.sendPhoneVerificationCode(
                                            context = context,
                                            phoneNumberRaw = phoneInput,
                                            scope = scope,
                                            onCodeSent = { verId, token ->
                                                activeLoading = ActiveLoginLoading.NONE
                                                verificationId = verId
                                                resendToken = token
                                                otpInput = ""
                                                infoMessage = "Verification code sent to ${phoneInput.trim()}."
                                            },
                                            onAuthSuccess = {
                                                activeLoading = ActiveLoginLoading.NONE
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                activeLoading = ActiveLoginLoading.NONE
                                                errorMessage = msg
                                            }
                                        )
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("send_otp_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanAccent,
                                        contentColor = NavyDeep
                                    )
                                ) {
                                    if (activeLoading == ActiveLoginLoading.PHONE_SEND) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = NavyDeep,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Send OTP", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { next ->
                                        if (next.length <= 6 && next.all { it.isDigit() }) {
                                            otpInput = next
                                        }
                                    },
                                    label = { Text("6-digit OTP code") },
                                    placeholder = { Text("123456") },
                                    singleLine = true,
                                    enabled = !isBusy,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input")
                                )

                                AuthStatusBanners(
                                    errorMessage = errorMessage,
                                    infoMessage = infoMessage,
                                    bottomPadding = 0.dp
                                )

                                Button(
                                    onClick = {
                                        val currentVerId = verificationId ?: return@Button
                                        errorMessage = null
                                        infoMessage = null
                                        activeLoading = ActiveLoginLoading.PHONE_VERIFY
                                        AuthManager.verifyPhoneOtpCode(
                                            verificationId = currentVerId,
                                            otpCode = otpInput,
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
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("verify_otp_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenStart,
                                        contentColor = NavyDeep
                                    )
                                ) {
                                    if (activeLoading == ActiveLoginLoading.PHONE_VERIFY) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = NavyDeep,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Verify & Sign In", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = {
                                            verificationId = null
                                            otpInput = ""
                                            errorMessage = null
                                            infoMessage = null
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("change_phone_button")
                                    ) {
                                        Text("Change number", color = Color(0xFFC5CEF0))
                                    }

                                    TextButton(
                                        onClick = {
                                            errorMessage = null
                                            infoMessage = null
                                            activeLoading = ActiveLoginLoading.PHONE_SEND
                                            AuthManager.sendPhoneVerificationCode(
                                                context = context,
                                                phoneNumberRaw = phoneInput,
                                                scope = scope,
                                                resendToken = resendToken,
                                                onCodeSent = { verId, token ->
                                                    activeLoading = ActiveLoginLoading.NONE
                                                    verificationId = verId
                                                    resendToken = token
                                                    infoMessage = "New OTP sent to ${phoneInput.trim()}."
                                                },
                                                onAuthSuccess = {
                                                    activeLoading = ActiveLoginLoading.NONE
                                                    onAuthSuccess()
                                                },
                                                onAuthError = { msg ->
                                                    activeLoading = ActiveLoginLoading.NONE
                                                    errorMessage = msg
                                                }
                                            )
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("resend_otp_button")
                                    ) {
                                        Text("Resend OTP", color = CyanAccent)
                                    }
                                }
                            }
                        }
                    }
                }

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
