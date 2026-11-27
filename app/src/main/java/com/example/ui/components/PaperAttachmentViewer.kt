package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.remote.AttachmentMediaHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.IndigoHeaderStart
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OutfitFontFamily

@Composable
fun AdminAttachmentDraftStrip(
    drafts: List<PaperAttachmentDraft>,
    onRemoveDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (drafts.isEmpty()) return
    val pdfs = drafts.filter { it.kind == "pdf" }
    val images = drafts.filter { it.kind == "image" }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pdfs.forEach { pdf ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "PDF attachment",
                        tint = DangerRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pdf.fileName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = AttachmentMediaHelper.formatBytesLabel(pdf.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onRemoveDraft(pdf.groupId) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("remove_draft_${pdf.groupId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove PDF",
                            tint = DangerRed
                        )
                    }
                }
            }
        }

        if (images.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                images.forEach { img ->
                    var bitmap by remember(img.groupId, img.previewFile.absolutePath) {
                        mutableStateOf<Bitmap?>(null)
                    }
                    LaunchedEffect(img.groupId, img.previewFile.absolutePath) {
                        bitmap = AttachmentMediaHelper.loadBitmapForDisplay(img.previewFile, maxDim = 320)
                    }
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        val bmp = bitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = img.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = img.fileName,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(28.dp),
                                tint = ElectricPurple
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.68f))
                                .clickable { onRemoveDraft(img.groupId) }
                                .testTag("remove_draft_${img.groupId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaperAttachmentGallery(
    parentId: String,
    onLoadAttachments: suspend (String) -> Result<List<ResolvedPaperAttachment>>,
    modifier: Modifier = Modifier
) {
    var isLoading by remember(parentId) { mutableStateOf(true) }
    var errorMessage by remember(parentId) { mutableStateOf<String?>(null) }
    var attachments by remember(parentId) { mutableStateOf<List<ResolvedPaperAttachment>>(emptyList()) }
    var reloadNonce by remember(parentId) { mutableIntStateOf(0) }

    var activePdf by remember { mutableStateOf<ResolvedPaperAttachment?>(null) }
    var activeImage by remember { mutableStateOf<ResolvedPaperAttachment?>(null) }

    LaunchedEffect(parentId, reloadNonce) {
        isLoading = true
        errorMessage = null
        val res = onLoadAttachments(parentId)
        res.onSuccess { list ->
            attachments = list
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.message ?: "Unable to load attachments"
            isLoading = false
        }
    }

    if (activePdf != null) {
        PdfPageReaderDialog(
            attachment = activePdf!!,
            onDismiss = { activePdf = null }
        )
    }
    if (activeImage != null) {
        ZoomableImageDialog(
            attachment = activeImage!!,
            onDismiss = { activeImage = null }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ElectricPurple
                )
                Text(
                    text = "Loading attached PDF & images...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "Failed to load attachments",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { reloadNonce++ }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry")
                }
            }
        } else if (attachments.isNotEmpty()) {
            val pdfs = attachments.filter { it.kind == "pdf" }
            val images = attachments.filter { it.kind == "image" }

            Text(
                text = "Attached PDF & Image Files",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricPurple
            )

            pdfs.forEach { pdf ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activePdf = pdf }
                        .testTag("open_pdf_${pdf.groupId}"),
                    shape = RoundedCornerShape(16.dp),
                    color = ElectricPurple.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DangerRed.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = "Open PDF",
                                tint = DangerRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pdf.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to read PDF • ${AttachmentMediaHelper.formatBytesLabel(pdf.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricPurple
                        ) {
                            Text(
                                text = "Open",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (images.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    images.forEach { img ->
                        var bmp by remember(img.groupId, img.localFile.absolutePath) {
                            mutableStateOf<Bitmap?>(null)
                        }
                        LaunchedEffect(img.groupId, img.localFile.absolutePath) {
                            bmp = AttachmentMediaHelper.loadBitmapForDisplay(img.localFile, maxDim = 500)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 132.dp, height = 160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { activeImage = img }
                                .testTag("open_image_${img.groupId}")
                        ) {
                            val loaded = bmp
                            if (loaded != null) {
                                Image(
                                    bitmap = loaded.asImageBitmap(),
                                    contentDescription = img.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.Center),
                                    strokeWidth = 2.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = "Zoom image",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "View",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageReaderDialog(
    attachment: ResolvedPaperAttachment,
    onDismiss: () -> Unit
) {
    var currentPage by remember(attachment.groupId) { mutableIntStateOf(0) }
    var pageCount by remember(attachment.groupId) { mutableIntStateOf(1) }
    var renderedBitmap by remember(attachment.groupId) { mutableStateOf<Bitmap?>(null) }
    var rendering by remember(attachment.groupId) { mutableStateOf(true) }

    var scale by remember(attachment.groupId, currentPage) { mutableFloatStateOf(1f) }
    var offset by remember(attachment.groupId, currentPage) { mutableStateOf(Offset.Zero) }
    var isImmersive by remember(attachment.groupId) { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.groupId, currentPage) {
        rendering = true
        val (bmp, total) = AttachmentMediaHelper.renderPdfPage(
            file = attachment.localFile,
            pageIndex = currentPage,
            targetWidthPx = 1440
        )
        renderedBitmap = bmp
        if (total > 0) pageCount = total
        rendering = false
    }

    if (showPageJumpDialog) {
        AlertDialog(
            onDismissRequest = { showPageJumpDialog = false },
            containerColor = NavyCard,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFD9E1FF),
            title = {
                Text(
                    text = "Jump to Page",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pages = (0 until pageCount).toList()
                    pages.chunked(4).forEach { rowPages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPages.forEach { pageIdx ->
                                val selected = pageIdx == currentPage
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            currentPage = pageIdx
                                            showPageJumpDialog = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) ElectricPurple else Color.White.copy(alpha = 0.09f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) CyanAccent else Color.White.copy(alpha = 0.14f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${pageIdx + 1}",
                                            color = Color.White,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                            repeat(4 - rowPages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageJumpDialog = false }) {
                    Text("Cancel", color = CyanAccent)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            color = NavyDeep
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(IndigoHeaderStart, NavyDeep, Color(0xFF070A1A))
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = !isImmersive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IndigoHeaderStart, NavyCard)
                                    )
                                )
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFF5E66), Color(0xFFDC1F32))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attachment.fileName,
                                        fontFamily = OutfitFontFamily,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Page ${currentPage + 1} of $pageCount • ${AttachmentMediaHelper.formatBytesLabel(attachment.sizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB5C1EB),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (scale > 1.05f) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                scale = 1f
                                                offset = Offset.Zero
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = CyanAccent.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomOutMap,
                                                contentDescription = "Reset zoom",
                                                tint = CyanAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Fit",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { isImmersive = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Full screen view",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .testTag("close_pdf_reader")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close PDF reader",
                                        tint = Color.White
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { ((currentPage + 1).toFloat() / pageCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = ElectricPurple,
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(
                                if (isImmersive) {
                                    Modifier.statusBarsPadding().navigationBarsPadding()
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = if (isImmersive) 0.dp else 4.dp, vertical = if (isImmersive) 0.dp else 6.dp)
                            .pointerInput(currentPage, isImmersive) {
                                detectTapGestures(
                                    onTap = {
                                        if (isImmersive) isImmersive = false
                                    },
                                    onDoubleTap = {
                                        if (scale > 1.15f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.2f
                                        }
                                    }
                                )
                            }
                            .pointerInput(currentPage) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = nextScale
                                    offset = if (nextScale > 1.02f) offset + pan else Offset.Zero
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = renderedBitmap
                        if (rendering && bmp == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = CyanAccent)
                                Text(
                                    text = "Rendering page ${currentPage + 1}...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFB5C1EB)
                                )
                            }
                        } else if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF page ${currentPage + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shadow(12.dp, RoundedCornerShape(if (isImmersive) 0.dp else 10.dp))
                                        .clip(RoundedCornerShape(if (isImmersive) 0.dp else 10.dp))
                                )
                            }

                            if (rendering) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp),
                                    shape = RoundedCornerShape(50),
                                    color = NavyCard.copy(alpha = 0.88f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = CyanAccent
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading page ${currentPage + 1}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Unable to render this PDF page.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DangerRed,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    AnimatedVisibility(visible = !isImmersive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 22.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = NavyCard.copy(alpha = 0.96f),
                                shadowElevation = 14.dp,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val hasPrev = currentPage > 0
                                    val hasNext = currentPage + 1 < pageCount

                                    Button(
                                        onClick = { if (hasPrev) currentPage-- },
                                        enabled = hasPrev,
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.14f),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("pdf_prev_page")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Previous page",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Previous",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = pageCount > 1) { showPageJumpDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.09f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${currentPage + 1} / $pageCount",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { if (hasNext) currentPage++ },
                                        enabled = hasNext,
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricPurple,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("pdf_next_page")
                                    ) {
                                        Text(
                                            text = "Next",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = "Next page",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isImmersive) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(14.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { isImmersive = false },
                        shape = RoundedCornerShape(50),
                        color = NavyCard.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FullscreenExit,
                                contentDescription = "Show controls",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Controls",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImageDialog(
    attachment: ResolvedPaperAttachment,
    onDismiss: () -> Unit
) {
    var bitmap by remember(attachment.groupId) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(attachment.groupId) { mutableFloatStateOf(1f) }
    var offset by remember(attachment.groupId) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(attachment.groupId, attachment.localFile.absolutePath) {
        bitmap = AttachmentMediaHelper.loadBitmapForDisplay(attachment.localFile, maxDim = 1600)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            color = Color.Black.copy(alpha = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val bmp = bitmap
                if (bmp == null) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = attachment.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = nextScale
                                    offset = if (nextScale > 1f) offset + pan else Offset.Zero
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachment.fileName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_image_viewer")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close image preview",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
