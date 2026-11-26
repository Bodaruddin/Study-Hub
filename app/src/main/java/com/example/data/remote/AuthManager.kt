package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class OAuthLoginProvider(val providerId: String, val displayName: String) {
    GITHUB("github.com", "GitHub"),
    MICROSOFT("microsoft.com", "Microsoft")
}

object AuthManager {
    private const val TAG = "Auth"

    fun authStateFlow(auth: FirebaseAuth = Firebase.auth): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun resolveDefaultWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = resolveDefaultWebClientId(context)
        if (clientId == null) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context.findActivity() ?: context, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val clientId = resolveDefaultWebClientId(context)
        if (clientId == null) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Sign-in requires an active screen context.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
            }
        }
    }

    fun sendPhoneVerificationCode(
        context: Context,
        phoneNumberRaw: String,
        scope: CoroutineScope,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val normalized = phoneNumberRaw.trim().replace(Regex("[\\s\\-()]"), "")
        if (!normalized.matches(Regex("^\\+[1-9]\\d{6,14}$"))) {
            onAuthError("Enter a valid phone number with country code (for example +919876543210).")
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Phone verification requires an active screen context.")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                scope.launch {
                    try {
                        Firebase.auth.signInWithCredential(credential).await()
                        onAuthSuccess()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Instant phone verification sign-in failed", e)
                        onAuthError(mapAuthException(e, "Phone"))
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "Phone verification failed: ${e.message}", e)
                onAuthError(mapAuthException(e, "Phone"))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId, token)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(normalized)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyPhoneOtpCode(
        verificationId: String,
        otpCode: String,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val cleanCode = otpCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            onAuthError("Enter the 6-digit OTP sent to your phone.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
        scope.launch {
            try {
                Firebase.auth.signInWithCredential(credential).await()
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "OTP verification failed", e)
                onAuthError(mapAuthException(e, "Phone"))
            }
        }
    }

    fun onOAuthProviderClicked(
        context: Context,
        provider: OAuthLoginProvider,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("${provider.displayName} sign-in requires an active screen context.")
            return
        }

        scope.launch {
            try {
                val pending = Firebase.auth.pendingAuthResult
                if (pending != null) {
                    pending.await()
                    onAuthSuccess()
                    return@launch
                }

                val builder = OAuthProvider.newBuilder(provider.providerId)
                if (provider == OAuthLoginProvider.MICROSOFT) {
                    builder.addCustomParameter("prompt", "select_account")
                }
                Firebase.auth.startActivityForSignInWithProvider(activity, builder.build()).await()
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "${provider.displayName} sign-in failed", e)
                onAuthError(mapAuthException(e, provider.displayName))
            }
        }
    }

    fun signInWithEmail(
        emailRaw: String,
        password: String,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val cleanEmail = emailRaw.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onAuthError("Please enter a valid email address.")
            return
        }
        if (password.isBlank()) {
            onAuthError("Please enter your password.")
            return
        }

        val credential = EmailAuthProvider.getCredential(cleanEmail, password)
        scope.launch {
            try {
                Firebase.auth.signInWithCredential(credential).await()
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Email sign-in failed", e)
                onAuthError(mapAuthException(e, "Email/Password"))
            }
        }
    }

    fun createAccountWithEmail(
        displayNameRaw: String,
        emailRaw: String,
        password: String,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit
    ) {
        val cleanName = displayNameRaw.trim()
        val cleanEmail = emailRaw.trim()
        if (cleanName.length < 2) {
            onAuthError("Please enter your full name (at least 2 characters).")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onAuthError("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            onAuthError("Password must be at least 6 characters long.")
            return
        }

        scope.launch {
            try {
                val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val createdUser = result.user
                if (createdUser != null) {
                    runCatching {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                        createdUser.updateProfile(profileUpdate).await()
                    }
                    runCatching {
                        createdUser.sendEmailVerification().await()
                    }
                }
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Email account creation failed", e)
                onAuthError(mapAuthException(e, "Email/Password"))
            }
        }
    }

    fun sendPasswordReset(
        emailRaw: String,
        scope: CoroutineScope,
        onSent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = emailRaw.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError("Enter your email address first so we can send a reset link.")
            return
        }

        scope.launch {
            try {
                Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
                onSent("Password reset email sent to $cleanEmail.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Password reset failed", e)
                onError(mapAuthException(e, "Email/Password"))
            }
        }
    }

    private fun mapAuthException(error: Throwable, providerLabel: String): String {
        if (error is FirebaseAuthUserCollisionException) {
            return "An account already exists with this email under a different login method. Sign in with your original provider."
        }
        if (error is FirebaseAuthException) {
            return when (error.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" -> {
                    val projectId = runCatching { Firebase.auth.app.options.projectId }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                    val projectTag = if (projectId != null) " (Project: $projectId)" else ""
                    if (providerLabel == "Email/Password") {
                        "Email/Password is blocked on Firebase$projectTag. Enable Email/Password in Authentication → Sign-in method, and ensure 'Enable create (sign-up)' is checked under Authentication → Settings → User actions."
                    } else {
                        "$providerLabel sign-in is not enabled in Firebase Console$projectTag (Authentication → Sign-in method)."
                    }
                }
                "ERROR_INVALID_EMAIL" ->
                    "Please enter a valid email address."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                    "Incorrect email or password. Please check and try again."
                "ERROR_USER_NOT_FOUND" ->
                    "No account found with this email. Switch to Create Account to sign up."
                "ERROR_EMAIL_ALREADY_IN_USE" ->
                    "An account already exists with this email. Switch to Sign In instead."
                "ERROR_WEAK_PASSWORD" ->
                    "Password is too weak. Use at least 6 characters."
                "ERROR_USER_DISABLED" ->
                    "This account has been disabled. Contact support for assistance."
                "ERROR_INVALID_PHONE_NUMBER" ->
                    "Invalid phone number. Include the country code (e.g. +919876543210)."
                "ERROR_INVALID_VERIFICATION_CODE" ->
                    "Incorrect OTP code. Please check the SMS and try again."
                "ERROR_SESSION_EXPIRED" ->
                    "OTP expired. Please request a new verification code."
                "ERROR_TOO_MANY_REQUESTS", "ERROR_QUOTA_EXCEEDED" ->
                    "Too many sign-in attempts right now. Please wait and try again."
                "ERROR_WEB_CONTEXT_CANCELED", "ERROR_USER_CANCELLED" ->
                    "$providerLabel sign-in was cancelled."
                else -> error.localizedMessage ?: "$providerLabel sign-in failed. Please try again."
            }
        }
        return error.localizedMessage ?: "$providerLabel sign-in failed. Please try again."
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onBeforeFirebaseSignOut: suspend () -> Unit = {},
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    onBeforeFirebaseSignOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Pre-signout cleanup error: ${e.message}")
                }
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
