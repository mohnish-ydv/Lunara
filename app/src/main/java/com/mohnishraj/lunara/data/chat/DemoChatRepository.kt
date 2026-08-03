package com.mohnishraj.lunara.data.chat

import com.mohnishraj.lunara.domain.ChatConversation
import com.mohnishraj.lunara.domain.ChatMessage
import com.mohnishraj.lunara.domain.ChatSignal
import com.mohnishraj.lunara.domain.ConversationSettings
import com.mohnishraj.lunara.domain.MessageDeliveryState
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MessageModuleType
import com.mohnishraj.lunara.domain.ModuleChecklistItem
import com.mohnishraj.lunara.domain.ModulePollOption
import com.mohnishraj.lunara.domain.MessageReaction
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.MediaTransferState
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant
import java.util.UUID

class DemoChatRepository : ChatRepository {
    private val demoNow = Instant.now()
    private val signals = MutableSharedFlow<ChatSignal>(extraBufferCapacity = 24)
    private val people = listOf(
        UserProfile("10000000-0000-0000-0000-000000000001", "anaya@preview.lunara", "anaya", "Anaya Mehta", "Ideas, photos and unfinished playlists.", 3),
        UserProfile("10000000-0000-0000-0000-000000000004", "arjun_s@preview.lunara", "arjun_s", "Arjun Shah", "Coffee, code and Sunday football.", 0),
        UserProfile("10000000-0000-0000-0000-000000000006", "zoya@preview.lunara", "zoya", "Zoya Khan", "Design systems, soft gradients and good typography.", 5),
        UserProfile("10000000-0000-0000-0000-000000000008", "kabir@preview.lunara", "kabir", "Kabir Rao", "Building tiny things that feel useful.", 1),
    )
    private val conversations = linkedMapOf(
        "demo-chat-anaya" to ChatConversation(
            id = "demo-chat-anaya",
            person = people[0],
            lastMessage = "That color system feels much calmer now ✨",
            lastMessageAt = at(-420),
            unreadCount = 2,
            isOnline = true,
            isPinned = true,
            labels = listOf("Design"),
        ),
        "demo-chat-arjun" to ChatConversation(
            id = "demo-chat-arjun",
            person = people[1],
            lastMessage = "Sunday works for me.",
            lastMessageAt = at(-7200),
            lastActiveAt = at(-1800),
            isMuted = true,
            labels = listOf("Friends"),
        ),
        "demo-chat-zoya" to ChatConversation(
            id = "demo-chat-zoya",
            person = people[2],
            lastMessage = "I saved the reference board here: https://example.com/lunara-board",
            lastMessageAt = at(-17200),
            lastActiveAt = at(-3600),
            labels = listOf("Design", "Ideas"),
        ),
        "demo-chat-kabir" to ChatConversation(
            id = "demo-chat-kabir",
            person = people[3],
            lastMessage = "Let us revisit this after the weekend.",
            lastMessageAt = at(-172800),
            lastActiveAt = at(-86400),
            isArchived = true,
            labels = listOf("Later"),
        ),
    )
    private val messageMap = linkedMapOf(
        "demo-chat-anaya" to mutableListOf(
            message("a1", "demo-chat-anaya", people[0].id, "Hey! I saw the new profile flow.", -3600),
            message("a2", "demo-chat-anaya", "me", "Still refining it. Does the pacing feel right?", -3300, MessageDeliveryState.Read, bookmarked = true),
            message("a3", "demo-chat-anaya", people[0].id, "Much better. The screens breathe now.", -1200),
            moduleMessage(
                id = "a-module-1",
                conversationId = "demo-chat-anaya",
                senderId = "me",
                seconds = -780,
                module = MessageModule(
                    type = MessageModuleType.Checklist,
                    title = "Profile launch polish",
                    description = "Small details before the portfolio recording.",
                    items = listOf(
                        ModuleChecklistItem("copy", "Tighten empty-state copy", true),
                        ModuleChecklistItem("motion", "Check transition pacing"),
                        ModuleChecklistItem("contrast", "Verify light and dark contrast"),
                    ),
                ),
                state = MessageDeliveryState.Read,
            ),
            moduleMessage(
                id = "a-module-2",
                conversationId = "demo-chat-anaya",
                senderId = people[0].id,
                seconds = -610,
                module = MessageModule(
                    type = MessageModuleType.Poll,
                    title = "Which accent feels calmer?",
                    description = "Pick one for the next visual pass.",
                    options = listOf(
                        ModulePollOption("violet", "Soft violet", listOf(people[0].id)),
                        ModulePollOption("mint", "Mint glow", listOf("me")),
                        ModulePollOption("peach", "Warm peach"),
                    ),
                ),
            ),
            message("a4", "demo-chat-anaya", people[0].id, "That color system feels much calmer now ✨", -420, reactions = listOf(MessageReaction("💜", 1, true))),
        ),
        "demo-chat-arjun" to mutableListOf(
            message("r1", "demo-chat-arjun", "me", "Football this Sunday?", -9000, MessageDeliveryState.Read),
            message("r2", "demo-chat-arjun", people[1].id, "Sunday works for me.", -7200),
        ),
        "demo-chat-zoya" to mutableListOf(
            message("z1", "demo-chat-zoya", people[2].id, "The new inbox needs a stronger visual rhythm.", -19000),
            message("z2", "demo-chat-zoya", "me", "Agreed. I am exploring labels, pinning and calmer density.", -18400, MessageDeliveryState.Read, bookmarked = true),
            message("z3", "demo-chat-zoya", people[2].id, "I saved the reference board here: https://example.com/lunara-board", -17200),
        ),
        "demo-chat-kabir" to mutableListOf(
            message("k1", "demo-chat-kabir", "me", "The sync flow is stable now.", -180000, MessageDeliveryState.Read),
            message("k2", "demo-chat-kabir", people[3].id, "Let us revisit this after the weekend.", -172800),
        ),
    )

    override suspend fun conversations(session: UserSession): Result<List<ChatConversation>> {
        delay(160)
        return Result.success(conversations.values.sortedWith(conversationOrder))
    }

    override suspend fun ensureConversation(session: UserSession, person: UserProfile): Result<ChatConversation> {
        delay(140)
        val existing = conversations.values.firstOrNull { it.person.id == person.id }
        if (existing != null) return Result.success(existing)
        val conversation = ChatConversation(UUID.randomUUID().toString(), person)
        conversations[conversation.id] = conversation
        messageMap[conversation.id] = mutableListOf()
        signals.tryEmit(ChatSignal.Refresh)
        return Result.success(conversation)
    }

    override suspend fun messages(
        session: UserSession,
        conversationId: String,
        before: String?,
        limit: Int,
    ): Result<List<ChatMessage>> {
        delay(150)
        val source = messageMap[conversationId].orEmpty()
        val filtered = if (before.isNullOrBlank()) source else source.filter { it.createdAt < before }
        return Result.success(filtered.takeLast(limit.coerceIn(1, 80)).map { normalizeMine(it, session.userId) })
    }

    override suspend fun searchMessages(
        session: UserSession,
        conversationId: String,
        query: String,
        limit: Int,
    ): Result<List<ChatMessage>> {
        delay(130)
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return Result.success(emptyList())
        val results = messageMap[conversationId].orEmpty()
            .asReversed()
            .filter { !it.isDeleted && it.body.lowercase().contains(needle) }
            .take(limit.coerceIn(1, 120))
            .reversed()
            .map { normalizeMine(it, session.userId) }
        return Result.success(results)
    }

    override suspend fun bookmarkedMessages(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> {
        delay(120)
        return Result.success(
            messageMap[conversationId].orEmpty()
                .asReversed()
                .filter(ChatMessage::isBookmarked)
                .take(limit.coerceIn(1, 120))
                .reversed()
                .map { normalizeMine(it, session.userId) }
        )
    }

    override suspend fun sharedLinks(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> {
        delay(120)
        return Result.success(
            messageMap[conversationId].orEmpty()
                .asReversed()
                .filter { !it.isDeleted && it.containsLink }
                .take(limit.coerceIn(1, 120))
                .reversed()
                .map { normalizeMine(it, session.userId) }
        )
    }

    override suspend fun mediaMessages(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> {
        delay(120)
        return Result.success(
            messageMap[conversationId].orEmpty()
                .asReversed()
                .filter { !it.isDeleted && it.attachment != null }
                .take(limit.coerceIn(1, 200))
                .reversed()
                .map { normalizeMine(it, session.userId) }
        )
    }

    override suspend fun sendMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        body: String,
        replyToId: String?,
    ): Result<ChatMessage> {
        delay(260)
        val messages = messageMap[conversationId] ?: return Result.failure(IllegalStateException("Conversation not found"))
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) return Result.failure(IllegalArgumentException("Message text cannot be empty"))
        if (cleanBody.length > 4000) return Result.failure(IllegalArgumentException("Messages can be up to 4000 characters"))
        messages.firstOrNull { it.clientId == clientId }?.let { return Result.success(normalizeMine(it, session.userId)) }
        val reply = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
        if (!replyToId.isNullOrBlank() && reply == null) {
            return Result.failure(IllegalArgumentException("The message you replied to is no longer available"))
        }
        val sent = ChatMessage(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            conversationId = conversationId,
            senderId = session.userId,
            body = cleanBody,
            createdAt = Instant.now().toString(),
            replyToId = replyToId.orEmpty(),
            replyPreview = reply?.body.orEmpty(),
            deliveryState = MessageDeliveryState.Sent,
        )
        messages += sent
        updateConversation(conversationId, sent.body, sent.createdAt)
        signals.tryEmit(ChatSignal.Refresh)
        return Result.success(sent)
    }

    override suspend fun sendMediaMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        attachment: MediaAttachment,
        replyToId: String?,
        onProgress: (Int) -> Unit,
    ): Result<ChatMessage> {
        val messages = messageMap[conversationId] ?: return Result.failure(IllegalStateException("Conversation not found"))
        val clean = attachment.validateForSend().getOrElse { return Result.failure(it) }
        messages.firstOrNull { it.clientId == clientId }?.let { return Result.success(normalizeMine(it, session.userId)) }
        val reply = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
        if (!replyToId.isNullOrBlank() && reply == null) {
            return Result.failure(IllegalArgumentException("The message you replied to is no longer available"))
        }
        listOf(8, 26, 48, 72, 100).forEach { progress ->
            delay(45)
            onProgress(progress)
        }
        val ready = clean.copy(transferState = MediaTransferState.Ready, progress = 100, errorMessage = "")
        val sent = ChatMessage(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            conversationId = conversationId,
            senderId = session.userId,
            body = ready.previewText(),
            createdAt = Instant.now().toString(),
            replyToId = replyToId.orEmpty(),
            replyPreview = reply?.previewText.orEmpty(),
            deliveryState = MessageDeliveryState.Sent,
            attachment = ready,
        )
        messages += sent
        updateConversation(conversationId, sent.previewText, sent.createdAt)
        signals.tryEmit(ChatSignal.Refresh)
        return Result.success(sent)
    }

    override suspend fun downloadMedia(
        session: UserSession,
        attachment: MediaAttachment,
        onProgress: (Int) -> Unit,
    ): Result<MediaAttachment> {
        if (attachment.localUri.isNotBlank()) return Result.success(attachment.copy(transferState = MediaTransferState.Ready, progress = 100))
        listOf(20, 50, 80, 100).forEach { progress -> delay(35); onProgress(progress) }
        return Result.failure(IllegalStateException("This preview file is not stored on this device"))
    }

    override suspend fun sendModuleMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        module: MessageModule,
        replyToId: String?,
    ): Result<ChatMessage> {
        delay(260)
        val messages = messageMap[conversationId] ?: return Result.failure(IllegalStateException("Conversation not found"))
        val cleanModule = module.validate().getOrElse { return Result.failure(it) }
        messages.firstOrNull { it.clientId == clientId }?.let { return Result.success(normalizeMine(it, session.userId)) }
        val reply = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
        if (!replyToId.isNullOrBlank() && reply == null) {
            return Result.failure(IllegalArgumentException("The message you replied to is no longer available"))
        }
        val sent = ChatMessage(
            id = UUID.randomUUID().toString(),
            clientId = clientId,
            conversationId = conversationId,
            senderId = session.userId,
            body = cleanModule.previewText(),
            createdAt = Instant.now().toString(),
            replyToId = replyToId.orEmpty(),
            replyPreview = reply?.previewText.orEmpty(),
            deliveryState = MessageDeliveryState.Sent,
            module = cleanModule,
        )
        messages += sent
        updateConversation(conversationId, sent.previewText, sent.createdAt)
        signals.tryEmit(ChatSignal.Refresh)
        return Result.success(sent)
    }

    override suspend fun updateModule(
        session: UserSession,
        messageId: String,
        expectedRevision: Int,
        module: MessageModule,
    ): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        check(!item.isDeleted) { "Removed messages cannot be updated" }
        check(item.module != null) { "This message is not interactive" }
        check(item.moduleRevision == expectedRevision) { "This card changed on another device. Refresh and try again." }
        val clean = module.validate().getOrThrow()
        list[index] = item.copy(
            body = clean.previewText(),
            module = clean,
            moduleRevision = item.moduleRevision + 1,
            editedAt = Instant.now().toString(),
        )
        refreshConversationPreview(item.conversationId)
    }

    override suspend fun editMessage(session: UserSession, messageId: String, body: String): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        check(item.senderId == session.userId || item.senderId == "me") { "You can only edit your own message" }
        check(!item.isDeleted) { "Removed messages cannot be changed" }
        check(item.module == null) { "Interactive cards are updated from their controls" }
        val cleanBody = body.trim()
        check(cleanBody.isNotBlank()) { "Message text cannot be empty" }
        check(cleanBody.length <= 4000) { "Messages can be up to 4000 characters" }
        list[index] = item.copy(body = cleanBody, editedAt = Instant.now().toString())
        refreshConversationPreview(item.conversationId)
    }

    override suspend fun deleteMessage(session: UserSession, messageId: String): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        check(item.senderId == session.userId || item.senderId == "me") { "You can only delete your own message" }
        check(!item.isDeleted) { "Message is already removed" }
        list[index] = item.copy(body = "", deletedAt = Instant.now().toString(), reactions = emptyList(), isBookmarked = false)
        refreshConversationPreview(item.conversationId)
    }

    override suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        check(!item.isDeleted) { "Removed messages cannot be reacted to" }
        val reactions = item.reactions.toMutableList()
        val mineIndex = reactions.indexOfFirst { it.reactedByMe }
        if (mineIndex >= 0) {
            val old = reactions[mineIndex]
            if (old.count <= 1) reactions.removeAt(mineIndex) else reactions[mineIndex] = old.copy(count = old.count - 1, reactedByMe = false)
        }
        if (!emoji.isNullOrBlank()) {
            val same = reactions.indexOfFirst { it.emoji == emoji }
            if (same >= 0) reactions[same] = reactions[same].copy(count = reactions[same].count + 1, reactedByMe = true)
            else reactions += MessageReaction(emoji, 1, true)
        }
        list[index] = item.copy(reactions = reactions)
    }

    override suspend fun setBookmark(session: UserSession, messageId: String, bookmarked: Boolean): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        check(!item.isDeleted) { "Removed messages cannot be saved" }
        list[index] = item.copy(isBookmarked = bookmarked)
    }

    override suspend fun updateConversationSettings(
        session: UserSession,
        conversationId: String,
        settings: ConversationSettings,
    ): Result<Unit> = mutate {
        val current = conversations[conversationId] ?: error("Conversation not found")
        conversations[conversationId] = current.copy(
            isPinned = settings.isPinned,
            isArchived = settings.isArchived,
            isMuted = settings.isMuted,
            labels = settings.labels.map(String::trim).filter(String::isNotBlank).distinct().take(4),
        )
    }

    override suspend fun markDelivered(session: UserSession, conversationId: String): Result<Unit> {
        delay(60)
        return Result.success(Unit)
    }

    override suspend fun markRead(session: UserSession, conversationId: String): Result<Unit> {
        delay(60)
        val current = conversations[conversationId] ?: return Result.success(Unit)
        if (current.unreadCount > 0) {
            conversations[conversationId] = current.copy(unreadCount = 0)
            signals.tryEmit(ChatSignal.Refresh)
        }
        return Result.success(Unit)
    }

    override suspend fun setTyping(session: UserSession, conversationId: String, typing: Boolean): Result<Unit> {
        conversations[conversationId]?.let { conversations[conversationId] = it.copy(isTyping = false) }
        return Result.success(Unit)
    }

    override suspend fun setPresence(session: UserSession, online: Boolean): Result<Unit> = Result.success(Unit)

    override fun observe(session: UserSession): Flow<ChatSignal> = signals

    private suspend fun mutate(block: () -> Unit): Result<Unit> {
        delay(140)
        return runCatching(block).onSuccess { signals.tryEmit(ChatSignal.Refresh) }
    }

    private fun find(messageId: String): Pair<MutableList<ChatMessage>, Int> {
        messageMap.values.forEach { list ->
            val index = list.indexOfFirst { it.id == messageId }
            if (index >= 0) return list to index
        }
        error("Message not found")
    }

    private fun updateConversation(id: String, body: String, at: String) {
        conversations[id]?.let {
            conversations[id] = it.copy(lastMessage = body, lastMessageAt = at, unreadCount = 0, isArchived = false)
        }
    }

    private fun refreshConversationPreview(id: String) {
        val latest = messageMap[id]?.maxByOrNull(ChatMessage::createdAt)
        val preview = when {
            latest == null -> ""
            latest.isDeleted -> "Message removed"
            else -> latest.previewText
        }
        conversations[id]?.let { current ->
            conversations[id] = current.copy(
                lastMessage = preview,
                lastMessageAt = latest?.createdAt.orEmpty(),
                unreadCount = 0,
            )
        }
    }

    private fun at(seconds: Long): String = demoNow.plusSeconds(seconds).toString()

    private fun normalizeMine(message: ChatMessage, userId: String): ChatMessage {
        val normalizedModule = message.module?.copy(
            options = message.module.options.map { option ->
                option.copy(voterIds = option.voterIds.map { if (it == "me") userId else it })
            },
            rsvps = message.module.rsvps.mapKeys { (key, _) -> if (key == "me") userId else key },
        )
        return message.copy(
            senderId = if (message.senderId == "me") userId else message.senderId,
            module = normalizedModule,
        )
    }

    private fun message(
        id: String,
        conversationId: String,
        senderId: String,
        body: String,
        seconds: Long,
        state: MessageDeliveryState = MessageDeliveryState.Delivered,
        reactions: List<MessageReaction> = emptyList(),
        bookmarked: Boolean = false,
        module: MessageModule? = null,
    ) = ChatMessage(
        id = id,
        clientId = id,
        conversationId = conversationId,
        senderId = senderId,
        body = body,
        createdAt = at(seconds),
        deliveryState = state,
        reactions = reactions,
        isBookmarked = bookmarked,
        module = module,
    )

    private fun moduleMessage(
        id: String,
        conversationId: String,
        senderId: String,
        seconds: Long,
        module: MessageModule,
        state: MessageDeliveryState = MessageDeliveryState.Delivered,
    ) = message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        body = module.previewText(),
        seconds = seconds,
        state = state,
        module = module,
    )

    private companion object {
        val conversationOrder = compareByDescending<ChatConversation> { it.isPinned }
            .thenByDescending(ChatConversation::lastMessageAt)
    }
}
