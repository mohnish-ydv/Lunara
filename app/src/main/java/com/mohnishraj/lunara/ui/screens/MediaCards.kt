package com.mohnishraj.lunara.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.MediaStorageSnapshot
import com.mohnishraj.lunara.domain.MediaTransferState
import com.mohnishraj.lunara.domain.VoiceRecordingState
import com.mohnishraj.lunara.domain.formatDuration
import com.mohnishraj.lunara.domain.formatFileSize
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun MediaAttachmentCard(
    attachment: MediaAttachment,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().animateContentSize()) {
        when (attachment.kind) {
            MediaKind.Image -> ImageAttachment(attachment, onDownload)
            MediaKind.Document -> DocumentAttachment(attachment, onDownload) { openDocument(context, attachment) }
            MediaKind.Voice -> VoiceAttachment(attachment, onDownload)
        }
        if (attachment.caption.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Text(attachment.caption, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        TransferStatus(attachment, onDownload)
    }
}

@Composable
private fun ImageAttachment(attachment: MediaAttachment, onDownload: () -> Unit) {
    var showViewer by remember { mutableStateOf(false) }
    var bitmap by remember(attachment.localUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(attachment.localUri) {
        bitmap = withContext(Dispatchers.IO) {
            attachment.localUri.takeIf(String::isNotBlank)?.let { text ->
                runCatching {
                    val uri = Uri.parse(text)
                    when (uri.scheme) {
                        "file" -> BitmapFactory.decodeFile(uri.path)
                        else -> null
                    }
                }.getOrNull()
            }
        }
    }
    val currentBitmap = bitmap
    DisposableEffect(currentBitmap) {
        onDispose { currentBitmap?.takeUnless { it.isRecycled }?.recycle() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(attachment.aspectRatio.coerceIn(0.72f, 1.8f))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(enabled = currentBitmap != null || attachment.remotePath.isNotBlank()) {
                if (currentBitmap != null) showViewer = true else onDownload()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (currentBitmap != null) {
            Image(currentBitmap.asImageBitmap(), contentDescription = attachment.fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = Violet, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(7.dp))
                Text(if (attachment.remotePath.isNotBlank()) "Tap to download photo" else "Photo preview unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (showViewer && currentBitmap != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
                Image(currentBitmap.asImageBitmap(), contentDescription = attachment.fileName, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                IconButton(onClick = { showViewer = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DocumentAttachment(attachment: MediaAttachment, onDownload: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
            .clickable { if (attachment.isOpenable) onOpen() else onDownload() }.padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Violet.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, contentDescription = null, tint = Violet, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${attachment.mimeType.substringAfterLast('/').uppercase()} · ${formatFileSize(attachment.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (attachment.isOpenable) Icons.Rounded.FolderOpen else Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun VoiceAttachment(attachment: MediaAttachment, onDownload: () -> Unit) {
    if (!attachment.isPlayable) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
                .clickable(onClick = onDownload).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Violet)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Voice note", style = MaterialTheme.typography.titleSmall)
                Text(formatDuration(attachment.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Violet)
        }
        return
    }

    val context = LocalContext.current
    var player by remember(attachment.localUri) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    val duration = attachment.durationMs.toInt().coerceAtLeast(1)

    DisposableEffect(attachment.localUri) {
        onDispose {
            runCatching { player?.stop() }
            player?.release()
            player = null
        }
    }
    LaunchedEffect(playing, player) {
        while (playing && isActive) {
            position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            delay(160)
        }
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (player == null) {
                        player = MediaPlayer().apply {
                            setDataSource(context, Uri.parse(attachment.localUri))
                            prepare()
                            if (position > 0) seekTo(position)
                            setOnCompletionListener { playing = false; position = 0; seekTo(0) }
                        }
                    }
                    player?.let { active ->
                        if (active.isPlaying) { active.pause(); playing = false }
                        else {
                            active.playbackParams = active.playbackParams.setSpeed(speed)
                            active.start(); playing = true
                        }
                    }
                },
                modifier = Modifier.size(44.dp).background(Violet, CircleShape),
            ) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "Pause" else "Play", tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Waveform(attachment.waveform, progress = position.toFloat() / duration.toFloat(), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(formatDuration(if (position > 0) position.toLong() else attachment.durationMs), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = position.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { value -> position = value.toInt(); runCatching { player?.seekTo(position) } },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = {
                    speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                    runCatching { player?.playbackParams = player?.playbackParams?.setSpeed(speed) ?: return@runCatching }
                },
                shape = CircleShape,
                color = Violet.copy(alpha = 0.14f),
            ) { Text("${speed}x", style = MaterialTheme.typography.labelMedium, color = Violet, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) }
        }
    }
}

@Composable
private fun Waveform(values: List<Int>, progress: Float, modifier: Modifier = Modifier) {
    val bars = if (values.isEmpty()) List(30) { 18 + (it % 6) * 7 } else values.takeLast(48)
    Row(modifier.height(34.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        bars.forEachIndexed { index, value ->
            val played = index.toFloat() / bars.size.coerceAtLeast(1) <= progress
            Box(Modifier.width(3.dp).height((8 + value.coerceIn(0, 100) * 0.24f).dp).clip(CircleShape).background(if (played) Violet else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)))
        }
    }
}

@Composable
private fun TransferStatus(attachment: MediaAttachment, onDownload: () -> Unit) {
    when (attachment.transferState) {
        MediaTransferState.Preparing, MediaTransferState.Uploading, MediaTransferState.Downloading -> {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { attachment.progress / 100f }, modifier = Modifier.fillMaxWidth().clip(CircleShape))
            Spacer(Modifier.height(4.dp))
            Text(
                when (attachment.transferState) {
                    MediaTransferState.Preparing -> "Preparing…"
                    MediaTransferState.Uploading -> "Uploading ${attachment.progress}%"
                    else -> "Downloading ${attachment.progress}%"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MediaTransferState.Failed -> {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onDownload)) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Peach, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(attachment.errorMessage.ifBlank { "Transfer failed. Tap to retry." }, color = Peach, style = MaterialTheme.typography.labelMedium)
            }
        }
        MediaTransferState.Ready -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerSheet(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onDocument: () -> Unit,
    onVoice: () -> Unit,
    onCard: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Text("Share something", style = MaterialTheme.typography.titleLarge)
            Text("Media stays private to conversation participants when cloud storage is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaChoice("Photos", Icons.Rounded.Image, Violet, Modifier.weight(1f), onPhoto)
                MediaChoice("Camera", Icons.Rounded.CameraAlt, Mint, Modifier.weight(1f), onCamera)
                MediaChoice("File", Icons.Rounded.Description, Peach, Modifier.weight(1f), onDocument)
                MediaChoice("Voice", Icons.Rounded.Mic, Violet, Modifier.weight(1f), onVoice)
            }
            Spacer(Modifier.height(12.dp))
            Surface(onClick = onCard, shape = RoundedCornerShape(18.dp), color = Mint.copy(alpha = 0.11f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Mint)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Interactive card", style = MaterialTheme.typography.titleSmall)
                        Text("Task, poll, checklist, event and more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaChoice(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(19.dp)).background(color.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun PendingAttachmentPreview(attachment: MediaAttachment, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Violet.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
            Icon(when (attachment.kind) { MediaKind.Image -> Icons.Rounded.Image; MediaKind.Document -> Icons.Rounded.Description; MediaKind.Voice -> Icons.Rounded.Mic }, contentDescription = null, tint = Violet)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text("${attachment.kind.displayName} · ${formatFileSize(attachment.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = "Remove") }
    }
}

@Composable
fun VoiceRecorderBar(
    state: VoiceRecordingState,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Peach.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Peach)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (state.isPaused) "Recording paused" else "Recording voice note", style = MaterialTheme.typography.labelLarge)
            Waveform(state.waveform, progress = 1f, modifier = Modifier.fillMaxWidth())
        }
        Text(formatDuration(state.elapsedMs), style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = onPauseResume) { Icon(if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = if (state.isPaused) "Resume" else "Pause") }
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = Peach) }
        IconButton(onClick = onSend, enabled = state.canSend, modifier = Modifier.background(if (state.canSend) Violet else Violet.copy(alpha = 0.28f), CircleShape)) {
            Icon(Icons.Rounded.Stop, contentDescription = "Finish and send", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGallerySheet(
    messages: List<ChatMessage>,
    loading: Boolean,
    onDownload: (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().height(650.dp).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Shared media", style = MaterialTheme.typography.titleLarge)
            Text("Photos, documents and voice notes from this conversation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Violet) }
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No media shared yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else -> androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        val attachment = message.attachment ?: return@items
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
                            Column(Modifier.padding(12.dp)) {
                                MediaAttachmentCard(attachment, onDownload = { onDownload(message) })
                                Spacer(Modifier.height(6.dp))
                                Text(message.createdAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaStorageSheet(
    snapshot: MediaStorageSnapshot,
    clearing: Boolean,
    onClearDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Violet.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = Violet, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("Media & storage", style = MaterialTheme.typography.titleLarge)
                    Text("${formatFileSize(snapshot.totalBytes)} across ${snapshot.totalItems} files", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(22.dp))
            StorageRow("Sent media", snapshot.outgoingItems, snapshot.outgoingBytes)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            StorageRow("Downloaded cache", snapshot.downloadedItems, snapshot.downloadedBytes)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onClearDownloads, enabled = !clearing && snapshot.downloadedItems > 0, modifier = Modifier.fillMaxWidth()) {
                if (clearing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Clear downloaded cache")
            }
            Spacer(Modifier.height(10.dp))
            Text("Sent files are kept so failed uploads can be retried. Clearing downloads never removes messages from a conversation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StorageRow(label: String, items: Int, bytes: Long) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("$items files", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(formatFileSize(bytes), fontWeight = FontWeight.SemiBold)
    }
}

private fun openDocument(context: Context, attachment: MediaAttachment) {
    val file = attachment.localUri.takeIf(String::isNotBlank)?.let(Uri::parse)?.path?.let(::File) ?: return
    if (!file.isFile) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = attachment.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Open with"))
    }
}
