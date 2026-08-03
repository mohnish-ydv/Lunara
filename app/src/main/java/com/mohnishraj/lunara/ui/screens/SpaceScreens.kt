package com.mohnishraj.lunara.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohnishraj.lunara.domain.MessageReaction
import com.mohnishraj.lunara.domain.SpaceChannel
import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceDetail
import com.mohnishraj.lunara.domain.SpaceMember
import com.mohnishraj.lunara.domain.SpaceMessage
import com.mohnishraj.lunara.domain.SpaceRole
import com.mohnishraj.lunara.domain.SpaceSummary
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.ui.SpaceUiState
import com.mohnishraj.lunara.ui.components.AvatarOrb
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Rose
import com.mohnishraj.lunara.ui.theme.Violet

@Composable
fun SpaceHubScreen(
    state: SpaceUiState,
    currentUserId: String,
    availablePeople: List<UserProfile>,
    onQueryChange: (String) -> Unit,
    onFavoriteOnlyChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenSpace: (SpaceSummary) -> Unit,
    onCloseSpace: () -> Unit,
    onOpenChannel: (SpaceChannel) -> Unit,
    onCloseChannel: () -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onReply: (SpaceMessage?) -> Unit,
    onReact: (SpaceMessage, String?) -> Unit,
    onShowCreateSpace: (Boolean) -> Unit,
    onCreateSpace: (String, String, String, List<String>) -> Unit,
    onShowCreateChannel: (Boolean) -> Unit,
    onCreateChannel: (String, String, SpaceChannelKind) -> Unit,
    onShowInfo: (Boolean) -> Unit,
    onToggleFavorite: (SpaceSummary) -> Unit,
    onToggleMute: (SpaceSummary) -> Unit,
    onLeaveSpace: (SpaceSummary) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.activeSpace != null && state.activeChannel != null -> SpaceChannelThread(
            state = state,
            currentUserId = currentUserId,
            onBack = onCloseChannel,
            onComposerChange = onComposerChange,
            onSend = onSend,
            onReply = onReply,
            onReact = onReact,
            onDismissNotice = onDismissNotice,
            modifier = modifier,
        )
        state.activeSpace != null -> SpaceDetailScreen(
            state = state,
            onBack = onCloseSpace,
            onOpenChannel = onOpenChannel,
            onShowCreateChannel = { onShowCreateChannel(true) },
            onShowInfo = { onShowInfo(true) },
            onToggleFavorite = { onToggleFavorite(state.activeSpace.space) },
            onToggleMute = { onToggleMute(state.activeSpace.space) },
            modifier = modifier,
        )
        else -> SpaceListScreen(
            state = state,
            onQueryChange = onQueryChange,
            onFavoriteOnlyChange = onFavoriteOnlyChange,
            onRefresh = onRefresh,
            onOpenSpace = onOpenSpace,
            onCreate = { onShowCreateSpace(true) },
            onDismissNotice = onDismissNotice,
            modifier = modifier,
        )
    }

    if (state.showCreateSpace) {
        CreateSpaceSheet(
            people = availablePeople,
            loading = state.actionId == "create-space",
            onDismiss = { onShowCreateSpace(false) },
            onCreate = onCreateSpace,
        )
    }
    if (state.showCreateChannel && state.activeSpace != null) {
        CreateChannelSheet(
            loading = state.actionId == "create-channel",
            onDismiss = { onShowCreateChannel(false) },
            onCreate = onCreateChannel,
        )
    }
    if (state.showInfo && state.activeSpace != null) {
        SpaceInfoSheet(
            detail = state.activeSpace,
            leaving = state.actionId == "leave-space",
            onDismiss = { onShowInfo(false) },
            onToggleFavorite = { onToggleFavorite(state.activeSpace.space) },
            onToggleMute = { onToggleMute(state.activeSpace.space) },
            onLeave = { onLeaveSpace(state.activeSpace.space) },
        )
    }
}

@Composable
private fun SpaceListScreen(
    state: SpaceUiState,
    onQueryChange: (String) -> Unit,
    onFavoriteOnlyChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenSpace: (SpaceSummary) -> Unit,
    onCreate: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier,
) {
    val visible = state.spaces.filter { it.matches(state.query) && (!state.favoriteOnly || it.isFavorite) }
    Column(modifier) {
        SpacePulseCard(state.spaces)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search spaces") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    focusedBorderColor = Violet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                ),
            )
            Surface(
                modifier = Modifier.size(54.dp).clickable { onFavoriteOnlyChange(!state.favoriteOnly) },
                shape = RoundedCornerShape(18.dp),
                color = if (state.favoriteOnly) Violet.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (state.favoriteOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite spaces",
                        tint = if (state.favoriteOnly) Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your spaces", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh spaces") }
            FilledTonalButton(onClick = onCreate, shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create")
            }
        }
        AnimatedVisibility(state.notice != null) {
            state.notice?.let { NoticeCard(it, onDismissNotice) }
        }
        Spacer(Modifier.height(8.dp))
        if (state.isLoading && state.spaces.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (visible.isEmpty()) {
            EmptySpaces(onCreate)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxSize()) {
                items(visible, key = SpaceSummary::id) { space -> SpaceCard(space) { onOpenSpace(space) } }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

@Composable
private fun SpacePulseCard(spaces: List<SpaceSummary>) {
    val unread = spaces.sumOf(SpaceSummary::unreadCount)
    val members = spaces.sumOf(SpaceSummary::memberCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.26f), Mint.copy(alpha = 0.13f))))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Group, contentDescription = null, tint = Mint) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("Shared momentum", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("${spaces.size} spaces · $members member seats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(unread.toString(), style = MaterialTheme.typography.headlineMedium, color = if (unread > 0) Mint else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("unread", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpaceCard(space: SpaceSummary, onClick: () -> Unit) {
    val accent = listOf(Violet, Mint, Peach, Rose)[kotlin.math.abs(space.accentSeed) % 4]
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(space.emoji, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(space.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (space.isFavorite) Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Violet, modifier = Modifier.size(17.dp))
                    if (space.isMuted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                    }
                }
                Text(space.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetaPill("${space.memberCount} members", Icons.Rounded.Group)
                    MetaPill("${space.channelCount} channels", Icons.Rounded.Tag)
                }
            }
            if (space.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(30.dp).background(Violet, CircleShape), contentAlignment = Alignment.Center) {
                    Text(space.unreadCount.coerceAtMost(99).toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, icon: ImageVector) {
    Row(
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptySpaces(onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).background(Violet.copy(alpha = 0.14f), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
            Text("✨", style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(20.dp))
        Text("Build a place that stays clear", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Create a space for a team, project, class or close circle.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCreate, shape = RoundedCornerShape(16.dp)) { Text("Create your first space") }
    }
}

@Composable
private fun SpaceDetailScreen(
    state: SpaceUiState,
    onBack: () -> Unit,
    onOpenChannel: (SpaceChannel) -> Unit,
    onShowCreateChannel: () -> Unit,
    onShowInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier,
) {
    val detail = state.activeSpace ?: return
    Column(modifier) {
        SpaceDetailHeader(detail, onBack, onShowInfo, onToggleFavorite)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailMetric(detail.space.memberCount.toString(), "members", Modifier.weight(1f))
            DetailMetric(detail.space.channelCount.toString(), "channels", Modifier.weight(1f))
            DetailMetric(detail.space.unreadCount.toString(), "unread", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Channels", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleMute) {
                Icon(Icons.Rounded.NotificationsOff, contentDescription = "Toggle mute", tint = if (detail.space.isMuted) Violet else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (detail.space.canManage) {
                FilledTonalButton(onClick = onShowCreateChannel, shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Channel")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.isLoadingDetail) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(detail.channels, key = SpaceChannel::id) { channel -> ChannelCard(channel) { onOpenChannel(channel) } }
                item {
                    Text("Members", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
                items(detail.members.take(8), key = { it.profile.id }) { member -> MemberRow(member) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SpaceDetailHeader(
    detail: SpaceDetail,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onFavorite: () -> Unit,
) {
    val space = detail.space
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f), shape = RoundedCornerShape(25.dp)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Violet.copy(alpha = 0.20f), Mint.copy(alpha = 0.08f)))).padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(48.dp).background(Violet.copy(alpha = 0.20f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(space.emoji, style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(space.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(space.myRole.name, style = MaterialTheme.typography.bodySmall, color = Mint)
                }
                IconButton(onClick = onFavorite) {
                    Icon(if (space.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Favorite", tint = if (space.isFavorite) Violet else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onInfo) { Icon(Icons.Rounded.Info, contentDescription = "Space information", tint = MaterialTheme.colorScheme.onSurface) }
            }
            if (space.description.isNotBlank()) {
                Text(space.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun DetailMetric(value: String, label: String, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChannelCard(channel: SpaceChannel, onClick: () -> Unit) {
    val icon = when (channel.kind) {
        SpaceChannelKind.Chat -> Icons.Rounded.Tag
        SpaceChannelKind.Announcements -> Icons.Rounded.Campaign
        SpaceChannelKind.Planning -> Icons.Rounded.Tune
    }
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Violet.copy(alpha = 0.14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Violet)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("# ${channel.displayName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(channel.lastMessage.ifBlank { channel.description.ifBlank { "No messages yet" } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (channel.unreadCount > 0) {
                Box(Modifier.size(28.dp).background(Mint.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(channel.unreadCount.toString(), color = Mint, style = MaterialTheme.typography.labelLarge)
                }
            } else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemberRow(member: SpaceMember) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AvatarOrb(member.profile.avatarSeed, member.profile.displayName, Modifier.size(42.dp))
                if (member.isOnline) Box(Modifier.align(Alignment.BottomEnd).size(11.dp).background(Mint, CircleShape))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(member.profile.displayName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("@${member.profile.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RolePill(member.role)
        }
    }
}

@Composable
private fun RolePill(role: SpaceRole) {
    val color = when (role) {
        SpaceRole.Owner -> Peach
        SpaceRole.Admin -> Violet
        SpaceRole.Member -> Mint
    }
    Text(
        role.name,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.background(color.copy(alpha = 0.13f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
private fun SpaceChannelThread(
    state: SpaceUiState,
    currentUserId: String,
    onBack: () -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onReply: (SpaceMessage?) -> Unit,
    onReact: (SpaceMessage, String?) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier,
) {
    val channel = state.activeChannel ?: return
    val space = state.activeSpace?.space ?: return
    val canPost = channel.kind != SpaceChannelKind.Announcements || space.canManage
    Column(modifier) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f), shape = RoundedCornerShape(23.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface) }
                Box(Modifier.size(42.dp).background(Violet.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(if (channel.kind == SpaceChannelKind.Announcements) Icons.Rounded.Campaign else Icons.Rounded.Tag, contentDescription = null, tint = Violet)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("# ${channel.displayName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(space.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        AnimatedVisibility(state.notice != null) { state.notice?.let { NoticeCard(it, onDismissNotice) } }
        Box(Modifier.weight(1f)) {
            if (state.isLoadingMessages) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.messages.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (channel.kind == SpaceChannelKind.Announcements) "Make the first announcement" else "Start the conversation", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text("This channel is ready when your group is.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    reverseLayout = true,
                ) {
                    items(state.messages.asReversed(), key = SpaceMessage::id) { message ->
                        SpaceMessageCard(message, message.isMine(currentUserId), onReply, onReact)
                    }
                }
            }
        }
        state.replyTo?.let { reply ->
            Row(
                Modifier.fillMaxWidth().background(Violet.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Replying to ${reply.sender.displayName}", style = MaterialTheme.typography.labelLarge, color = Violet)
                    Text(reply.body, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onReply(null) }) { Icon(Icons.Rounded.Close, contentDescription = "Cancel reply") }
            }
            Spacer(Modifier.height(7.dp))
        }
        if (canPost) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.composer,
                    onValueChange = onComposerChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (channel.kind == SpaceChannelKind.Announcements) "Write an announcement" else "Message #${channel.displayName}") },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        focusedBorderColor = Violet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    ),
                )
                Surface(
                    modifier = Modifier.size(56.dp).clickable(enabled = state.composer.isNotBlank() && state.actionId != "send-space-message", onClick = onSend),
                    shape = RoundedCornerShape(19.dp),
                    color = if (state.composer.isNotBlank()) Violet else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.actionId == "send-space-message") CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = if (state.composer.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Violet)
                    Spacer(Modifier.width(10.dp))
                    Text("Only owners and admins can post here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SpaceMessageCard(
    message: SpaceMessage,
    mine: Boolean,
    onReply: (SpaceMessage?) -> Unit,
    onReact: (SpaceMessage, String?) -> Unit,
) {
    val alignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (mine) Brush.linearGradient(listOf(Violet.copy(alpha = 0.95f), Color(0xFF6552D9))) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)))
                .clickable { onReply(message) }
                .padding(13.dp),
        ) {
            if (!mine) Text(message.sender.displayName, style = MaterialTheme.typography.labelLarge, color = Mint)
            if (message.replyPreview.isNotBlank()) {
                Text(
                    message.replyPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mine) Color.White.copy(alpha = 0.74f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.10f), RoundedCornerShape(10.dp)).padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
            }
            Text(message.body, color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            if (message.reactions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    message.reactions.forEach { reaction -> ReactionChip(reaction) { onReact(message, if (reaction.reactedByMe) null else reaction.emoji) } }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("✨", "❤️", "👍").forEach { emoji ->
                        Text(emoji, modifier = Modifier.clickable { onReact(message, emoji) }.padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionChip(reaction: MessageReaction, onClick: () -> Unit) {
    Row(
        Modifier.background(if (reaction.reactedByMe) Mint.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f), CircleShape).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(reaction.emoji)
        Text(reaction.count.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSpaceSheet(
    people: List<UserProfile>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    val selected = remember { mutableStateListOf<String>() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create a space", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Start focused. You can add more people and channels later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(emoji, { emoji = it.take(4) }, Modifier.width(92.dp), label = { Text("Emoji") }, singleLine = true)
                OutlinedTextField(name, { name = it.take(48) }, Modifier.weight(1f), label = { Text("Space name") }, singleLine = true)
            }
            OutlinedTextField(description, { description = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("What is this space for?") }, minLines = 2, maxLines = 4)
            Text("Invite connections", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (people.isEmpty()) {
                Text("Connect with people first, or create the space for yourself.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    people.forEach { person ->
                        val chosen = person.id in selected
                        Surface(
                            modifier = Modifier.clickable {
                                if (chosen) selected.remove(person.id) else if (selected.size < 24) selected.add(person.id)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (chosen) Violet.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AvatarOrb(person.avatarSeed, person.displayName, Modifier.size(40.dp))
                                Spacer(Modifier.height(5.dp))
                                Text(person.displayName.substringBefore(' '), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                if (chosen) Icon(Icons.Rounded.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onCreate(name, description, emoji, selected.toList()) },
                enabled = name.trim().length >= 2 && !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Create space")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChannelSheet(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, SpaceChannelKind) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(SpaceChannelKind.Chat) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New channel", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpaceChannelKind.entries.forEach { option ->
                    FilterPill(option.name, option == kind) { kind = option }
                }
            }
            OutlinedTextField(name, { name = it.take(32) }, Modifier.fillMaxWidth(), label = { Text("Channel name") }, prefix = { Text("# ") }, singleLine = true)
            OutlinedTextField(description, { description = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2, maxLines = 3)
            Text(
                when (kind) {
                    SpaceChannelKind.Chat -> "Everyone can talk here."
                    SpaceChannelKind.Announcements -> "Only owners and admins can post."
                    SpaceChannelKind.Planning -> "A focused channel for decisions and next steps."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { onCreate(name, description, kind) }, enabled = name.trim().length >= 2 && !loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Create channel")
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.background(if (selected) Violet.copy(alpha = 0.17f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceInfoSheet(
    detail: SpaceDetail,
    leaving: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMute: () -> Unit,
    onLeave: () -> Unit,
) {
    val space = detail.space
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(74.dp).background(Violet.copy(alpha = 0.16f), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Text(space.emoji, style = MaterialTheme.typography.headlineLarge) }
            Spacer(Modifier.height(12.dp))
            Text(space.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(space.description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = Violet)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Invite code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(space.inviteCode.ifBlank { "Available after cloud setup" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FilledTonalButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(if (space.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (space.isFavorite) "Favorited" else "Favorite")
                }
                FilledTonalButton(onClick = onToggleMute, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (space.isMuted) "Unmute" else "Mute")
                }
            }
            Spacer(Modifier.height(10.dp))
            if (space.myRole != SpaceRole.Owner) {
                OutlinedButton(onClick = onLeave, enabled = !leaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    if (leaving) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp) else Text("Leave space")
                }
            } else {
                Text("Owners must transfer ownership before leaving.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NoticeCard(message: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(17.dp)) }
    }
}
