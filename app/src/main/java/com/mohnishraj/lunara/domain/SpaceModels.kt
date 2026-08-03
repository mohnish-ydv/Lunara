package com.mohnishraj.lunara.domain

enum class SpaceRole {
    Owner,
    Admin,
    Member,
}

enum class SpaceChannelKind {
    Chat,
    Announcements,
    Planning,
}

data class SpaceMember(
    val profile: UserProfile,
    val role: SpaceRole = SpaceRole.Member,
    val joinedAt: String = "",
    val isOnline: Boolean = false,
)

data class SpaceChannel(
    val id: String,
    val spaceId: String,
    val name: String,
    val description: String = "",
    val kind: SpaceChannelKind = SpaceChannelKind.Chat,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val isPinned: Boolean = false,
) {
    val displayName: String get() = name.trim().ifBlank { "channel" }
}

data class SpaceSummary(
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "✨",
    val accentSeed: Int = 0,
    val memberCount: Int = 1,
    val channelCount: Int = 1,
    val unreadCount: Int = 0,
    val lastActivityAt: String = "",
    val myRole: SpaceRole = SpaceRole.Member,
    val isFavorite: Boolean = false,
    val isMuted: Boolean = false,
    val inviteCode: String = "",
) {
    fun matches(query: String): Boolean {
        val needle = query.trim().lowercase()
        return needle.isBlank() ||
            name.lowercase().contains(needle) ||
            description.lowercase().contains(needle) ||
            inviteCode.lowercase().contains(needle)
    }

    val canManage: Boolean get() = myRole == SpaceRole.Owner || myRole == SpaceRole.Admin
}

data class SpaceMessage(
    val id: String,
    val clientId: String,
    val channelId: String,
    val sender: UserProfile,
    val body: String,
    val createdAt: String,
    val editedAt: String = "",
    val replyToId: String = "",
    val replyPreview: String = "",
    val reactions: List<MessageReaction> = emptyList(),
    val isAnnouncement: Boolean = false,
) {
    val isEdited: Boolean get() = editedAt.isNotBlank()
    fun isMine(userId: String): Boolean = sender.id == userId || sender.id == "me"
}

data class SpaceDetail(
    val space: SpaceSummary,
    val channels: List<SpaceChannel> = emptyList(),
    val members: List<SpaceMember> = emptyList(),
)

sealed interface SpaceSignal {
    data object Refresh : SpaceSignal
    data object Reconnected : SpaceSignal
}

fun validateSpaceName(value: String): Result<String> = runCatching {
    val clean = value.trim().replace(Regex("\\s+"), " ")
    require(clean.length in 2..48) { "Space name must be 2–48 characters" }
    clean
}

fun validateChannelName(value: String): Result<String> = runCatching {
    val clean = value.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-')
    require(clean.length in 2..32) { "Channel name must be 2–32 characters" }
    clean
}

fun validateSpaceMessage(value: String): Result<String> = runCatching {
    val clean = value.trim()
    require(clean.isNotBlank()) { "Message cannot be empty" }
    require(clean.length <= 4000) { "Messages can be up to 4000 characters" }
    clean
}
