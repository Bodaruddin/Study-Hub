package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.remote.SupabaseSchemaSql
import com.example.data.remote.SupabaseTestResult
import com.example.ui.components.Tactile3DCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.GreenEnd
import com.example.ui.theme.GreenStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.OrangeStart
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleStart
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun AdminSupabaseSetupDialog(
    status: SupabaseConnectionStatus,
    onDismiss: () -> Unit,
    onTestConnection: suspend (String, String) -> SupabaseTestResult,
    onSaveConfig: suspend (String, String) -> Result<SupabaseTestResult>,
    onClearConfig: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlInput by remember(status.savedUrl) { mutableStateOf(status.savedUrl) }
    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var showSqlPreview by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<SupabaseTestResult?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = ElectricPurple,
        unfocusedBorderColor = NavyCardBorder,
        focusedLabelColor = Color(0xFFC5A6FF),
        unfocusedLabelColor = Color(0xFF9AA5D1),
        focusedPlaceholderColor = Color(0xFF7B88B8),
        unfocusedPlaceholderColor = Color(0xFF7B88B8),
        cursorColor = ElectricPurple
    )

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = NavySurface,
        titleContentColor = Color.White,
        textContentColor = Color(0xFFDCE4FF),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricPurple.copy(alpha = 0.24f))
                        .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = Color(0xFFC5A6FF), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Setup Supabase Database",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Manual Admin Configuration",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9AA5D1)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (status.configured) SuccessGreen.copy(alpha = 0.14f) else OrangeStart.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (status.configured) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (status.configured) SuccessGreen else OrangeStart,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (status.configured) {
                                    "Active: ${status.hostLabel ?: "Connected"}"
                                } else {
                                    "Supabase Not Connected Yet"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (status.configured) {
                                    "Source: ${status.sourceLabel} • Key: ${status.savedKeyPreview}"
                                } else {
                                    "Enter your Project URL & Anon Key from Supabase Settings → API."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC5CEF0)
                            )
                        }
                    }
                }

                Text(
                    text = "1. Supabase Project Credentials",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        validationError = null
                    },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://your-project.supabase.co") },
                    singleLine = true,
                    enabled = !isBusy,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url_input")
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        validationError = null
                    },
                    label = { Text("Supabase Anon / Public Key") },
                    placeholder = {
                        Text(
                            if (status.hasManualOverride) {
                                "Saved (${status.savedKeyPreview}) — paste to replace"
                            } else {
                                "eyJhbGciOi..."
                            }
                        )
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { keyVisible = !keyVisible },
                            modifier = Modifier.testTag("supabase_toggle_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (keyVisible) "Hide key" else "Show key",
                                tint = Color(0xFFC5CEF0)
                            )
                        }
                    },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_anon_key_input")
                )

                Text(
                    text = "2. Create Tables in Supabase SQL Editor",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NavyCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Copy the SQL below and run it in Supabase Dashboard → SQL Editor to create all 15 StudyHub tables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC5CEF0)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("StudyHub Supabase SQL", SupabaseSchemaSql.SCHEMA_SQL))
                                    onShowMessage("Supabase SQL schema copied to clipboard")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("copy_supabase_sql_btn")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy SQL", color = CyanAccent, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { showSqlPreview = !showSqlPreview },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("toggle_sql_preview_btn")
                            ) {
                                Icon(
                                    imageVector = if (showSqlPreview) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (showSqlPreview) "Hide SQL" else "View SQL",
                                    tint = Color(0xFFC5A6FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showSqlPreview) "Hide" else "View", color = Color(0xFFC5A6FF))
                            }
                        }

                        AnimatedVisibility(visible = showSqlPreview) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 170.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF070B1E))
                                    .border(1.dp, NavyCardBorder, RoundedCornerShape(10.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = SupabaseSchemaSql.SCHEMA_SQL,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = Color(0xFFB7C6F7)
                                )
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerRed.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFFFB4B4), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD0D0))
                        }
                    }
                }

                testResult?.let { res ->
                    val bgColor = when {
                        res.connected && res.schemaReady -> SuccessGreen.copy(alpha = 0.18f)
                        res.connected -> OrangeStart.copy(alpha = 0.18f)
                        else -> DangerRed.copy(alpha = 0.18f)
                    }
                    val iconTint = when {
                        res.connected && res.schemaReady -> SuccessGreen
                        res.connected -> OrangeStart
                        else -> DangerRed
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.connected && res.schemaReady) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(res.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            isBusy = true
                            scope.launch {
                                testResult = onTestConnection(urlInput, keyInput)
                                isBusy = false
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("test_supabase_connection_btn")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFC5A6FF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Tactile3DCard(
                        faceBrush = Brush.linearGradient(listOf(GreenStart, GreenEnd)),
                        depthColor = Color(0xFF045E34),
                        shadowColor = GreenEnd,
                        cornerRadius = 14.dp,
                        depthHeight = 3.dp,
                        floatingLift = 2.5.dp,
                        glassGlossAlpha = 0.9f,
                        onClick = {
                            if (isBusy) return@Tactile3DCard
                            if (keyInput.isBlank() && status.hasManualOverride && urlInput.trim().equals(status.savedUrl, ignoreCase = true)) {
                                validationError = null
                                isBusy = true
                                scope.launch {
                                    val res = onTestConnection(urlInput, "")
                                    testResult = res
                                    isBusy = false
                                    if (res.connected && res.schemaReady) {
                                        onShowMessage("Supabase verified and connected!")
                                        onDismiss()
                                    }
                                }
                                return@Tactile3DCard
                            }
                            if (urlInput.isBlank() || keyInput.isBlank()) {
                                validationError = "Enter both Supabase Project URL and Anon Key."
                                return@Tactile3DCard
                            }
                            validationError = null
                            isBusy = true
                            scope.launch {
                                val res = onSaveConfig(urlInput, keyInput)
                                isBusy = false
                                res.onSuccess { test ->
                                    testResult = test
                                    if (test.connected && test.schemaReady) {
                                        onDismiss()
                                    }
                                }.onFailure { err ->
                                    validationError = err.message ?: "Invalid Supabase credentials"
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_supabase_config_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save & Connect",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (status.hasManualOverride) {
                    TextButton(
                        enabled = !isBusy,
                        onClick = {
                            onClearConfig()
                            urlInput = ""
                            keyInput = ""
                            testResult = null
                        },
                        modifier = Modifier.testTag("clear_supabase_config_btn")
                    ) {
                        Text("Reset", color = DangerRed)
                    }
                }
                TextButton(
                    enabled = !isBusy,
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_supabase_setup_btn")
                ) {
                    Text("Close", color = Color(0xFFC5CEF0))
                }
            }
        },
        confirmButton = {}
    )
}
