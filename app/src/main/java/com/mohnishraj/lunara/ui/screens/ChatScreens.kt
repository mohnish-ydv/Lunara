@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.mohnishraj.lunara.ui.screens

import android.Manifest
import android.content.pm.PackageManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.ChatConversation
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.ConversationFilter
import com.mohnishraj.lunara.domain.MessageDeliveryState
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.data.media.MediaAttachmentStore
import com.mohnishraj.lunara.ui.ChatUiState
import com.mohnishraj.lunara.ui.components.AvatarOrb
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Rose
import com.mohnishraj.lunara.ui.theme.Violet
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatsScreen(
    state: ChatUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ConversationFilter) -> Unit,
    onRefresh: () -> Unit,
    onOpenConversation: (ChatConversation) -> Unit,
    onOpenOrganizer: (ChatConversation?) -> Unit,
    onSaveSettings: (ChatConversation, Boolean, Boolean, Boolean, List<String>) -> Unit,
    onFindPeople: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = remember(state.conversations, state.query, state.filter) {
        state.conversations
            .filter { conversation ->
                when (state.filter) {
                    ConversationFilter.All -> !conversation.isArchived
                    ConversationFilter.Unread -> !conversation.isArchived && conversation.unreadCount > 0
                    ConversationFilter.Pinned -> !conversation.isArchived && conversation.isPinned
                    ConversationFilter.Muted -> !conversation.isArchived && conversation.isMuted
                    ConversationFilter.Archived -> conversation.isArchived
                }
            }
            .filter { it.matches(state.query) }
            .sortedWith(compareByDescending<ChatConversation> { it.isPinned }.thenByDescending(ChatConversation::lastMessageAt))
    }

    Column(modifier.fillMaxSize()) {
        InboxSearch(
            value = state.query,
            onValueChange = onQueryChange,
            onRefresh = onRefresh,
            refreshing = state.isLoadingConversations,
        )
        Spacer(Modifier.height(11.dp))
        InboxPulse(state.conversations)
        Spacer(Modifier.height(11.dp))
        ConversationFilters(state.filter, onFilterChange)
        Spacer(Modifier.height(10.dp))
        ChatNotice(state.notice, onDismissNotice)

        when {
            state.isLoadingConversations && state.conversations.isEmpty() -> InboxSkeleton(Modifier.fillMaxSize())
            visible.isEmpty() -> EmptyChats(
                hasQuery = state.query.isNotBlank(),
                filter = state.filter,
                onFindPeople = onFindPeople,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(filterHeading(state.filter), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text("${visible.size}", style = MaterialTheme.typography.labelLarge, color = Violet)
                    }
                }
                items(visible, key = ChatConversation::id) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation) },
                        onLongClick = { onOpenOrganizer(conversation) },
                    )
                }
                item { Spacer(Modifier.height(92.dp)) }
            }
        }
    }

    state.organizingConversation?.let { conversation ->
        ConversationSettingsSheet(
            conversation = conversation,
            busy = state.actionMessageId == "settings:${conversation.id}",
            onDismiss = { onOpenOrganizer(null) },
            onSave = { pinned, archived, muted, labels ->
                onSaveSettings(conversation, pinned, archived, muted, labels)
            },
        )
    }
}

@Composable
private fun InboxSearch(
    value: String,
    onValueChange: (String) -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    )
                )
            )
            .padding(start = 16.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Violet, modifier = Modifier.size(21.dp))
        Spacer(Modifier.size(11.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { field ->
                Box {
                    if (value.isBlank()) Text("Search people, messages or labels", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    field()
                }
            },
        )
        if (value.isNotBlank()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
            }
        }
        IconButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.size(40.dp)) {
            if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Rounded.Refresh, contentDescription = "Refresh conversations", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InboxPulse(conversations: List<ChatConversation>) {
    val active = conversations.count { !it.isArchived }
    val unread = conversations.sumOf(ChatConversation::unreadCount)
    val drafts = conversations.count { it.draft.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(Brush.horizontalGradient(listOf(Violet.copy(alpha = 0.18f), Mint.copy(alpha = 0.09f))))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PulseMetric(active.toString(), "active")
        PulseMetric(unread.toString(), "unread")
        PulseMetric(drafts.toString(), "drafts")
        PulseMetric(conversations.count(ChatConversation::isPinned).toString(), "pinned")
    }
}

@Composable
private fun PulseMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConversationFilters(selected: ConversationFilter, onSelected: (ConversationFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConversationFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filterLabel(filter)) },
                leadingIcon = filterIcon(filter)?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp)) } },
            )
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val emphasis by animateFloatAsState(if (conversation.unreadCount > 0) 1f else 0.72f, label = "conversationEmphasis")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        if (conversation.isPinned) Violet.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    )
                )
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AvatarOrb(conversation.person.avatarSeed, conversation.person.displayName, Modifier.size(58.dp))
            if (conversation.isOnline) OnlineDot(Modifier.align(Alignment.BottomEnd))
        }
        Spacer(Modifier.size(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.person.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (conversation.isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = Violet, modifier = Modifier.size(15.dp))
                if (conversation.isMuted) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = "Muted", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    compactTime(conversation.lastMessageAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversation.unreadCount > 0) Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        conversation.isTyping -> "typing…"
                        conversation.draft.isNotBlank() -> "Draft: ${conversation.draft}"
                        conversation.lastMessage.isBlank() -> "Start a conversation"
                        else -> conversation.lastMessage
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        conversation.isTyping -> Mint
                        conversation.draft.isNotBlank() -> Peach
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = emphasis)
                    },
                    fontStyle = if (conversation.isTyping) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = if (conversation.draft.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.unreadCount > 0) {
                    Spacer(Modifier.size(9.dp))
                    UnreadBadge(conversation.unreadCount)
                }
            }
            if (conversation.labels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    conversation.labels.take(3).forEach { label -> LabelPill(label) }
                }
            }
        }
    }
}

@Composable
private fun OnlineDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(15.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp)
            .clip(CircleShape)
            .background(Mint)
    )
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        Modifier.clip(CircleShape).background(Violet).padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LabelPill(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = Violet,
        modifier = Modifier.clip(CircleShape).background(Violet.copy(alpha = 0.11f)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun InboxSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { index ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)).padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Violet.copy(alpha = 0.12f + index * 0.01f)))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.48f).height(15.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                    Box(Modifier.fillMaxWidth(0.78f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)))
                }
            }
        }
    }
}

@Composable
private fun EmptyChats(
    hasQuery: Boolean,
    filter: ConversationFilter,
    onFindPeople: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = filter != ConversationFilter.All
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(112.dp).background(
                Brush.linearGradient(listOf(Violet.copy(alpha = 0.22f), Mint.copy(alpha = 0.12f))),
                RoundedCornerShape(34.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when {
                    hasQuery -> Icons.Rounded.Search
                    filter == ConversationFilter.Archived -> Icons.Rounded.Archive
                    else -> Icons.Rounded.PeopleAlt
                },
                contentDescription = null,
                tint = if (hasQuery) Peach else Violet,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            when {
                hasQuery -> "Nothing matches that search"
                filtered -> "This view is beautifully quiet"
                else -> "Your conversations begin here"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                hasQuery -> "Try another name, label or word from a message."
                filtered -> "Conversations will appear here when they match this filter."
                else -> "Open a connected profile and send the first message."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        if (!hasQuery && !filtered) {
            Spacer(Modifier.height(20.dp))
            Surface(onClick = onFindPeople, shape = CircleShape, color = Violet, contentColor = Color.White) {
                Text("Find someone", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 19.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
fun ChatThreadScreen(
    state: ChatUiState,
    currentUserId: String,
    onBack: () -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onLoadOlder: () -> Unit,
    onSelectMessage: (ChatMessage?) -> Unit,
    onRetry: (ChatMessage) -> Unit,
    onCancelContext: () -> Unit,
    onDismissNotice: () -> Unit,
    onReply: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String?) -> Unit,
    onBookmark: (ChatMessage) -> Unit,
    onShowSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShowBookmarks: (Boolean) -> Unit,
    onShowDetails: (Boolean) -> Unit,
    onShowModuleComposer: (Boolean) -> Unit,
    onShowAttachmentPicker: (Boolean) -> Unit,
    onCreateCameraCapture: () -> MediaAttachmentStore.CameraCapture,
    onMediaSelected: (String, MediaKind, String?) -> Unit,
    onClearPendingMedia: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onPauseResumeVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    onFinishVoice: () -> Unit,
    onDownloadMedia: (ChatMessage) -> Unit,
    onShowMediaGallery: (Boolean) -> Unit,
    onSendModule: (MessageModule) -> Unit,
    onUpdateModule: (ChatMessage, MessageModule) -> Unit,
    onOpenOrganizer: (ChatConversation?) -> Unit,
    onSaveSettings: (ChatConversation, Boolean, Boolean, Boolean, List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversation = state.activeConversation ?: return
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    var pendingCamera by remember { mutableStateOf<MediaAttachmentStore.CameraCapture?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onMediaSelected(it.toString(), MediaKind.Image, null) }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onMediaSelected(it.toString(), MediaKind.Document, null) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingCamera
        pendingCamera = null
        if (success && capture != null) onMediaSelected(capture.uri, MediaKind.Image, capture.filePath)
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartVoiceRecording()
    }

    LaunchedEffect(state.messages.lastOrNull()?.id, conversation.id) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(modifier.fillMaxSize().imePadding()) {
        ChatHeader(
            conversation = conversation,
            onBack = onBack,
            onSearch = { onShowSearch(true) },
            onBookmarks = { onShowBookmarks(true) },
            onDetails = { onShowDetails(true) },
            onOptions = { onOpenOrganizer(conversation) },
        )
        ChatNotice(state.notice, onDismissNotice)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoadingMessages && state.messages.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Violet,
                    strokeWidth = 2.5.dp,
                )
                state.messages.isEmpty() -> FirstMessagePrompt(conversation, Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    item {
                        if (state.hasMore) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                if (state.isLoadingMore) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Surface(onClick = onLoadOlder, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)) {
                                    Text("Load earlier messages", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
                                }
                            }
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            mine = message.isMine(currentUserId),
                            currentUserId = currentUserId,
                            enabled = state.actionMessageId == null,
                            onOpenActions = { onSelectMessage(message) },
                            onRetry = { onRetry(message) },
                            onDownload = { onDownloadMedia(message) },
                            onUpdateModule = { updated -> onUpdateModule(message, updated) },
                        )
                    }
                    if (conversation.isTyping) item { TypingBubble(conversation.person.displayName) }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
        ComposerContext(state, onCancelContext)
        state.pendingAttachment?.let { attachment ->
            PendingAttachmentPreview(attachment = attachment, onRemove = onClearPendingMedia)
            Spacer(Modifier.height(7.dp))
        }
        if (state.voiceRecording.isRecording) {
            VoiceRecorderBar(
                state = state.voiceRecording,
                onPauseResume = onPauseResumeVoice,
                onCancel = onCancelVoice,
                onSend = onFinishVoice,
            )
        } else {
            MessageComposer(
                value = state.composer,
                onValueChange = onComposerChange,
                onSend = onSend,
                onAddModule = { onShowAttachmentPicker(true) },
                enabled = state.actionMessageId == null && !state.isPreparingMedia,
                canSendEmpty = state.pendingAttachment != null,
                preparing = state.isPreparingMedia,
            )
        }
    }

    if (state.showModuleComposer) {
        ModuleComposerSheet(
            onDismiss = { onShowModuleComposer(false) },
            onSend = onSendModule,
        )
    }

    if (state.showAttachmentPicker) {
        AttachmentPickerSheet(
            onPhoto = { onShowAttachmentPicker(false); photoLauncher.launch(arrayOf("image/*")) },
            onCamera = {
                val capture = onCreateCameraCapture()
                pendingCamera = capture
                onShowAttachmentPicker(false)
                cameraLauncher.launch(android.net.Uri.parse(capture.uri))
            },
            onDocument = { onShowAttachmentPicker(false); documentLauncher.launch(arrayOf("application/pdf", "text/*", "application/zip", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "*/*")) },
            onVoice = {
                onShowAttachmentPicker(false)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onStartVoiceRecording()
                else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            },
            onCard = { onShowAttachmentPicker(false); onShowModuleComposer(true) },
            onDismiss = { onShowAttachmentPicker(false) },
        )
    }

    state.selectedMessage?.let { message ->
        MessageActionsSheet(
            message = message,
            mine = message.isMine(currentUserId),
            busy = state.actionMessageId == message.id,
            onDismiss = { onSelectMessage(null) },
            onReply = { onReply(message) },
            onEdit = { onEdit(message) },
            onDelete = { onDelete(message) },
            onReact = { emoji -> onReact(message, emoji) },
            onBookmark = { onBookmark(message) },
        )
    }
    if (state.showMessageSearch) {
        MessageCollectionSheet(
            title = "Search this conversation",
            subtitle = "Find exact words without leaving the thread.",
            query = state.messageSearchQuery,
            onQueryChange = onSearchQueryChange,
            messages = state.messageSearchResults,
            loading = state.isSearchingMessages,
            emptyText = if (state.messageSearchQuery.isBlank()) "Type a word or phrase" else "No matching messages",
            currentUserId = currentUserId,
            onSelect = onSelectMessage,
            onDismiss = { onShowSearch(false) },
        )
    }
    if (state.showBookmarks) {
        MessageCollectionSheet(
            title = "Saved messages",
            subtitle = "Your private collection from this conversation.",
            query = "",
            onQueryChange = {},
            messages = state.bookmarkedMessages,
            loading = state.isLoadingBookmarks,
            emptyText = "No saved messages yet",
            currentUserId = currentUserId,
            onSelect = onSelectMessage,
            onDismiss = { onShowBookmarks(false) },
            showSearch = false,
        )
    }
    if (state.showDetails) {
        ConversationDetailsSheet(
            state = state,
            onDismiss = { onShowDetails(false) },
            onOpenBookmarks = { onShowDetails(false); onShowBookmarks(true) },
            onOpenMedia = { onShowDetails(false); onShowMediaGallery(true) },
        )
    }
    if (state.showMediaGallery) {
        MediaGallerySheet(
            messages = state.mediaMessages,
            loading = state.isLoadingMedia,
            onDownload = onDownloadMedia,
            onDismiss = { onShowMediaGallery(false) },
        )
    }
    state.organizingConversation?.let { target ->
        ConversationSettingsSheet(
            conversation = target,
            busy = state.actionMessageId == "settings:${target.id}",
            onDismiss = { onOpenOrganizer(null) },
            onSave = { pinned, archived, muted, labels -> onSaveSettings(target, pinned, archived, muted, labels) },
        )
    }
}

@Composable
private fun ChatHeader(
    conversation: ChatConversation,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onBookmarks: () -> Unit,
    onDetails: () -> Unit,
    onOptions: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                    )
                )
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        AvatarOrb(conversation.person.avatarSeed, conversation.person.displayName, Modifier.size(44.dp))
        Spacer(Modifier.size(10.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onDetails)
                .padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.person.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.isMuted) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = "Muted", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                when {
                    conversation.isTyping -> "typing…"
                    conversation.isOnline -> "online"
                    conversation.lastActiveAt.isNotBlank() -> "active ${relativeTime(conversation.lastActiveAt)}"
                    else -> "@${conversation.person.username}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (conversation.isTyping || conversation.isOnline) Mint else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CompactHeaderAction(Icons.Rounded.Search, "Search messages", onSearch)
        CompactHeaderAction(Icons.Rounded.BookmarkBorder, "Saved messages", onBookmarks)
        CompactHeaderAction(Icons.Rounded.MoreHoriz, "Conversation options", onOptions)
    }
}

@Composable
private fun CompactHeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    mine: Boolean,
    currentUserId: String,
    enabled: Boolean,
    onOpenActions: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onUpdateModule: (MessageModule) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (message.module == null) 0.84f else 0.94f)
                .clip(RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp, bottomStart = if (mine) 21.dp else 7.dp, bottomEnd = if (mine) 7.dp else 21.dp))
                .background(
                    if (mine && message.module == null && message.attachment == null) Brush.linearGradient(listOf(Violet, Color(0xFF6653D8)))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)))
                )
                .combinedClickable(onClick = onOpenActions, onLongClick = onOpenActions)
                .animateContentSize()
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            val accentBubble = mine && message.module == null && message.attachment == null
            if (message.replyPreview.isNotBlank()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color.Black.copy(alpha = if (mine) 0.15f else 0.06f)).padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text("Reply", style = MaterialTheme.typography.labelSmall, color = if (accentBubble) Mint else Violet)
                    Text(message.replyPreview, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = if (accentBubble) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (message.isDeleted) {
                Text(
                    "Message removed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = if (accentBubble) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
            } else if (message.attachment != null) {
                MediaAttachmentCard(
                    attachment = message.attachment,
                    onDownload = { if (message.deliveryState == MessageDeliveryState.Failed) onRetry() else onDownload() },
                )
            } else if (message.module != null) {
                MessageModuleCard(
                    module = message.module,
                    currentUserId = currentUserId,
                    enabled = enabled && message.deliveryState != MessageDeliveryState.Failed,
                    onUpdate = onUpdateModule,
                )
            } else {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (accentBubble) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                if (message.isBookmarked) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = "Saved", tint = if (accentBubble) Mint else Violet, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (message.isEdited) Text("edited · ", style = MaterialTheme.typography.labelSmall, color = if (accentBubble) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(compactTime(message.createdAt), style = MaterialTheme.typography.labelSmall, color = if (accentBubble) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant)
                if (mine) {
                    Spacer(Modifier.size(5.dp))
                    DeliveryIcon(message.deliveryState, onRetry, accentBubble)
                }
            }
            if (message.reactions.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    message.reactions.take(4).forEach { reaction ->
                        Text(
                            "${reaction.emoji} ${reaction.count}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clip(CircleShape).background(if (reaction.reactedByMe) Mint.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.10f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (accentBubble) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryIcon(state: MessageDeliveryState, onRetry: () -> Unit, onAccent: Boolean) {
    val icon = when (state) {
        MessageDeliveryState.Sending -> Icons.Rounded.Schedule
        MessageDeliveryState.Sent -> Icons.Rounded.Check
        MessageDeliveryState.Delivered, MessageDeliveryState.Read -> Icons.Rounded.DoneAll
        MessageDeliveryState.Failed -> Icons.Rounded.ErrorOutline
    }
    val color = when (state) {
        MessageDeliveryState.Read -> Mint
        MessageDeliveryState.Failed -> Peach
        else -> if (onAccent) Color.White.copy(alpha = 0.76f) else MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        icon,
        contentDescription = state.name,
        tint = color,
        modifier = Modifier.size(16.dp).then(if (state == MessageDeliveryState.Failed) Modifier.combinedClickable(onClick = onRetry) else Modifier),
    )
}

@Composable
private fun ComposerContext(state: ChatUiState, onCancel: () -> Unit) {
    val message = state.editTarget ?: state.replyTo ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 3.dp, height = 40.dp).clip(CircleShape).background(if (state.editTarget != null) Peach else Violet))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (state.editTarget != null) "Editing message" else "Replying", style = MaterialTheme.typography.labelMedium, color = if (state.editTarget != null) Peach else Violet)
            Text(message.previewText, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = "Cancel") }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddModule: () -> Unit,
    enabled: Boolean,
    canSendEmpty: Boolean,
    preparing: Boolean,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 3.dp)) {
        if (value.isNotBlank()) {
            Text(
                "${value.length}/4000",
                style = MaterialTheme.typography.labelSmall,
                color = if (value.length > 3800) Peach else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(end = 64.dp, bottom = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            IconButton(
                onClick = onAddModule,
                enabled = enabled,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            ) {
                if (preparing) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = Violet)
                else Icon(Icons.Rounded.AddCircle, contentDescription = "Add media or interactive card", tint = Violet, modifier = Modifier.size(24.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)).padding(horizontal = 16.dp, vertical = 14.dp),
                minLines = 1,
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { field -> Box { if (value.isBlank()) Text("Write a message", color = MaterialTheme.colorScheme.onSurfaceVariant); field() } },
            )
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(
                    Brush.linearGradient(
                        if ((value.isBlank() && !canSendEmpty) || !enabled) listOf(Violet.copy(alpha = 0.36f), Violet.copy(alpha = 0.36f))
                        else listOf(Violet, Color(0xFF6550D5))
                    )
                ).combinedClickable(enabled = enabled && (value.isNotBlank() || canSendEmpty), onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun TypingBubble(name: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text("$name is typing  • • •", color = Mint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)).padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun FirstMessagePrompt(conversation: ChatConversation, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarOrb(conversation.person.avatarSeed, conversation.person.displayName, Modifier.size(82.dp))
        Spacer(Modifier.height(16.dp))
        Text("Start something meaningful", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text("This is the beginning of your conversation with ${conversation.person.displayName}.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    message: ChatMessage,
    mine: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String?) -> Unit,
    onBookmark: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var showDetails by remember(message.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            val synced = !message.id.startsWith("local-") && message.deliveryState != MessageDeliveryState.Sending && message.deliveryState != MessageDeliveryState.Failed
            if (synced && !message.isDeleted) {
                Text("React", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("💜", "👍", "😂", "🔥", "✨", "😮").forEach { emoji ->
                        Text(emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clip(CircleShape).combinedClickable(enabled = !busy, onClick = { onReact(emoji) }).padding(8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ActionRow("Reply", Icons.AutoMirrored.Rounded.Reply, enabled = !busy, onClick = onReply)
                ActionRow(if (message.isBookmarked) "Remove from saved" else "Save message", if (message.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, enabled = !busy, onClick = onBookmark)
            }
            if (!message.isDeleted) ActionRow("Copy summary", Icons.Rounded.Check, enabled = !busy) { clipboard.setText(AnnotatedString(message.previewText)); onDismiss() }
            if (mine && synced && !message.isDeleted && message.module == null && message.attachment == null) ActionRow("Edit", Icons.Rounded.Edit, enabled = !busy, onClick = onEdit)
            if (mine && synced) ActionRow("Remove", Icons.Rounded.DeleteOutline, enabled = !busy, danger = true, onClick = onDelete)
            ActionRow("Message details", Icons.Rounded.Info, enabled = true) { showDetails = !showDetails }
            AnimatedVisibility(showDetails) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(14.dp)) {
                    Text("Sent ${fullTime(message.createdAt)}", style = MaterialTheme.typography.bodyMedium)
                    if (mine) Text("Status: ${message.deliveryState.name.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (message.isEdited) Text("Edited ${fullTime(message.editedAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (message.isBookmarked) Text("Saved privately", color = Violet)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSettingsSheet(
    conversation: ChatConversation,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, Boolean, List<String>) -> Unit,
) {
    var pinned by remember(conversation.id, conversation.isPinned) { mutableStateOf(conversation.isPinned) }
    var archived by remember(conversation.id, conversation.isArchived) { mutableStateOf(conversation.isArchived) }
    var muted by remember(conversation.id, conversation.isMuted) { mutableStateOf(conversation.isMuted) }
    var labelInput by remember(conversation.id) { mutableStateOf("") }
    var labels by remember(conversation.id, conversation.labels) { mutableStateOf(conversation.labels) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarOrb(conversation.person.avatarSeed, conversation.person.displayName, Modifier.size(52.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Organize conversation", style = MaterialTheme.typography.titleLarge)
                    Text(conversation.person.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Violet)
            }
            Spacer(Modifier.height(18.dp))
            SettingToggle("Pin to top", "Keep this conversation easy to reach.", Icons.Rounded.PushPin, pinned) { pinned = it }
            SettingToggle("Mute updates", "Keep messages without notification noise.", Icons.Rounded.NotificationsOff, muted) { muted = it }
            SettingToggle("Archive", "Move it out of the main inbox.", Icons.Rounded.Archive, archived) { archived = it }
            Spacer(Modifier.height(16.dp))
            Text("Labels", style = MaterialTheme.typography.titleMedium)
            Text("Add up to four private labels.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(9.dp))
            if (labels.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    labels.forEach { label ->
                        AssistChip(onClick = { labels = labels.filterNot { it == label } }, label = { Text(label) }, leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove $label", modifier = Modifier.size(16.dp)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it.take(24) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New label") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                trailingIcon = {
                    Text(
                        "Add",
                        color = if (labelInput.isNotBlank() && labels.size < 4) Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.combinedClickable(enabled = labelInput.isNotBlank() && labels.size < 4) {
                            val clean = labelInput.trim()
                            if (clean.isNotBlank() && clean !in labels) labels = (labels + clean).take(4)
                            labelInput = ""
                        }.padding(8.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = { onSave(pinned, archived, muted, labels) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Violet,
                contentColor = Color.White,
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Save organization", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Violet.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageCollectionSheet(
    title: String,
    subtitle: String,
    query: String,
    onQueryChange: (String) -> Unit,
    messages: List<ChatMessage>,
    loading: Boolean,
    emptyText: String,
    currentUserId: String,
    onSelect: (ChatMessage?) -> Unit,
    onDismiss: () -> Unit,
    showSearch: Boolean = true,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().height(620.dp).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showSearch) {
                Spacer(Modifier.height(13.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") } }) else null,
                    placeholder = { Text("Search messages") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Violet) }
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(messages, key = { it.id }) { message ->
                        SearchResultCard(message, mine = message.isMine(currentUserId), onClick = { onSelect(message) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(message: ChatMessage, mine: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)).combinedClickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (mine) "You" else "Them", style = MaterialTheme.typography.labelMedium, color = if (mine) Violet else Mint)
            Spacer(Modifier.weight(1f))
            if (message.isBookmarked) Icon(Icons.Rounded.Bookmark, contentDescription = "Saved", tint = Violet, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(fullTime(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(message.previewText, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationDetailsSheet(
    state: ChatUiState,
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenMedia: () -> Unit,
) {
    val conversation = state.activeConversation ?: return
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AvatarOrb(conversation.person.avatarSeed, conversation.person.displayName, Modifier.size(78.dp))
            Spacer(Modifier.height(12.dp))
            Text(conversation.person.displayName, style = MaterialTheme.typography.titleLarge)
            Text("@${conversation.person.username}", color = Violet)
            if (conversation.labels.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { conversation.labels.forEach { LabelPill(it) } }
            }
            Spacer(Modifier.height(20.dp))
            if (state.isLoadingDetails) {
                CircularProgressIndicator(color = Violet)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DetailMetric(state.insight.totalMessages.toString(), "loaded", Icons.Rounded.MoreHoriz, Modifier.weight(1f))
                    DetailMetric(state.insight.bookmarkedMessages.toString(), "saved", Icons.Rounded.Bookmark, Modifier.weight(1f))
                    DetailMetric(state.insight.sharedMedia.toString(), "media", Icons.Rounded.PeopleAlt, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onOpenBookmarks, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Violet.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, tint = Violet)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Saved messages", style = MaterialTheme.typography.titleMedium)
                            Text("Open your private collection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(onClick = onOpenMedia, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Mint.copy(alpha = 0.11f)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PeopleAlt, contentDescription = null, tint = Mint)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Shared media", style = MaterialTheme.typography.titleMedium)
                            Text("Photos, files and voice notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Shared links", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                if (state.sharedLinks.isEmpty()) {
                    Text("Links shared in this conversation will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
                } else {
                    state.sharedLinks.take(4).forEach { message ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Mint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = Mint, modifier = Modifier.size(19.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(message.body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).combinedClickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.size(13.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        if (!enabled) { Spacer(Modifier.weight(1f)); CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
    }
}

@Composable
private fun ChatNotice(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(message != null) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)).combinedClickable(onClick = onDismiss).padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message.orEmpty(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            Text("Dismiss", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private fun filterHeading(filter: ConversationFilter): String = when (filter) {
    ConversationFilter.All -> "Your conversations"
    ConversationFilter.Unread -> "Waiting for you"
    ConversationFilter.Pinned -> "Pinned conversations"
    ConversationFilter.Muted -> "Muted conversations"
    ConversationFilter.Archived -> "Archived conversations"
}

private fun filterLabel(filter: ConversationFilter): String = when (filter) {
    ConversationFilter.All -> "Inbox"
    ConversationFilter.Unread -> "Unread"
    ConversationFilter.Pinned -> "Pinned"
    ConversationFilter.Muted -> "Muted"
    ConversationFilter.Archived -> "Archived"
}

private fun filterIcon(filter: ConversationFilter): ImageVector? = when (filter) {
    ConversationFilter.All -> null
    ConversationFilter.Unread -> Icons.Rounded.DoneAll
    ConversationFilter.Pinned -> Icons.Rounded.PushPin
    ConversationFilter.Muted -> Icons.Rounded.NotificationsOff
    ConversationFilter.Archived -> Icons.Rounded.Archive
}

private fun compactTime(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(value)
        val local = instant.atZone(ZoneId.systemDefault())
        val now = Instant.now().atZone(ZoneId.systemDefault())
        when {
            local.toLocalDate() == now.toLocalDate() -> local.format(DateTimeFormatter.ofPattern("h:mm a"))
            local.year == now.year -> local.format(DateTimeFormatter.ofPattern("d MMM"))
            else -> local.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
        }
    }.getOrDefault("")
}

private fun fullTime(value: String): String = runCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
}.getOrDefault(value)

private fun relativeTime(value: String): String = runCatching {
    val seconds = Instant.now().epochSecond - Instant.parse(value).epochSecond
    when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}.getOrDefault("recently")
