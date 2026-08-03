package com.mohnishraj.lunara.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.MediaStorageSnapshot
import com.mohnishraj.lunara.domain.MediaTransferState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MediaAttachmentStore(private val context: Context) {
    private val outgoingDir = File(context.filesDir, "lunara_media/outgoing").apply { mkdirs() }
    private val downloadDir = File(context.cacheDir, "lunara_media/downloads").apply { mkdirs() }
    private val cameraDir = File(context.cacheDir, "lunara_media/camera").apply { mkdirs() }

    suspend fun import(uriText: String, kind: MediaKind, caption: String = ""): Result<MediaAttachment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriText)
                when (kind) {
                    MediaKind.Image -> importImage(uri, caption)
                    MediaKind.Document -> importDocument(uri, caption)
                    MediaKind.Voice -> error("Voice notes are created by the recorder")
                }.validateForSend().getOrThrow()
            }
        }

    fun createCameraCapture(): CameraCapture {
        cameraDir.mkdirs()
        val file = File(cameraDir, "camera-${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return CameraCapture(uri.toString(), file.absolutePath)
    }

    suspend fun importCamera(path: String, caption: String = ""): Result<MediaAttachment> =
        withContext(Dispatchers.IO) {
            val source = File(path)
            try {
                runCatching { importImage(Uri.fromFile(source), caption).validateForSend().getOrThrow() }
            } finally {
                source.delete()
            }
        }

    suspend fun voiceAttachment(
        file: File,
        durationMs: Long,
        waveform: List<Int>,
        caption: String = "",
    ): Result<MediaAttachment> = withContext(Dispatchers.IO) {
        var destination: File? = null
        runCatching {
            require(file.isFile && file.length() > 0L) { "Voice recording is empty" }
            destination = File(outgoingDir, "voice-${UUID.randomUUID()}.m4a")
            val target = requireNotNull(destination)
            file.copyTo(target, overwrite = true)
            MediaAttachment(
                id = UUID.randomUUID().toString(),
                kind = MediaKind.Voice,
                fileName = target.name,
                mimeType = "audio/mp4",
                sizeBytes = target.length(),
                caption = caption,
                localUri = Uri.fromFile(target).toString(),
                durationMs = durationMs,
                waveform = normalizeWaveform(waveform),
            ).validateForSend().getOrThrow()
        }.onSuccess {
            if (file.absolutePath != destination?.absolutePath) file.delete()
        }.onFailure {
            destination?.delete()
            file.delete()
        }
    }

    suspend fun saveDownload(
        attachment: MediaAttachment,
        input: InputStream,
        onBytesCopied: (Long) -> Unit = {},
    ): Result<MediaAttachment> = withContext(Dispatchers.IO) {
        runCatching {
            downloadDir.mkdirs()
            val safeName = attachment.fileName.safeFileName().ifBlank { "media-${attachment.id}" }
            val destination = File(downloadDir, "${attachment.id}-$safeName")
            val partial = File(downloadDir, ".${attachment.id}-${UUID.randomUUID()}.part")
            try {
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        require(copied <= MediaAttachment.MAX_FILE_BYTES) { "Downloaded file exceeds 25 MB" }
                        output.write(buffer, 0, read)
                        onBytesCopied(copied)
                    }
                    output.fd.sync()
                }
                require(partial.length() > 0L) { "Downloaded file is empty" }
                if (destination.exists()) destination.delete()
                check(partial.renameTo(destination)) { "Could not save this download" }
            } finally {
                partial.delete()
            }
            attachment.copy(
                localUri = Uri.fromFile(destination).toString(),
                sizeBytes = destination.length(),
                transferState = MediaTransferState.Ready,
                progress = 100,
                errorMessage = "",
            )
        }
    }

    fun hydrateLocal(attachment: MediaAttachment): MediaAttachment {
        if (fileFor(attachment) != null) return attachment
        val safeName = attachment.fileName.safeFileName().ifBlank { "media-${attachment.id}" }
        val cached = File(downloadDir, "${attachment.id}-$safeName")
        return if (cached.isFile && cached.length() > 0L) {
            attachment.copy(
                localUri = Uri.fromFile(cached).toString(),
                sizeBytes = cached.length(),
                transferState = MediaTransferState.Ready,
                progress = 100,
                errorMessage = "",
            )
        } else attachment
    }

    fun fileFor(attachment: MediaAttachment): File? {
        if (attachment.localUri.isBlank()) return null
        val uri = Uri.parse(attachment.localUri)
        return when (uri.scheme) {
            "file" -> uri.path?.let(::File)
            else -> null
        }?.takeIf(File::isFile)
    }

    fun shareUri(attachment: MediaAttachment): Uri? {
        val file = fileFor(attachment) ?: return null
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun storageSnapshot(): MediaStorageSnapshot = MediaStorageSnapshot(
        outgoingBytes = outgoingDir.safeFiles().sumOf(File::length),
        downloadedBytes = downloadDir.safeFiles().sumOf(File::length),
        outgoingItems = outgoingDir.safeFiles().size,
        downloadedItems = downloadDir.safeFiles().size,
    )

    fun clearDownloadedCache(): MediaStorageSnapshot {
        downloadDir.safeFiles().forEach(File::delete)
        return storageSnapshot()
    }

    fun deleteOutgoing(attachment: MediaAttachment) {
        fileFor(attachment)?.takeIf { file ->
            runCatching { file.canonicalPath.startsWith(outgoingDir.canonicalPath + File.separator) }.getOrDefault(false)
        }?.delete()
    }

    private fun importImage(uri: Uri, caption: String): MediaAttachment {
        val sourceName = queryName(uri).safeFileName().ifBlank { "photo.jpg" }
        val bitmap = decodeBitmap(uri)
        val scaled = bitmap.scaledWithin(2048)
        val destination = File(outgoingDir, "image-${UUID.randomUUID()}.jpg")
        return try {
            FileOutputStream(destination).use { output ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, 84, output)) { "Could not prepare this photo" }
                output.fd.sync()
            }
            MediaAttachment(
                id = UUID.randomUUID().toString(),
                kind = MediaKind.Image,
                fileName = sourceName.substringBeforeLast('.', sourceName).take(150) + ".jpg",
                mimeType = "image/jpeg",
                sizeBytes = destination.length(),
                caption = caption,
                localUri = Uri.fromFile(destination).toString(),
                width = scaled.width,
                height = scaled.height,
            ).validateForSend().getOrThrow()
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            if (scaled !== bitmap && !bitmap.isRecycled) bitmap.recycle()
            if (!scaled.isRecycled) scaled.recycle()
        }
    }

    private fun importDocument(uri: Uri, caption: String): MediaAttachment {
        val name = queryName(uri).safeFileName().ifBlank { "document" }.take(180)
        val mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val declaredSize = querySize(uri)
        require(declaredSize <= 0L || declaredSize <= MediaAttachment.MAX_FILE_BYTES) { "Files can be up to 25 MB" }
        val destination = File(outgoingDir, "document-${UUID.randomUUID()}-${name}")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyToLimited(output, MediaAttachment.MAX_FILE_BYTES)
                    output.fd.sync()
                }
            } ?: error("Could not read this document")
            MediaAttachment(
                id = UUID.randomUUID().toString(),
                kind = MediaKind.Document,
                fileName = name,
                mimeType = mime,
                sizeBytes = destination.length(),
                caption = caption,
                localUri = Uri.fromFile(destination).toString(),
            ).validateForSend().getOrThrow()
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = if (uri.scheme == "file") ImageDecoder.createSource(File(requireNotNull(uri.path)))
            else ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val scale = minOf(1f, 2048f / maxOf(width, height).toFloat())
                decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path, bounds)
            else context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported photo" }
            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 2048) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path, options)
                ?: error("Could not decode this photo")
            else context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Could not decode this photo")
        }
    }

    private fun queryName(uri: Uri): String {
        if (uri.scheme == "file") return uri.path?.let(::File)?.name.orEmpty()
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else "" }.orEmpty()
        }.getOrDefault("").ifBlank { uri.lastPathSegment.orEmpty() }
    }

    private fun querySize(uri: Uri): Long {
        if (uri.scheme == "file") return uri.path?.let(::File)?.length() ?: -1L
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun Bitmap.scaledWithin(maxDimension: Int): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= maxDimension) return this
        val ratio = maxDimension.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    private fun normalizeWaveform(values: List<Int>): List<Int> {
        if (values.isEmpty()) return List(36) { 16 }
        val samples = if (values.size <= 90) values else {
            val step = values.size.toDouble() / 90.0
            List(90) { index -> values[(index * step).toInt().coerceAtMost(values.lastIndex)] }
        }
        val max = samples.maxOrNull()?.coerceAtLeast(1) ?: 1
        return samples.map { ((it.toDouble() / max) * 100.0).toInt().coerceIn(4, 100) }
    }

    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim('.', ' ')

    private fun java.io.InputStream.copyToLimited(output: java.io.OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { "Files can be up to 25 MB" }
            output.write(buffer, 0, read)
        }
    }

    private fun File.safeFiles(): List<File> = listFiles()?.filter(File::isFile).orEmpty()

    data class CameraCapture(val uri: String, val filePath: String)
}
