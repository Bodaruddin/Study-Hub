package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.PaperAttachmentChunk
import com.example.data.model.PaperAttachmentDraft
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AttachmentMediaHelper {
    private const val MAX_PDF_BYTES = 5_000_000 // ~11 chunks max (well within 20-chunk rule)
    private const val MAX_DECODED_BYTES = 6_000_000L
    private const val MAX_CHUNKS = 20
    private const val CHUNK_BINARY_BYTES = 450_000 // ~600k Base64 chars (< 750k limit)
    private const val MAX_IMAGE_DIM_PX = 1280

    private fun mediaRootDir(context: Context): File =
        File(context.filesDir, "paper_media").apply { mkdirs() }

    private fun draftsDir(context: Context): File =
        requireWithinDir(mediaRootDir(context), File(mediaRootDir(context), "drafts")).apply { mkdirs() }

    fun mediaDirForParent(context: Context, parentId: String): File {
        val root = mediaRootDir(context)
        val safeParent = sanitizeSegment(parentId)
        return requireWithinDir(root, File(root, safeParent)).apply { mkdirs() }
    }

    fun sanitizeSegment(raw: String): String {
        val cleaned = raw
            .trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("\\.{2,}"), "_")
            .trimStart('.', '_')
        return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") "file" else cleaned.take(120)
    }

    internal fun requireWithinDir(baseDir: File, target: File): File {
        val baseCanonical = baseDir.canonicalFile
        val targetCanonical = target.canonicalFile
        val basePrefix = if (baseCanonical.path.endsWith(File.separator)) {
            baseCanonical.path
        } else {
            baseCanonical.path + File.separator
        }
        require(targetCanonical.path.startsWith(basePrefix)) {
            "Blocked path outside attachment storage directory."
        }
        return targetCanonical
    }

    internal fun hasPdfHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        val scanLimit = min(bytes.size, 1024)
        val header = String(bytes, 0, scanLimit, Charsets.US_ASCII)
        return header.contains("%PDF-")
    }

    private fun queryDisplayName(context: Context, uri: Uri, fallback: String): String {
        val resolver = context.contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else null
            }
        }.getOrNull() ?: fallback
    }

    suspend fun preparePdfDraft(context: Context, uri: Uri): Result<PaperAttachmentDraft> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val rawName = queryDisplayName(context, uri, "question_paper.pdf")
                val safeName = if (rawName.endsWith(".pdf", ignoreCase = true)) rawName else "$rawName.pdf"
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(16_384)
                    var total = 0
                    while (true) {
                        val read = input.read(chunk)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_PDF_BYTES) {
                            throw IllegalArgumentException("PDF exceeds 5 MB limit. Please select a smaller or compressed PDF.")
                        }
                        buffer.write(chunk, 0, read)
                    }
                    buffer.toByteArray()
                } ?: throw IllegalStateException("Unable to read the selected PDF file.")

                if (bytes.isEmpty()) {
                    throw IllegalStateException("Selected PDF file is empty.")
                }
                if (!hasPdfHeader(bytes)) {
                    throw IllegalArgumentException("Selected file is not a valid PDF document.")
                }

                val groupId = "att_pdf_${System.currentTimeMillis()}"
                val drafts = draftsDir(context)
                val outFile = requireWithinDir(drafts, File(drafts, "${groupId}_${sanitizeSegment(safeName)}"))
                outFile.writeBytes(bytes)

                PaperAttachmentDraft(
                    groupId = groupId,
                    kind = "pdf",
                    fileName = safeName.take(160),
                    mimeType = "application/pdf",
                    sizeBytes = bytes.size.toLong(),
                    previewFile = outFile,
                    isExistingRemote = false
                )
            }
        }

    suspend fun prepareImageDraft(context: Context, uri: Uri, indexSuffix: Int = 0): Result<PaperAttachmentDraft> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val rawName = queryDisplayName(context, uri, "page_${indexSuffix + 1}.jpg")
                val baseName = rawName.substringBeforeLast('.').ifBlank { "page_${indexSuffix + 1}" }
                val safeName = "${baseName.take(120)}.jpg"

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    ?: throw IllegalStateException("Unable to open selected image.")

                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IllegalStateException("Unsupported or corrupted image file.")
                }

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > MAX_IMAGE_DIM_PX * 2 ||
                    bounds.outHeight / sampleSize > MAX_IMAGE_DIM_PX * 2
                ) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: throw IllegalStateException("Could not decode image.")

                val scaled = scaleBitmapIfNeeded(decoded, MAX_IMAGE_DIM_PX)
                if (scaled !== decoded) decoded.recycle()

                var quality = 82
                var jpegBytes: ByteArray
                do {
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    jpegBytes = baos.toByteArray()
                    quality -= 10
                } while (jpegBytes.size > 380_000 && quality >= 50)

                scaled.recycle()

                val groupId = "att_img_${System.currentTimeMillis()}_$indexSuffix"
                val drafts = draftsDir(context)
                val outFile = requireWithinDir(drafts, File(drafts, "${groupId}_${sanitizeSegment(safeName)}"))
                outFile.writeBytes(jpegBytes)

                PaperAttachmentDraft(
                    groupId = groupId,
                    kind = "image",
                    fileName = safeName,
                    mimeType = "image/jpeg",
                    sizeBytes = jpegBytes.size.toLong(),
                    previewFile = outFile,
                    isExistingRemote = false
                )
            }
        }

    private fun scaleBitmapIfNeeded(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxDim) return source
        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = max(1, (w * ratio).toInt())
        val targetH = max(1, (h * ratio).toInt())
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    fun chunkFileToBase64(file: File): List<String> {
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return emptyList()
        val chunks = ArrayList<String>()
        var offset = 0
        while (offset < bytes.size) {
            val end = min(bytes.size, offset + CHUNK_BINARY_BYTES)
            val slice = bytes.copyOfRange(offset, end)
            chunks.add(Base64.encodeToString(slice, Base64.NO_WRAP))
            offset = end
        }
        return chunks
    }

    fun decodeChunksToFile(
        context: Context,
        parentId: String,
        groupId: String,
        fileName: String,
        chunks: List<PaperAttachmentChunk>
    ): File {
        require(chunks.isNotEmpty() && chunks.size <= MAX_CHUNKS) {
            "Attachment chunk count out of allowed range."
        }
        val targetDir = mediaDirForParent(context, parentId)
        val outFile = requireWithinDir(
            targetDir,
            File(targetDir, "${sanitizeSegment(groupId)}_${sanitizeSegment(fileName)}")
        )
        if (outFile.exists() && outFile.length() > 0L) {
            return outFile
        }
        val sorted = chunks.sortedBy { it.chunkIndex }
        var totalWritten = 0L
        try {
            outFile.outputStream().use { fos ->
                sorted.forEach { chunk ->
                    val decoded = Base64.decode(chunk.dataBase64, Base64.DEFAULT)
                    totalWritten += decoded.size
                    if (totalWritten > MAX_DECODED_BYTES) {
                        throw IllegalStateException("Attachment exceeds maximum allowed decoded size.")
                    }
                    fos.write(decoded)
                }
            }
        } catch (e: Exception) {
            runCatching { outFile.delete() }
            throw e
        }
        return outFile
    }

    fun copyDraftToParentCache(
        context: Context,
        parentId: String,
        draft: PaperAttachmentDraft
    ): File {
        val targetDir = mediaDirForParent(context, parentId)
        val outFile = requireWithinDir(
            targetDir,
            File(targetDir, "${sanitizeSegment(draft.groupId)}_${sanitizeSegment(draft.fileName)}")
        )
        if (draft.previewFile.canonicalPath != outFile.canonicalPath && draft.previewFile.exists()) {
            draft.previewFile.copyTo(outFile, overwrite = true)
        }
        return outFile
    }

    fun deleteParentMediaCache(context: Context, parentId: String) {
        if (parentId.isBlank()) return
        runCatching {
            val root = mediaRootDir(context)
            val safeParent = sanitizeSegment(parentId)
            val dir = requireWithinDir(root, File(root, safeParent))
            if (dir != root.canonicalFile && dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    fun formatBytesLabel(bytes: Long): String = when {
        bytes <= 0L -> "0 KB"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${max(1L, bytes / 1024L)} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun buildAttachmentSummaryLabel(
        drafts: List<PaperAttachmentDraft>,
        fallback: String
    ): String {
        if (drafts.isEmpty()) return fallback
        val pdfCount = drafts.count { it.kind == "pdf" }
        val imgCount = drafts.count { it.kind == "image" }
        val totalBytes = drafts.sumOf { it.sizeBytes }
        val parts = buildList {
            if (pdfCount > 0) add(if (pdfCount == 1) "PDF" else "$pdfCount PDFs")
            if (imgCount > 0) add(if (imgCount == 1) "1 Image" else "$imgCount Images")
        }.joinToString(" + ")
        return "$parts • ${formatBytesLabel(totalBytes)}".take(40)
    }

    suspend fun loadBitmapForDisplay(file: File, maxDim: Int = 1400): Bitmap? =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = 1
                while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(file.absolutePath, opts)
            }.getOrNull()
        }

    suspend fun renderPdfPage(
        file: File,
        pageIndex: Int,
        targetWidthPx: Int = 1100
    ): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null to 0
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) return@use null to 0
                    val safeIdx = pageIndex.coerceIn(0, count - 1)
                    renderer.openPage(safeIdx).use { page ->
                        val ratio = targetWidthPx.toFloat() / max(1, page.width).toFloat()
                        val height = max(1, (page.height * ratio).toInt())
                        val bitmap = Bitmap.createBitmap(targetWidthPx, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap to count
                    }
                }
            }
        }.getOrElse { null to 0 }
    }
}
