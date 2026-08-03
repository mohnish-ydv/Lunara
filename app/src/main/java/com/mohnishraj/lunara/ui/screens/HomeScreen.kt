package com.mohnishraj.lunara.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.ChatConversation
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.ConversationFilter
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.SpaceChannel
import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceMessage
import com.mohnishraj.lunara.domain.SpaceSummary
import com.mohnishraj.lunara.data.media.MediaAttachmentStore
import com.mohnishraj.lunara.ui.AppUiState
import com.mohnishraj.lunara.ui.HomeTab
import com.mohnishraj.lunara.ui.PeopleTab
import com.mohnishraj.lunara.ui.components.AvatarOrb
import com.mohnishraj.lunara.ui.components.ConnectionPill
import com.mohnishraj.lunara.ui.components.LunaraBackdrop
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Violet

@Composable
fun HomeScreen(
    state: AppUiState,
    onTabSelected: (HomeTab) -> Unit,
    onChatQueryChange: (String) -> Unit,
    onConversationFilterChange: (ConversationFilter) -> Unit,
    onChatRefresh: () -> Unit,
    onOpenConversationOrganizer: (ChatConversation?) -> Unit,
    onSaveConversationSettings: (ChatConversation, Boolean, Boolean, Boolean, List<String>) -> Unit,
    onOpenConversation: (ChatConversation) -> Unit,
    onCloseConversation: () -> Unit,
    onComposerChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onLoadOlderMessages: () -> Unit,
    onSelectMessage: (ChatMessage?) -> Unit,
    onRetryMessage: (ChatMessage) -> Unit,
    onCancelComposerContext: () -> Unit,
    onDismissChatNotice: () -> Unit,
    onReplyMessage: (ChatMessage) -> Unit,
    onEditMessage: (ChatMessage) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onReactMessage: (ChatMessage, String?) -> Unit,
    onBookmarkMessage: (ChatMessage) -> Unit,
    onShowMessageSearch: (Boolean) -> Unit,
    onMessageSearchQueryChange: (String) -> Unit,
    onShowBookmarks: (Boolean) -> Unit,
    onShowConversationDetails: (Boolean) -> Unit,
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
    onSpaceQueryChange: (String) -> Unit,
    onFavoriteSpacesOnlyChange: (Boolean) -> Unit,
    onSpaceRefresh: () -> Unit,
    onOpenSpace: (SpaceSummary) -> Unit,
    onCloseSpace: () -> Unit,
    onOpenSpaceChannel: (SpaceChannel) -> Unit,
    onCloseSpaceChannel: () -> Unit,
    onSpaceComposerChange: (String) -> Unit,
    onSendSpaceMessage: () -> Unit,
    onReplySpaceMessage: (SpaceMessage?) -> Unit,
    onReactSpaceMessage: (SpaceMessage, String?) -> Unit,
    onShowCreateSpace: (Boolean) -> Unit,
    onCreateSpace: (String, String, String, List<String>) -> Unit,
    onShowCreateSpaceChannel: (Boolean) -> Unit,
    onCreateSpaceChannel: (String, String, SpaceChannelKind) -> Unit,
    onShowSpaceInfo: (Boolean) -> Unit,
    onToggleSpaceFavorite: (SpaceSummary) -> Unit,
    onToggleSpaceMute: (SpaceSummary) -> Unit,
    onLeaveSpace: (SpaceSummary) -> Unit,
    onDismissSpaceNotice: () -> Unit,
    onStartChat: (UserProfile) -> Unit,
    onPeopleTabSelected: (PeopleTab) -> Unit,
    onPeopleQueryChange: (String) -> Unit,
    onPeopleRefresh: () -> Unit,
    onOpenPerson: (UserProfile) -> Unit,
    onClosePerson: () -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
    onRemove: (String, String) -> Unit,
    onBlock: (UserProfile) -> Unit,
    onUnblock: (UserProfile) -> Unit,
    onShowShare: (Boolean) -> Unit,
    onShowPrivacy: (Boolean) -> Unit,
    onShowBlocked: (Boolean) -> Unit,
    onUpdatePrivacy: (Boolean, Boolean) -> Unit,
    onDismissPeopleNotice: () -> Unit,
    onShowMediaStorage: (Boolean) -> Unit,
    onClearDownloadedMedia: () -> Unit,
    onSignOut: () -> Unit,
) {
    val profile = state.profile ?: return
    if (state.chat.activeConversation != null) {
        LunaraBackdrop {
            ChatThreadScreen(
                state = state.chat,
                currentUserId = state.session?.userId.orEmpty(),
                onBack = onCloseConversation,
                onComposerChange = onComposerChange,
                onSend = onSendMessage,
                onLoadOlder = onLoadOlderMessages,
                onSelectMessage = onSelectMessage,
                onRetry = onRetryMessage,
                onCancelContext = onCancelComposerContext,
                onDismissNotice = onDismissChatNotice,
                onReply = onReplyMessage,
                onEdit = onEditMessage,
                onDelete = onDeleteMessage,
                onReact = onReactMessage,
                onBookmark = onBookmarkMessage,
                onShowSearch = onShowMessageSearch,
                onSearchQueryChange = onMessageSearchQueryChange,
                onShowBookmarks = onShowBookmarks,
                onShowDetails = onShowConversationDetails,
                onShowModuleComposer = onShowModuleComposer,
                onShowAttachmentPicker = onShowAttachmentPicker,
                onCreateCameraCapture = onCreateCameraCapture,
                onMediaSelected = onMediaSelected,
                onClearPendingMedia = onClearPendingMedia,
                onStartVoiceRecording = onStartVoiceRecording,
                onPauseResumeVoice = onPauseResumeVoice,
                onCancelVoice = onCancelVoice,
                onFinishVoice = onFinishVoice,
                onDownloadMedia = onDownloadMedia,
                onShowMediaGallery = onShowMediaGallery,
                onSendModule = onSendModule,
                onUpdateModule = onUpdateModule,
                onOpenOrganizer = onOpenConversationOrganizer,
                onSaveSettings = onSaveConversationSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
            )
        }
        return
    }

    LunaraBackdrop {
        val immersiveSpaceChannel = state.homeTab == HomeTab.Spaces && state.spaces.activeChannel != null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = if (immersiveSpaceChannel) 16.dp else 20.dp)
                .padding(top = if (immersiveSpaceChannel) 8.dp else 17.dp, bottom = if (immersiveSpaceChannel) 8.dp else 13.dp),
        ) {
            if (!immersiveSpaceChannel) {
                HomeHeader(state = state, profile = profile)
                Spacer(Modifier.height(18.dp))
            }

            AnimatedContent(
                targetState = state.homeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "homeTab",
                modifier = Modifier.weight(1f),
            ) { tab ->
                when (tab) {
                    HomeTab.Chats -> ChatsScreen(
                        state = state.chat,
                        onQueryChange = onChatQueryChange,
                        onFilterChange = onConversationFilterChange,
                        onRefresh = onChatRefresh,
                        onOpenConversation = onOpenConversation,
                        onOpenOrganizer = onOpenConversationOrganizer,
                        onSaveSettings = onSaveConversationSettings,
                        onFindPeople = { onTabSelected(HomeTab.People) },
                        onDismissNotice = onDismissChatNotice,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HomeTab.People -> PeopleScreen(
                        state = state.people,
                        currentProfile = profile,
                        onTabSelected = onPeopleTabSelected,
                        onQueryChange = onPeopleQueryChange,
                        onRefresh = onPeopleRefresh,
                        onOpenPerson = onOpenPerson,
                        onClosePerson = onClosePerson,
                        onSendRequest = onSendRequest,
                        onAccept = onAccept,
                        onReject = onReject,
                        onCancel = onCancel,
                        onRemove = onRemove,
                        onStartChat = onStartChat,
                        onBlock = onBlock,
                        onUnblock = onUnblock,
                        onShowShare = onShowShare,
                        onShowPrivacy = onShowPrivacy,
                        onShowBlocked = onShowBlocked,
                        onUpdatePrivacy = onUpdatePrivacy,
                        onDismissNotice = onDismissPeopleNotice,
                    )
                    HomeTab.Spaces -> SpaceHubScreen(
                        state = state.spaces,
                        currentUserId = state.session?.userId.orEmpty(),
                        availablePeople = state.people.snapshot.connections.map { it.person },
                        onQueryChange = onSpaceQueryChange,
                        onFavoriteOnlyChange = onFavoriteSpacesOnlyChange,
                        onRefresh = onSpaceRefresh,
                        onOpenSpace = onOpenSpace,
                        onCloseSpace = onCloseSpace,
                        onOpenChannel = onOpenSpaceChannel,
                        onCloseChannel = onCloseSpaceChannel,
                        onComposerChange = onSpaceComposerChange,
                        onSend = onSendSpaceMessage,
                        onReply = onReplySpaceMessage,
                        onReact = onReactSpaceMessage,
                        onShowCreateSpace = onShowCreateSpace,
                        onCreateSpace = onCreateSpace,
                        onShowCreateChannel = onShowCreateSpaceChannel,
                        onCreateChannel = onCreateSpaceChannel,
                        onShowInfo = onShowSpaceInfo,
                        onToggleFavorite = onToggleSpaceFavorite,
                        onToggleMute = onToggleSpaceMute,
                        onLeaveSpace = onLeaveSpace,
                        onDismissNotice = onDismissSpaceNotice,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HomeTab.You -> ProfileOverview(
                        profile = profile,
                        onShare = { onShowShare(true) },
                        onPrivacy = { onShowPrivacy(true) },
                        onBlocked = { onShowBlocked(true) },
                        onStorage = { onShowMediaStorage(true) },
                        onSignOut = onSignOut,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (!immersiveSpaceChannel) {
                Spacer(Modifier.height(12.dp))
                BottomBar(selected = state.homeTab, onSelected = onTabSelected)
            }
        }

        if (state.homeTab == HomeTab.Chats) {
            FloatingActionButton(
                onClick = { onTabSelected(HomeTab.People) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 24.dp, bottom = 89.dp),
                containerColor = Violet,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Find someone")
            }
        }
        if (state.people.showShareCard) {
            ShareProfileSheet(profile = profile, onDismiss = { onShowShare(false) })
        }
        if (state.people.showPrivacy) {
            PrivacySheet(
                profile = profile,
                loading = state.people.actionId == "privacy",
                onDismiss = { onShowPrivacy(false) },
                onSave = onUpdatePrivacy,
                onBlocked = {
                    onShowPrivacy(false)
                    onShowBlocked(true)
                },
            )
        }
        if (state.people.showBlocked) {
            BlockedSheet(
                people = state.people.snapshot.blocked,
                actionId = state.people.actionId,
                onDismiss = { onShowBlocked(false) },
                onUnblock = onUnblock,
            )
        }

        if (state.media.showStorage) {
            MediaStorageSheet(
                snapshot = state.media.snapshot,
                clearing = state.media.isClearing,
                onClearDownloads = onClearDownloadedMedia,
                onDismiss = { onShowMediaStorage(false) },
            )
        }
    }
}

@Composable
private fun HomeHeader(state: AppUiState, profile: UserProfile) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = when (state.homeTab) {
                    HomeTab.Chats -> "Good to see you,"
                    HomeTab.People -> "Find your people"
                    HomeTab.Spaces -> "Shared spaces"
                    HomeTab.You -> "Your profile"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when (state.homeTab) {
                    HomeTab.Chats -> profile.displayName
                    HomeTab.People -> "People"
                    HomeTab.Spaces -> "Spaces"
                    HomeTab.You -> profile.displayName
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        ConnectionPill(state.isOnline)
        Spacer(Modifier.size(10.dp))
        AvatarOrb(profile.avatarSeed, profile.displayName, Modifier.size(48.dp))
    }
}

@Composable
private fun ProfileOverview(
    profile: UserProfile,
    onShare: () -> Unit,
    onPrivacy: () -> Unit,
    onBlocked: () -> Unit,
    onStorage: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        AvatarOrb(profile.avatarSeed, profile.displayName, Modifier.size(92.dp))
        Spacer(Modifier.height(15.dp))
        Text(profile.displayName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("@${profile.username}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(profile.bio, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Violet.copy(alpha = 0.18f), Mint.copy(alpha = 0.10f))),
                    RoundedCornerShape(22.dp),
                )
                .clickable(onClick = onShare)
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = Violet)
            }
            Spacer(Modifier.size(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Share your profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(profile.resolvedShareCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        ProfileAction("Discovery & privacy", "Control discovery and requests", Icons.Rounded.Tune, onPrivacy)
        Spacer(Modifier.height(10.dp))
        ProfileAction("Blocked profiles", "Review profiles you have blocked", Icons.Rounded.Block, onBlocked)
        Spacer(Modifier.height(10.dp))
        ProfileAction("Media & storage", "Review sent files and downloaded cache", Icons.Rounded.Storage, onStorage)
        Spacer(Modifier.weight(1f))
        Text(
            "Sign out",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f), CircleShape)
                .clickable(onClick = onSignOut)
                .padding(horizontal = 18.dp, vertical = 11.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), RoundedCornerShape(19.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(Violet.copy(alpha = 0.12f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BottomBar(selected: HomeTab, onSelected: (HomeTab) -> Unit) {
    val items = listOf(
        Triple("Chats", Icons.Rounded.ChatBubbleOutline, HomeTab.Chats),
        Triple("People", Icons.Rounded.Group, HomeTab.People),
        Triple("Spaces", Icons.Rounded.BubbleChart, HomeTab.Spaces),
        Triple("You", Icons.Rounded.PersonOutline, HomeTab.You),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { (label, icon, tab) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected == tab) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                        RoundedCornerShape(17.dp),
                    )
                    .clickable { onSelected(tab) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

