package com.mohnishraj.lunara.domain

enum class MessageDeliveryState {
    Sending,
    Sent,
    Delivered,
    Read,
    Failed,
}

enum class ConversationFilter {
    All,
    Unread,
    Pinned,
    Muted,
    Archived,
}

data class MessageReaction(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean = false,
)

data class ChatMessage(
    val id: String,
    val clientId: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val createdAt: String,
    val editedAt: String = "",
    val deletedAt: String = "",
    val replyToId: String = "",
    val replyPreview: String = "",
    val deliveryState: MessageDeliveryState = MessageDeliveryState.Sent,
    val reactions: List<MessageReaction> = emptyList(),
    val isBookmarked: Boolean = false,
    val module: MessageModule? = null,
    val moduleRevision: Int = 0,
    val attachment: MediaAttachment? = null,
) {
    fun isMine(userId: String): Boolean = senderId == userId
    val isDeleted: Boolean get() = deletedAt.isNotBlank()
    val isEdited: Boolean get() = editedAt.isNotBlank() && !isDeleted
    val containsLink: Boolean get() = !isDeleted && (URL_PATTERN.containsMatchIn(body) || module?.locationName?.let(URL_PATTERN::containsMatchIn) == true)
    val previewText: String get() = when {
        isDeleted -> "Message removed"
        module != null -> module.previewText()
        attachment != null -> attachment.previewText()
        else -> body
    }

    companion object {
        private val URL_PATTERN = Regex("(?:https?://|www\\.)\\S+", RegexOption.IGNORE_CASE)
    }
}

data class ChatConversation(
    val id: String,
    val person: UserProfile,
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val lastActiveAt: String = "",
    val isTyping: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val labels: List<String> = emptyList(),
    val draft: String = "",
) {
    fun matches(query: String): Boolean {
        val needle = query.trim().lowercase()
        return needle.isBlank() ||
            person.displayName.lowercase().contains(needle) ||
            person.username.lowercase().contains(needle) ||
            lastMessage.lowercase().contains(needle) ||
            draft.lowercase().contains(needle) ||
            labels.any { it.lowercase().contains(needle) }
    }
}

data class ConversationSettings(
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isMuted: Boolean,
    val labels: List<String>,
)

data class ConversationInsight(
    val totalMessages: Int = 0,
    val bookmarkedMessages: Int = 0,
    val sharedLinks: Int = 0,
    val sharedMedia: Int = 0,
    val firstMessageAt: String = "",
)

sealed interface ChatSignal {
    data object Refresh : ChatSignal
    data object Reconnected : ChatSignal
}
