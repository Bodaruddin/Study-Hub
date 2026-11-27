package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PaperAttachmentDraft
import com.example.data.model.ResolvedPaperAttachment
import com.example.data.remote.AttachmentMediaHelper
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricPurple

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
                text = "Attached Question Paper Files",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricPurple
            )

            pdfs.forEach { pdf ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { activePdf = pdf }
                        .testTag("open_pdf_${pdf.groupId}"),
                    shape = RoundedCornerShape(14.dp),
                    color = ElectricPurple.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "Open PDF",
                            tint = DangerRed,
                            modifier = Modifier.size(28.dp)
                        )
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
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricPurple
                        )
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

    LaunchedEffect(attachment.groupId, currentPage) {
        rendering = true
        val (bmp, total) = AttachmentMediaHelper.renderPdfPage(
            file = attachment.localFile,
            pageIndex = currentPage,
            targetWidthPx = 1200
        )
        renderedBitmap = bmp
        if (total > 0) pageCount = total
        rendering = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $pageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_pdf_reader")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close PDF reader")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = renderedBitmap
                    if (rendering && bmp == null) {
                        CircularProgressIndicator(color = ElectricPurple)
                    } else if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF page ${currentPage + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text(
                            text = "Unable to render this PDF page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DangerRed
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.testTag("pdf_prev_page")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous page")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                    Text(
                        text = "${currentPage + 1} / $pageCount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { if (currentPage + 1 < pageCount) currentPage++ },
                        enabled = currentPage + 1 < pageCount,
                        modifier = Modifier.testTag("pdf_next_page")
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next page")
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.94f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        .padding(16.dp),
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
