package com.mohnishraj.lunara.domain

enum class MediaKind(val wireName: String, val displayName: String) {
    Image("image", "Photo"),
    Document("document", "Document"),
    Voice("voice", "Voice note");

    companion object {
        fun fromWire(value: String): MediaKind? = entries.firstOrNull { it.wireName == value }
    }
}

enum class MediaTransferState {
    Preparing,
    Uploading,
    Ready,
    Downloading,
    Failed,
}

data class MediaAttachment(
    val id: String,
    val kind: MediaKind,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val caption: String = "",
    val localUri: String = "",
    val remotePath: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val waveform: List<Int> = emptyList(),
    val transferState: MediaTransferState = MediaTransferState.Ready,
    val progress: Int = 100,
    val errorMessage: String = "",
) {
    val isLocal: Boolean get() = localUri.isNotBlank()
    val isRemote: Boolean get() = remotePath.isNotBlank()
    val isPlayable: Boolean get() = kind == MediaKind.Voice && isLocal
    val isOpenable: Boolean get() = isLocal && transferState == MediaTransferState.Ready
    val aspectRatio: Float get() = if (width > 0 && height > 0) width.toFloat() / height else 1.35f

    fun previewText(caption: String = ""): String {
        val label = when (kind) {
            MediaKind.Image -> "Photo"
            MediaKind.Document -> "Document · ${fileName.ifBlank { "File" }}"
            MediaKind.Voice -> "Voice note · ${formatDuration(durationMs)}"
        }
        val cleanCaption = caption.trim().ifBlank { this.caption.trim() }
        return cleanCaption.takeIf(String::isNotBlank)?.let { "$label · $it" } ?: label
    }

    fun validateForSend(): Result<MediaAttachment> = runCatching {
        require(id.isNotBlank()) { "Media identity is missing" }
        require(fileName.trim().isNotBlank()) { "File name is missing" }
        require(mimeType.trim().isNotBlank()) { "File type is missing" }
        require(sizeBytes in 1..MAX_FILE_BYTES) { "Files can be up to 25 MB" }
        when (kind) {
            MediaKind.Image -> {
                require(sizeBytes <= MAX_IMAGE_BYTES) { "Photos can be up to 12 MB" }
                require(width >= 0 && height >= 0) { "Photo dimensions are invalid" }
            }
            MediaKind.Document -> Unit
            MediaKind.Voice -> {
                require(sizeBytes <= MAX_VOICE_BYTES) { "Voice notes can be up to 20 MB" }
                require(durationMs in 250..MAX_VOICE_DURATION_MS) { "Voice notes must be between 1 second and 20 minutes" }
                require(waveform.size <= 180) { "Voice waveform is too large" }
                require(waveform.all { it in 0..100 }) { "Voice waveform is invalid" }
            }
        }
        require(isLocal || isRemote) { "Media file is not available" }
        copy(
            fileName = fileName.trim().take(180),
            mimeType = mimeType.trim().lowercase().take(120),
            caption = caption.trim().take(1000),
            waveform = waveform.take(180).map { it.coerceIn(0, 100) },
            progress = progress.coerceIn(0, 100),
        )
    }

    companion object {
        const val MAX_IMAGE_BYTES = 12L * 1024L * 1024L
        const val MAX_FILE_BYTES = 25L * 1024L * 1024L
        const val MAX_VOICE_BYTES = 20L * 1024L * 1024L
        const val MAX_VOICE_DURATION_MS = 20L * 60L * 1000L
    }
}

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0,
    val waveform: List<Int> = emptyList(),
) {
    val canSend: Boolean get() = isRecording && elapsedMs >= 750
}

data class MediaStorageSnapshot(
    val outgoingBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val outgoingItems: Int = 0,
    val downloadedItems: Int = 0,
) {
    val totalBytes: Long get() = outgoingBytes + downloadedBytes
    val totalItems: Int get() = outgoingItems + downloadedItems
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}
