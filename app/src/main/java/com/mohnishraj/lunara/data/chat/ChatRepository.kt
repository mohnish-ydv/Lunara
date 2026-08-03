package com.mohnishraj.lunara.data.chat

import com.mohnishraj.lunara.domain.ChatConversation
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.ChatSignal
import com.mohnishraj.lunara.domain.ConversationSettings
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun conversations(session: UserSession): Result<List<ChatConversation>>
    suspend fun ensureConversation(session: UserSession, person: UserProfile): Result<ChatConversation>
    suspend fun messages(
        session: UserSession,
        conversationId: String,
        before: String? = null,
        limit: Int = 40,
    ): Result<List<ChatMessage>>
    suspend fun searchMessages(
        session: UserSession,
        conversationId: String,
        query: String,
        limit: Int = 80,
    ): Result<List<ChatMessage>>
    suspend fun bookmarkedMessages(
        session: UserSession,
        conversationId: String,
        limit: Int = 80,
    ): Result<List<ChatMessage>>
    suspend fun sharedLinks(
        session: UserSession,
        conversationId: String,
        limit: Int = 80,
    ): Result<List<ChatMessage>>
    suspend fun mediaMessages(
        session: UserSession,
        conversationId: String,
        limit: Int = 120,
    ): Result<List<ChatMessage>>
    suspend fun sendMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        body: String,
        replyToId: String? = null,
    ): Result<ChatMessage>
    suspend fun sendMediaMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        attachment: MediaAttachment,
        replyToId: String? = null,
        onProgress: (Int) -> Unit = {},
    ): Result<ChatMessage>
    suspend fun downloadMedia(
        session: UserSession,
        attachment: MediaAttachment,
        onProgress: (Int) -> Unit = {},
    ): Result<MediaAttachment>
    suspend fun sendModuleMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        module: MessageModule,
        replyToId: String? = null,
    ): Result<ChatMessage>
    suspend fun updateModule(
        session: UserSession,
        messageId: String,
        expectedRevision: Int,
        module: MessageModule,
    ): Result<Unit>
    suspend fun editMessage(session: UserSession, messageId: String, body: String): Result<Unit>
    suspend fun deleteMessage(session: UserSession, messageId: String): Result<Unit>
    suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit>
    suspend fun setBookmark(session: UserSession, messageId: String, bookmarked: Boolean): Result<Unit>
    suspend fun updateConversationSettings(
        session: UserSession,
        conversationId: String,
        settings: ConversationSettings,
    ): Result<Unit>
    suspend fun markDelivered(session: UserSession, conversationId: String): Result<Unit>
    suspend fun markRead(session: UserSession, conversationId: String): Result<Unit>
    suspend fun setTyping(session: UserSession, conversationId: String, typing: Boolean): Result<Unit>
    suspend fun setPresence(session: UserSession, online: Boolean): Result<Unit>
    fun observe(session: UserSession): Flow<ChatSignal>
}
