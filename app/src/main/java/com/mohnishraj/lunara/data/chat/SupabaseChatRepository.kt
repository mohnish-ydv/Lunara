package com.mohnishraj.lunara.data.chat

import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.data.media.MediaAttachmentStore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SupabaseChatRepository(
    private val mediaStore: MediaAttachmentStore,
) : ChatRepository {
    private val socketClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun conversations(session: UserSession): Result<List<ChatConversation>> = io {
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/list_my_conversations",
            token = session.accessToken,
            body = "{}",
        )
        response.requireSuccess()
        JSONArray(response.body).toConversations()
    }

    override suspend fun ensureConversation(
        session: UserSession,
        person: UserProfile,
    ): Result<ChatConversation> = io {
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/ensure_direct_conversation",
            token = session.accessToken,
            body = JSONObject().put("other_user", person.id).toString(),
        )
        response.requireSuccess()
        val id = response.body.trim().trim('"')
        if (id.isBlank()) error("Could not open this conversation")
        val listResponse = request(
            method = "POST",
            path = "/rest/v1/rpc/list_my_conversations",
            token = session.accessToken,
            body = "{}",
        )
        listResponse.requireSuccess()
        JSONArray(listResponse.body).toConversations().firstOrNull { it.id == id }
            ?: ChatConversation(id = id, person = person)
    }

    override suspend fun messages(
        session: UserSession,
        conversationId: String,
        before: String?,
        limit: Int,
    ): Result<List<ChatMessage>> = io {
        val body = JSONObject()
            .put("target_conversation", conversationId)
            .put("before_time", before ?: JSONObject.NULL)
            .put("page_size", limit.coerceIn(1, 80))
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/get_conversation_messages",
            token = session.accessToken,
            body = body.toString(),
        )
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun searchMessages(
        session: UserSession,
        conversationId: String,
        query: String,
        limit: Int,
    ): Result<List<ChatMessage>> = io {
        val clean = query.trim()
        if (clean.isBlank()) return@io emptyList()
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/search_conversation_messages",
            token = session.accessToken,
            body = JSONObject()
                .put("target_conversation", conversationId)
                .put("search_query", clean)
                .put("page_size", limit.coerceIn(1, 120))
                .toString(),
        )
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun bookmarkedMessages(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> = io {
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/list_conversation_bookmarks",
            token = session.accessToken,
            body = JSONObject()
                .put("target_conversation", conversationId)
                .put("page_size", limit.coerceIn(1, 120))
                .toString(),
        )
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun sharedLinks(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> = io {
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/list_conversation_links",
            token = session.accessToken,
            body = JSONObject()
                .put("target_conversation", conversationId)
                .put("page_size", limit.coerceIn(1, 120))
                .toString(),
        )
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun mediaMessages(
        session: UserSession,
        conversationId: String,
        limit: Int,
    ): Result<List<ChatMessage>> = io {
        val response = request(
            method = "POST",
            path = "/rest/v1/rpc/list_conversation_media",
            token = session.accessToken,
            body = JSONObject()
                .put("target_conversation", conversationId)
                .put("page_size", limit.coerceIn(1, 200))
                .toString(),
        )
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun sendMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        body: String,
        replyToId: String?,
    ): Result<ChatMessage> = io {
        val payload = JSONObject()
            .put("conversation_id", conversationId)
            .put("sender_id", session.userId)
            .put("client_id", clientId)
            .put("body", body.trim())
            .put("reply_to_id", replyToId ?: JSONObject.NULL)
        val response = request(
            method = "POST",
            path = "/rest/v1/messages?on_conflict=sender_id,client_id",
            token = session.accessToken,
            body = payload.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation"),
        )
        response.requireSuccess()
        val row = JSONArray(response.body).optJSONObject(0) ?: error("Message was not returned")
        row.toMessage().copy(deliveryState = MessageDeliveryState.Sent)
    }

    override suspend fun sendMediaMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        attachment: MediaAttachment,
        replyToId: String?,
        onProgress: (Int) -> Unit,
    ): Result<ChatMessage> = io {
        val clean = attachment.validateForSend().getOrThrow()
        val file = mediaStore.fileFor(clean) ?: error("The selected file is no longer available")
        val safeName = clean.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(160).ifBlank { "media" }
        val objectPath = "$conversationId/${session.userId}/$clientId/$safeName"
        uploadMedia(session, objectPath, file, clean.mimeType, onProgress)
        val remote = clean.copy(
            remotePath = objectPath,
            transferState = MediaTransferState.Ready,
            progress = 100,
            errorMessage = "",
        )
        try {
            val payload = JSONObject()
                .put("conversation_id", conversationId)
                .put("sender_id", session.userId)
                .put("client_id", clientId)
                .put("body", remote.previewText())
                .put("reply_to_id", replyToId ?: JSONObject.NULL)
                .put("media_type", remote.kind.wireName)
                .put("media_payload", remote.toJson())
            val response = request(
                method = "POST",
                path = "/rest/v1/messages?on_conflict=sender_id,client_id",
                token = session.accessToken,
                body = payload.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation"),
            )
            response.requireSuccess()
            val row = JSONArray(response.body).optJSONObject(0) ?: error("Media message was not returned")
            val message = row.toMessage()
            message.copy(
                deliveryState = MessageDeliveryState.Sent,
                attachment = message.attachment?.copy(localUri = clean.localUri) ?: remote,
            )
        } catch (error: Throwable) {
            deleteUploadedMedia(session, objectPath)
            throw error
        }
    }

    override suspend fun downloadMedia(
        session: UserSession,
        attachment: MediaAttachment,
        onProgress: (Int) -> Unit,
    ): Result<MediaAttachment> = withContext(Dispatchers.IO) {
        runCatching {
            if (attachment.localUri.isNotBlank() && mediaStore.fileFor(attachment) != null) {
                return@runCatching attachment.copy(transferState = MediaTransferState.Ready, progress = 100)
            }
            require(attachment.remotePath.isNotBlank()) { "This file is not available online" }
            val url = AppConfig.supabaseUrl + "/storage/v1/object/authenticated/chat-media/" + encodePath(attachment.remotePath)
            val request = Request.Builder()
                .url(url)
                .header("apikey", AppConfig.supabaseAnonKey)
                .header("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            socketClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Could not download this file")
                val body = response.body ?: error("Downloaded file is empty")
                val total = body.contentLength().takeIf { it > 0L }
                val downloaded = body.byteStream().use { input ->
                    mediaStore.saveDownload(attachment, input) { copied ->
                        total?.let { onProgress(((copied * 100L) / it).toInt().coerceIn(0, 99)) }
                    }.getOrThrow()
                }
                onProgress(100)
                downloaded
            }
        }
    }

    override suspend fun sendModuleMessage(
        session: UserSession,
        conversationId: String,
        clientId: String,
        module: MessageModule,
        replyToId: String?,
    ): Result<ChatMessage> = io {
        val clean = module.validate().getOrThrow()
        val payload = JSONObject()
            .put("conversation_id", conversationId)
            .put("sender_id", session.userId)
            .put("client_id", clientId)
            .put("body", clean.previewText())
            .put("reply_to_id", replyToId ?: JSONObject.NULL)
            .put("module_type", clean.type.wireName)
            .put("module_payload", clean.toJson())
        val response = request(
            method = "POST",
            path = "/rest/v1/messages?on_conflict=sender_id,client_id",
            token = session.accessToken,
            body = payload.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation"),
        )
        response.requireSuccess()
        val row = JSONArray(response.body).optJSONObject(0) ?: error("Card was not returned")
        row.toMessage().copy(deliveryState = MessageDeliveryState.Sent)
    }

    override suspend fun updateModule(
        session: UserSession,
        messageId: String,
        expectedRevision: Int,
        module: MessageModule,
    ): Result<Unit> = write {
        val clean = module.validate().getOrThrow()
        request(
            method = "POST",
            path = "/rest/v1/rpc/update_message_module",
            token = session.accessToken,
            body = JSONObject()
                .put("target_message", messageId)
                .put("expected_revision", expectedRevision)
                .put("next_payload", clean.toJson())
                .toString(),
        ).requireSuccess()
    }

    override suspend fun editMessage(session: UserSession, messageId: String, body: String): Result<Unit> = write {
        val response = request(
            method = "PATCH",
            path = "/rest/v1/messages?${queryString(listOf("id" to "eq.$messageId", "sender_id" to "eq.${session.userId}"))}",
            token = session.accessToken,
            body = JSONObject().put("body", body.trim()).put("edited_at", Instant.now().toString()).toString(),
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        response.requireSuccess()
        if (JSONArray(response.body).length() == 0) error("Message is no longer available")
    }

    override suspend fun deleteMessage(session: UserSession, messageId: String): Result<Unit> = write {
        val response = request(
            method = "PATCH",
            path = "/rest/v1/messages?${queryString(listOf("id" to "eq.$messageId", "sender_id" to "eq.${session.userId}"))}",
            token = session.accessToken,
            body = JSONObject()
                .put("body", "")
                .put("deleted_at", Instant.now().toString())
                .toString(),
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        response.requireSuccess()
        if (JSONArray(response.body).length() == 0) error("Message is no longer available")
    }

    override suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit> = write {
        val delete = request(
            method = "DELETE",
            path = "/rest/v1/message_reactions?${queryString(listOf("message_id" to "eq.$messageId", "user_id" to "eq.${session.userId}"))}",
            token = session.accessToken,
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        )
        delete.requireSuccess()
        if (!emoji.isNullOrBlank()) {
            request(
                method = "POST",
                path = "/rest/v1/message_reactions",
                token = session.accessToken,
                body = JSONObject()
                    .put("message_id", messageId)
                    .put("user_id", session.userId)
                    .put("emoji", emoji.take(8))
                    .toString(),
                extraHeaders = mapOf("Prefer" to "return=minimal"),
            ).requireSuccess()
        }
    }

    override suspend fun setBookmark(
        session: UserSession,
        messageId: String,
        bookmarked: Boolean,
    ): Result<Unit> = write {
        if (bookmarked) {
            request(
                method = "POST",
                path = "/rest/v1/message_bookmarks?on_conflict=user_id,message_id",
                token = session.accessToken,
                body = JSONObject()
                    .put("user_id", session.userId)
                    .put("message_id", messageId)
                    .toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
            ).requireSuccess()
        } else {
            request(
                method = "DELETE",
                path = "/rest/v1/message_bookmarks?${queryString(listOf("user_id" to "eq.${session.userId}", "message_id" to "eq.$messageId"))}",
                token = session.accessToken,
                extraHeaders = mapOf("Prefer" to "return=minimal"),
            ).requireSuccess()
        }
    }

    override suspend fun updateConversationSettings(
        session: UserSession,
        conversationId: String,
        settings: ConversationSettings,
    ): Result<Unit> = write {
        val labels = JSONArray()
        settings.labels.map(String::trim).filter(String::isNotBlank).distinct().take(4).forEach(labels::put)
        request(
            method = "POST",
            path = "/rest/v1/conversation_preferences?on_conflict=user_id,conversation_id",
            token = session.accessToken,
            body = JSONObject()
                .put("user_id", session.userId)
                .put("conversation_id", conversationId)
                .put("is_pinned", settings.isPinned)
                .put("is_archived", settings.isArchived)
                .put("is_muted", settings.isMuted)
                .put("labels", labels)
                .toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        ).requireSuccess()
    }

    override suspend fun markDelivered(session: UserSession, conversationId: String): Result<Unit> = rpcUnit(
        session = session,
        function = "mark_conversation_delivered",
        body = JSONObject().put("target_conversation", conversationId),
    )

    override suspend fun markRead(session: UserSession, conversationId: String): Result<Unit> = rpcUnit(
        session = session,
        function = "mark_conversation_read",
        body = JSONObject().put("target_conversation", conversationId),
    )

    override suspend fun setTyping(
        session: UserSession,
        conversationId: String,
        typing: Boolean,
    ): Result<Unit> = rpcUnit(
        session = session,
        function = "set_conversation_typing",
        body = JSONObject().put("target_conversation", conversationId).put("typing", typing),
    )

    override suspend fun setPresence(session: UserSession, online: Boolean): Result<Unit> = rpcUnit(
        session = session,
        function = "set_my_presence",
        body = JSONObject().put("online", online),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(session: UserSession): Flow<ChatSignal> = callbackFlow {
        val socketUrl = AppConfig.supabaseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "/realtime/v1/websocket?apikey=${encode(AppConfig.supabaseAnonKey)}&vsn=1.0.0"
        val topic = "realtime:lunara-${session.userId}"
        val reference = AtomicInteger(1)
        val disposed = AtomicBoolean(false)
        val connecting = AtomicBoolean(false)
        var socket: WebSocket? = null
        var heartbeat: Job? = null
        var reconnect: Job? = null
        lateinit var connect: () -> Unit

        fun scheduleReconnect() {
            if (disposed.get() || reconnect?.isActive == true) return
            reconnect = launch {
                delay(2_000)
                if (isActive && !disposed.get()) connect()
            }
        }

        connect = connect@ {
            if (disposed.get() || !connecting.compareAndSet(false, true)) return@connect
            heartbeat?.cancel()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connecting.set(false)
                    val changes = JSONArray()
                    listOf("messages", "message_reactions", "message_receipts", "message_bookmarks", "conversation_preferences", "typing_states", "presence_states", "conversations")
                        .forEach { table ->
                            changes.put(
                                JSONObject()
                                    .put("event", "*")
                                    .put("schema", "public")
                                    .put("table", table)
                            )
                        }
                    val config = JSONObject()
                        .put("broadcast", JSONObject().put("ack", false).put("self", false))
                        .put("presence", JSONObject().put("key", session.userId))
                        .put("postgres_changes", changes)
                    val payload = JSONObject()
                        .put("config", config)
                        .put("access_token", session.accessToken)
                    webSocket.send(envelope(topic, "phx_join", payload, reference.getAndIncrement().toString()))
                    heartbeat?.cancel()
                    heartbeat = launch(Dispatchers.IO) {
                        while (isActive && !disposed.get()) {
                            delay(25_000)
                            webSocket.send(envelope("phoenix", "heartbeat", JSONObject(), reference.getAndIncrement().toString()))
                        }
                    }
                    trySend(ChatSignal.Reconnected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = runCatching { JSONObject(text).optString("event") }.getOrDefault("")
                    if (event == "postgres_changes" || event == "broadcast" || event == "presence_diff") {
                        trySend(ChatSignal.Refresh)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    connecting.set(false)
                    heartbeat?.cancel()
                    if (!disposed.get()) scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
                    connecting.set(false)
                    heartbeat?.cancel()
                    trySend(ChatSignal.Refresh)
                    scheduleReconnect()
                }
            }

            val request = Request.Builder()
                .url(socketUrl)
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .build()
            socket = socketClient.newWebSocket(request, listener)
        }

        connect()
        awaitClose {
            disposed.set(true)
            reconnect?.cancel()
            heartbeat?.cancel()
            socket?.close(1000, "Screen closed")
        }
    }

    private fun envelope(topic: String, event: String, payload: JSONObject, ref: String): String =
        JSONObject()
            .put("topic", topic)
            .put("event", event)
            .put("payload", payload)
            .put("ref", ref)
            .toString()

    private suspend fun rpcUnit(
        session: UserSession,
        function: String,
        body: JSONObject,
    ): Result<Unit> = write {
        request(
            method = "POST",
            path = "/rest/v1/rpc/$function",
            token = session.accessToken,
            body = body.toString(),
        ).requireSuccess()
    }

    private suspend fun <T> io(action: () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching(action) }

    private suspend fun write(action: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) { runCatching(action) }

    private fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = URI(AppConfig.supabaseUrl + path).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", AppConfig.supabaseAnonKey)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            extraHeaders.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input ->
                BufferedReader(InputStreamReader(input)).use { reader -> reader.readText() }
            }.orEmpty()
            HttpResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }

    private fun queryString(parameters: List<Pair<String, String>>): String = parameters.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun JSONArray.toConversations(): List<ChatConversation> = buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            add(
                ChatConversation(
                    id = json.optString("conversation_id"),
                    person = json.toPerson(),
                    lastMessage = json.optString("last_message"),
                    lastMessageAt = json.optString("last_message_at"),
                    unreadCount = json.optInt("unread_count"),
                    isOnline = json.optBoolean("is_online"),
                    lastActiveAt = json.optString("last_active_at"),
                    isTyping = json.optBoolean("is_typing"),
                    isPinned = json.optBoolean("is_pinned"),
                    isArchived = json.optBoolean("is_archived"),
                    isMuted = json.optBoolean("is_muted"),
                    labels = json.optJSONArray("labels")?.toStringList().orEmpty(),
                )
            )
        }
    }

    private fun JSONArray.toMessages(): List<ChatMessage> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { add(it.toMessage()) }
    }

    private fun JSONObject.toMessage(): ChatMessage {
        val reactionArray = optJSONArray("reactions") ?: JSONArray()
        val reactions = buildList {
            for (index in 0 until reactionArray.length()) {
                val item = reactionArray.optJSONObject(index) ?: continue
                add(
                    MessageReaction(
                        emoji = item.optString("emoji"),
                        count = item.optInt("count", 1),
                        reactedByMe = item.optBoolean("reacted_by_me"),
                    )
                )
            }
        }
        return ChatMessage(
            id = optString("id"),
            clientId = optString("client_id", optString("id")),
            conversationId = optString("conversation_id"),
            senderId = optString("sender_id"),
            body = optString("body"),
            createdAt = optString("created_at"),
            editedAt = optString("edited_at"),
            deletedAt = optString("deleted_at"),
            replyToId = optString("reply_to_id"),
            replyPreview = optString("reply_preview"),
            deliveryState = runCatching {
                MessageDeliveryState.valueOf(optString("delivery_state", "Sent"))
            }.getOrDefault(MessageDeliveryState.Sent),
            reactions = reactions,
            isBookmarked = optBoolean("is_bookmarked"),
            module = MessageModuleType.fromWire(optString("module_type"))?.let { type ->
                optJSONObject("module_payload")?.toModule(type)
            },
            moduleRevision = optInt("module_revision"),
            attachment = MediaKind.fromWire(optString("media_type"))?.let { kind ->
                optJSONObject("media_payload")?.toMedia(kind)?.let(mediaStore::hydrateLocal)
            },
        )
    }

    private fun MessageModule.toJson(): JSONObject {
        val checklist = JSONArray().also { array ->
            items.forEach { item ->
                array.put(JSONObject().put("id", item.id).put("text", item.text).put("completed", item.completed))
            }
        }
        val pollOptions = JSONArray().also { array ->
            options.forEach { option ->
                array.put(
                    JSONObject()
                        .put("id", option.id)
                        .put("text", option.text)
                        .put("voter_ids", JSONArray(option.voterIds))
                )
            }
        }
        val rsvpObject = JSONObject().also { json -> rsvps.forEach { (key, value) -> json.put(key, value) } }
        return JSONObject()
            .put("title", title)
            .put("description", description)
            .put("completed", completed)
            .put("due_at", dueAt)
            .put("items", checklist)
            .put("options", pollOptions)
            .put("event_at", eventAt)
            .put("location_name", locationName)
            .put("latitude", latitude ?: JSONObject.NULL)
            .put("longitude", longitude ?: JSONObject.NULL)
            .put("rsvps", rsvpObject)
            .put("code", code)
            .put("language", language)
            .put("contact_name", contactName)
            .put("contact_value", contactValue)
    }

    private fun JSONObject.toModule(type: MessageModuleType): MessageModule {
        val checklist = optJSONArray("items") ?: JSONArray()
        val optionsJson = optJSONArray("options") ?: JSONArray()
        val rsvpJson = optJSONObject("rsvps") ?: JSONObject()
        return MessageModule(
            type = type,
            title = optString("title"),
            description = optString("description"),
            completed = optBoolean("completed"),
            dueAt = optString("due_at"),
            items = buildList {
                for (index in 0 until checklist.length()) {
                    checklist.optJSONObject(index)?.let { item ->
                        add(ModuleChecklistItem(item.optString("id"), item.optString("text"), item.optBoolean("completed")))
                    }
                }
            },
            options = buildList {
                for (index in 0 until optionsJson.length()) {
                    optionsJson.optJSONObject(index)?.let { option ->
                        add(
                            ModulePollOption(
                                id = option.optString("id"),
                                text = option.optString("text"),
                                voterIds = option.optJSONArray("voter_ids")?.toStringList().orEmpty(),
                            )
                        )
                    }
                }
            },
            eventAt = optString("event_at"),
            locationName = optString("location_name"),
            latitude = if (isNull("latitude")) null else optDouble("latitude"),
            longitude = if (isNull("longitude")) null else optDouble("longitude"),
            rsvps = buildMap {
                val keys = rsvpJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    rsvpJson.optString(key).takeIf(String::isNotBlank)?.let { put(key, it) }
                }
            },
            code = optString("code"),
            language = optString("language"),
            contactName = optString("contact_name"),
            contactValue = optString("contact_value"),
        )
    }

    private fun MediaAttachment.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("file_name", fileName)
        .put("mime_type", mimeType)
        .put("size_bytes", sizeBytes)
        .put("caption", caption)
        .put("remote_path", remotePath)
        .put("width", width)
        .put("height", height)
        .put("duration_ms", durationMs)
        .put("waveform", JSONArray(waveform))

    private fun JSONObject.toMedia(kind: MediaKind): MediaAttachment = MediaAttachment(
        id = optString("id"),
        kind = kind,
        fileName = optString("file_name"),
        mimeType = optString("mime_type"),
        sizeBytes = optLong("size_bytes"),
        caption = optString("caption"),
        remotePath = optString("remote_path"),
        width = optInt("width"),
        height = optInt("height"),
        durationMs = optLong("duration_ms"),
        waveform = optJSONArray("waveform")?.let { array ->
            buildList { for (index in 0 until array.length()) add(array.optInt(index).coerceIn(0, 100)) }
        }.orEmpty(),
        transferState = MediaTransferState.Ready,
        progress = 100,
    )

    private fun uploadMedia(
        session: UserSession,
        objectPath: String,
        file: File,
        mimeType: String,
        onProgress: (Int) -> Unit,
    ) {
        val url = AppConfig.supabaseUrl + "/storage/v1/object/chat-media/" + encodePath(objectPath)
        val request = Request.Builder()
            .url(url)
            .header("apikey", AppConfig.supabaseAnonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("x-upsert", "true")
            .post(ProgressFileBody(file, mimeType, onProgress))
            .build()
        socketClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body?.string().orEmpty()
                error(runCatching { JSONObject(detail).optString("message") }.getOrNull().orEmpty().ifBlank { "Could not upload this file" })
            }
        }
        onProgress(100)
    }

    private fun deleteUploadedMedia(session: UserSession, objectPath: String) {
        runCatching {
            val request = Request.Builder()
                .url(AppConfig.supabaseUrl + "/storage/v1/object/chat-media/" + encodePath(objectPath))
                .header("apikey", AppConfig.supabaseAnonKey)
                .header("Authorization", "Bearer ${session.accessToken}")
                .delete()
                .build()
            socketClient.newCall(request).execute().close()
        }
    }

    private fun encodePath(path: String): String = path.split('/').joinToString("/") { segment ->
        encode(segment).replace("+", "%20")
    }

    private class ProgressFileBody(
        private val file: File,
        private val mimeType: String,
        private val onProgress: (Int) -> Unit,
    ) : RequestBody() {
        override fun contentType() = mimeType.toMediaTypeOrNull()
        override fun contentLength(): Long = file.length()
        override fun writeTo(sink: BufferedSink) {
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                val total = contentLength().coerceAtLeast(1L)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    copied += read
                    onProgress(((copied * 100L) / total).toInt().coerceIn(0, 99))
                }
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun JSONObject.toPerson(): UserProfile = UserProfile(
        id = optString("person_id"),
        email = "",
        username = optString("person_username"),
        displayName = optString("person_display_name"),
        bio = optString("person_bio"),
        avatarSeed = optInt("person_avatar_seed"),
        shareCode = optString("person_share_code"),
    )

    private data class HttpResponse(val code: Int, val body: String) {
        fun requireSuccess() {
            if (code !in 200..299) error(errorMessage())
        }

        private fun errorMessage(): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            return json?.optString("message")?.takeIf(String::isNotBlank)
                ?: json?.optString("msg")?.takeIf(String::isNotBlank)
                ?: json?.optString("details")?.takeIf(String::isNotBlank)
                ?: when (code) {
                    401 -> "Your session has expired. Sign in again."
                    403 -> "This conversation is no longer available."
                    409 -> "That message was already sent."
                    else -> "Could not sync the conversation. Try again."
                }
        }
    }
}
