package com.mohnishraj.lunara.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mohnishraj.lunara.domain.ConnectionRequest
import com.mohnishraj.lunara.domain.ConnectedPerson
import com.mohnishraj.lunara.domain.RelationshipState
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.relationshipFor
import com.mohnishraj.lunara.ui.PeopleTab
import com.mohnishraj.lunara.ui.PeopleUiState
import com.mohnishraj.lunara.ui.components.AvatarOrb
import com.mohnishraj.lunara.ui.theme.Mint
import com.mohnishraj.lunara.ui.theme.Peach
import com.mohnishraj.lunara.ui.theme.Violet

@Composable
fun PeopleScreen(
    state: PeopleUiState,
    currentProfile: UserProfile,
    onTabSelected: (PeopleTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPerson: (UserProfile) -> Unit,
    onClosePerson: () -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
    onRemove: (String, String) -> Unit,
    onStartChat: (UserProfile) -> Unit,
    onBlock: (UserProfile) -> Unit,
    onUnblock: (UserProfile) -> Unit,
    onShowShare: (Boolean) -> Unit,
    onShowPrivacy: (Boolean) -> Unit,
    onShowBlocked: (Boolean) -> Unit,
    onUpdatePrivacy: (Boolean, Boolean) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PeopleTabs(
            selected = state.tab,
            requestCount = state.snapshot.incoming.size,
            onSelected = onTabSelected,
        )
        Spacer(Modifier.height(14.dp))

        NoticeCard(message = state.notice, onDismiss = onDismissNotice)

        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "peopleTab",
            modifier = Modifier.weight(1f),
        ) { tab ->
            when (tab) {
                PeopleTab.Discover -> DiscoverPeople(
                    state = state,
                    onQueryChange = onQueryChange,
                    onRefresh = onRefresh,
                    onOpenPerson = onOpenPerson,
                    onSendRequest = onSendRequest,
                    onAccept = onAccept,
                    onCancel = onCancel,
                )
                PeopleTab.Requests -> RequestsList(
                    state = state,
                    onRefresh = onRefresh,
                    onOpenPerson = onOpenPerson,
                    onAccept = onAccept,
                    onReject = onReject,
                    onCancel = onCancel,
                )
                PeopleTab.Connections -> ConnectionsList(
                    state = state,
                    onRefresh = onRefresh,
                    onOpenPerson = onOpenPerson,
                    onRemove = onRemove,
                )
            }
        }
    }

    state.selectedPerson?.let { person ->
        PersonSheet(
            person = person,
            relationship = state.snapshot.relationshipFor(person.id),
            busy = state.actionId == person.id,
            request = state.snapshot.incoming.firstOrNull { it.person.id == person.id },
            outgoing = state.snapshot.outgoing.firstOrNull { it.person.id == person.id },
            connection = state.snapshot.connections.firstOrNull { it.person.id == person.id },
            onDismiss = onClosePerson,
            onSendRequest = { onSendRequest(person) },
            onAccept = { id -> onAccept(id, person.id) },
            onReject = { id -> onReject(id, person.id) },
            onCancel = { id -> onCancel(id, person.id) },
            onRemove = { id -> onRemove(id, person.id) },
            onStartChat = { onStartChat(person) },
            onBlock = { onBlock(person) },
            onUnblock = { onUnblock(person) },
        )
    }


}

@Composable
private fun PeopleTabs(selected: PeopleTab, requestCount: Int, onSelected: (PeopleTab) -> Unit) {
    val tabs = listOf(
        PeopleTab.Discover to "Discover",
        PeopleTab.Requests to if (requestCount > 0) "Requests $requestCount" else "Requests",
        PeopleTab.Connections to "Connected",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { (tab, title) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected == tab) Violet.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DiscoverPeople(
    state: PeopleUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPerson: (UserProfile) -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onAccept: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Name, @username or profile code") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.isSearching) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                } else if (state.query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onRefresh() }),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                focusedBorderColor = Violet,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
            ),
        )
        Spacer(Modifier.height(14.dp))
        if (state.results.isEmpty() && !state.isSearching) {
            EmptyPeople(
                icon = Icons.Rounded.Search,
                title = if (state.query.isBlank()) "A wider circle starts here" else "No matching profiles",
                subtitle = if (state.query.isBlank()) {
                    "Search by name, username or a profile code shared with you."
                } else {
                    "Try a different name or ask them to share their profile code."
                },
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.results, key = UserProfile::id) { person ->
                    val relationship = state.snapshot.relationshipFor(person.id)
                    PersonCard(
                        person = person,
                        relationship = relationship,
                        busy = state.actionId == person.id,
                        onClick = { onOpenPerson(person) },
                        onPrimary = when (relationship) {
                            RelationshipState.None -> if (person.allowRequests) ({ onSendRequest(person) }) else null
                            RelationshipState.IncomingRequest -> {
                                val request = state.snapshot.incoming.firstOrNull { it.person.id == person.id }
                                request?.let { incoming -> ({ onAccept(incoming.id, person.id) }) }
                            }
                            else -> null
                        },
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun RequestsList(
    state: PeopleUiState,
    onRefresh: () -> Unit,
    onOpenPerson: (UserProfile) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
) {
    val incoming = state.snapshot.incoming
    val outgoing = state.snapshot.outgoing
    if (state.isLoading && incoming.isEmpty() && outgoing.isEmpty()) {
        LoadingCenter()
        return
    }
    if (incoming.isEmpty() && outgoing.isEmpty()) {
        EmptyPeople(
            icon = Icons.Rounded.GroupAdd,
            title = "Nothing waiting on you",
            subtitle = "New requests and the ones you send will appear here.",
            action = "Refresh",
            onAction = onRefresh,
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (incoming.isNotEmpty()) {
            item { SectionLabel("Waiting for you", "${incoming.size}") }
            items(incoming, key = ConnectionRequest::id) { request ->
                RequestCard(
                    request = request,
                    busy = state.actionId == request.person.id,
                    onClick = { onOpenPerson(request.person) },
                    onAccept = { onAccept(request.id, request.person.id) },
                    onSecondary = { onReject(request.id, request.person.id) },
                    secondaryLabel = "Decline",
                )
            }
        }
        if (outgoing.isNotEmpty()) {
            item { SectionLabel("Sent by you", "${outgoing.size}") }
            items(outgoing, key = ConnectionRequest::id) { request ->
                RequestCard(
                    request = request,
                    busy = state.actionId == request.person.id,
                    onClick = { onOpenPerson(request.person) },
                    onAccept = null,
                    onSecondary = { onCancel(request.id, request.person.id) },
                    secondaryLabel = "Cancel",
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ConnectionsList(
    state: PeopleUiState,
    onRefresh: () -> Unit,
    onOpenPerson: (UserProfile) -> Unit,
    onRemove: (String, String) -> Unit,
) {
    val connections = state.snapshot.connections
    if (state.isLoading && connections.isEmpty()) {
        LoadingCenter()
        return
    }
    if (connections.isEmpty()) {
        EmptyPeople(
            icon = Icons.Rounded.Link,
            title = "Your circle is still open",
            subtitle = "Accept a request or discover someone to connect with.",
            action = "Refresh",
            onAction = onRefresh,
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel("Your connections", "${connections.size}") }
        items(connections, key = ConnectedPerson::connectionId) { connected ->
            PersonCard(
                person = connected.person,
                relationship = RelationshipState.Connected,
                busy = state.actionId == connected.person.id,
                onClick = { onOpenPerson(connected.person) },
                onPrimary = null,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PersonCard(
    person: UserProfile,
    relationship: RelationshipState,
    busy: Boolean,
    onClick: () -> Unit,
    onPrimary: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarOrb(person.avatarSeed, person.displayName, Modifier.size(52.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${person.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            if (person.bio.isNotBlank()) {
                Text(
                    person.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        if (busy) {
            CircularProgressIndicator(Modifier.size(23.dp), strokeWidth = 2.dp)
        } else {
            val (label, icon, color) = when (relationship) {
                RelationshipState.None -> if (person.allowRequests) {
                    Triple("Connect", Icons.Rounded.PersonAdd, Violet)
                } else {
                    Triple("Closed", Icons.Rounded.Lock, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RelationshipState.IncomingRequest -> Triple("Accept", Icons.Rounded.Check, Mint)
                RelationshipState.OutgoingRequest -> Triple("Sent", Icons.AutoMirrored.Rounded.Send, Peach)
                RelationshipState.Connected -> Triple("Connected", Icons.Rounded.Link, Mint)
                RelationshipState.Blocked -> Triple("Blocked", Icons.Rounded.Block, MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .then(if (onPrimary != null) Modifier.clickable(onClick = onPrimary) else Modifier)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: ConnectionRequest,
    busy: Boolean,
    onClick: () -> Unit,
    onAccept: (() -> Unit)?,
    onSecondary: () -> Unit,
    secondaryLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarOrb(request.person.avatarSeed, request.person.displayName, Modifier.size(50.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(request.person.displayName, style = MaterialTheme.typography.titleMedium)
                Text("@${request.person.username}", color = MaterialTheme.colorScheme.primary)
            }
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (onAccept != null) {
                Button(
                    onClick = onAccept,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet),
                ) { Text("Accept") }
            }
            OutlinedButton(
                onClick = onSecondary,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text(secondaryLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonSheet(
    person: UserProfile,
    relationship: RelationshipState,
    busy: Boolean,
    request: ConnectionRequest?,
    outgoing: ConnectionRequest?,
    connection: ConnectedPerson?,
    onDismiss: () -> Unit,
    onSendRequest: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
    onStartChat: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarOrb(person.avatarSeed, person.displayName, Modifier.size(88.dp))
            Spacer(Modifier.height(14.dp))
            Text(person.displayName, style = MaterialTheme.typography.headlineSmall)
            Text("@${person.username}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            if (person.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(person.bio, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(9.dp))
            Text(person.resolvedShareCode, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))

            when (relationship) {
                RelationshipState.None -> {
                    if (person.allowRequests) {
                        SheetPrimary("Send request", busy, onSendRequest)
                    } else {
                        Text(
                            "Not accepting new requests",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    }
                }
                RelationshipState.IncomingRequest -> {
                    SheetPrimary("Accept request", busy) { request?.id?.let(onAccept) }
                    SheetSecondary("Decline") { request?.id?.let(onReject) }
                }
                RelationshipState.OutgoingRequest -> SheetSecondary("Cancel request") { outgoing?.id?.let(onCancel) }
                RelationshipState.Connected -> {
                    SheetPrimary("Message", busy, onStartChat)
                    Spacer(Modifier.height(8.dp))
                    SheetSecondary("Remove connection") { connection?.connectionId?.let(onRemove) }
                }
                RelationshipState.Blocked -> SheetPrimary("Unblock profile", busy, onUnblock)
            }
            if (relationship != RelationshipState.Blocked) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Block profile",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !busy, onClick = onBlock)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareProfileSheet(profile: UserProfile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareText = "Find me on Lunara: @${profile.username}\nProfile code: ${profile.resolvedShareCode}"
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Share your profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("A username, a code, or one quick scan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.clip(RoundedCornerShape(26.dp)).background(Color.White).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                QrCode(data = "lunara://profile/${profile.username}", modifier = Modifier.size(210.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("@${profile.username}", style = MaterialTheme.typography.titleLarge)
            Text(profile.resolvedShareCode, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share profile"))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share profile")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacySheet(
    profile: UserProfile,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean) -> Unit,
    onBlocked: () -> Unit,
) {
    var discoverable by remember(profile.isDiscoverable) { mutableStateOf(profile.isDiscoverable) }
    var allowRequests by remember(profile.allowRequests) { mutableStateOf(profile.allowRequests) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp)) {
            Text("Privacy", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("Choose how new people can find and reach you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            PrivacyRow(
                title = "Appear in discovery",
                subtitle = "People can find your profile by name or username.",
                checked = discoverable,
                onChecked = { discoverable = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            PrivacyRow(
                title = "Allow connection requests",
                subtitle = "New people can send you a request.",
                checked = allowRequests,
                onChecked = { allowRequests = it },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onBlocked).padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(12.dp))
                Text("Blocked profiles", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Rounded.MoreHoriz, contentDescription = null)
            }
            Spacer(Modifier.height(18.dp))
            SheetPrimary("Save changes", loading) { onSave(discoverable, allowRequests) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockedSheet(
    people: List<UserProfile>,
    actionId: String?,
    onDismiss: () -> Unit,
    onUnblock: (UserProfile) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp)) {
            Text("Blocked profiles", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("They cannot send requests or appear in your circle.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            if (people.isEmpty()) {
                EmptyPeople(Icons.Rounded.Lock, "No blocked profiles", "Profiles you block will appear here.")
            } else {
                people.forEach { person ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarOrb(person.avatarSeed, person.displayName, Modifier.size(48.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("@${person.username}", color = MaterialTheme.colorScheme.primary)
                        }
                        if (actionId == person.id) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text(
                            "Unblock",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { onUnblock(person) }.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(14.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SectionLabel(title: String, count: String) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(count, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NoticeCard(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = message != null) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp))
                .background(Mint.copy(alpha = 0.12f)).clickable(onClick = onDismiss).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Mint)
            Spacer(Modifier.size(10.dp))
            Text(message.orEmpty(), modifier = Modifier.weight(1f), color = Mint, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Mint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyPeople(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(30.dp)).background(Violet.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onAction).padding(10.dp))
        }
    }
}

@Composable
private fun LoadingCenter() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Violet)
    }
}

@Composable
private fun SheetPrimary(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Violet),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(label)
    }
}

@Composable
private fun SheetSecondary(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(17.dp),
    ) { Text(label) }
}

@Composable
private fun QrCode(data: String, modifier: Modifier = Modifier) {
    val bitmap = remember(data) {
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 512, 512)
        Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Profile QR code", modifier = modifier)
}
