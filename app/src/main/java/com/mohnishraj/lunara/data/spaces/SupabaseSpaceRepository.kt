package com.mohnishraj.lunara.data.spaces

import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.domain.MessageReaction
import com.mohnishraj.lunara.domain.SpaceChannel
import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceDetail
import com.mohnishraj.lunara.domain.SpaceMember
import com.mohnishraj.lunara.domain.SpaceMessage
import com.mohnishraj.lunara.domain.SpaceRole
import com.mohnishraj.lunara.domain.SpaceSignal
import com.mohnishraj.lunara.domain.SpaceSummary
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import com.mohnishraj.lunara.domain.validateChannelName
import com.mohnishraj.lunara.domain.validateSpaceMessage
import com.mohnishraj.lunara.domain.validateSpaceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SupabaseSpaceRepository : SpaceRepository {
    private val socketClient = OkHttpClient.Builder().retryOnConnectionFailure(true).build()

    override suspend fun spaces(session: UserSession): Result<List<SpaceSummary>> = io {
        val response = rpc(session, "list_my_spaces", JSONObject())
        response.requireSuccess()
        JSONArray(response.body).toSpaces()
    }

    override suspend fun detail(session: UserSession, spaceId: String): Result<SpaceDetail> = io {
        val response = rpc(session, "get_space_detail", JSONObject().put("target_space", spaceId))
        response.requireSuccess()
        val root = response.body.toJsonObject()
        SpaceDetail(
            space = root.getJSONObject("space").toSpace(),
            channels = root.optJSONArray("channels")?.toChannels().orEmpty(),
            members = root.optJSONArray("members")?.toMembers().orEmpty(),
        )
    }

    override suspend fun messages(
        session: UserSession,
        channelId: String,
        before: String?,
        limit: Int,
    ): Result<List<SpaceMessage>> = io {
        val body = JSONObject()
            .put("target_channel", channelId)
            .put("before_at", before ?: JSONObject.NULL)
            .put("page_size", limit.coerceIn(1, 100))
        val response = rpc(session, "get_space_messages", body)
        response.requireSuccess()
        JSONArray(response.body).toMessages()
    }

    override suspend fun createSpace(
        session: UserSession,
        name: String,
        description: String,
        emoji: String,
        memberIds: List<String>,
    ): Result<SpaceDetail> = io {
        val response = rpc(
            session,
            "create_space",
            JSONObject()
                .put("space_name", validateSpaceName(name).getOrThrow())
                .put("space_description", description.trim().take(240))
                .put("space_emoji", emoji.trim().takeIf(String::isNotBlank)?.take(4) ?: "✨")
                .put("initial_members", JSONArray(memberIds.distinct().take(24))),
        )
        response.requireSuccess()
        val spaceId = response.body.scalarString()
        detail(session, spaceId).getOrThrow()
    }

    override suspend fun createChannel(
        session: UserSession,
        spaceId: String,
        name: String,
        description: String,
        kind: SpaceChannelKind,
    ): Result<SpaceChannel> = io {
        val response = rpc(
            session,
            "create_space_channel",
            JSONObject()
                .put("target_space", spaceId)
                .put("channel_name", validateChannelName(name).getOrThrow())
                .put("channel_description", description.trim().take(160))
                .put("channel_kind", kind.name.lowercase()),
        )
        response.requireSuccess()
        response.body.toJsonObject().toChannel()
    }

    override suspend fun sendMessage(
        session: UserSession,
        channelId: String,
        clientId: String,
        body: String,
        replyToId: String?,
    ): Result<SpaceMessage> = io {
        val response = rpc(
            session,
            "send_space_message",
            JSONObject()
                .put("target_channel", channelId)
                .put("client_message_id", clientId)
                .put("message_body", validateSpaceMessage(body).getOrThrow())
                .put("reply_to_message", replyToId ?: JSONObject.NULL),
        )
        response.requireSuccess()
        response.body.toJsonObject().toMessage()
    }

    override suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit> = write {
        rpc(
            session,
            "react_space_message",
            JSONObject().put("target_message", messageId).put("reaction_emoji", emoji ?: JSONObject.NULL),
        ).requireSuccess()
    }

    override suspend fun setPreferences(
        session: UserSession,
        spaceId: String,
        favorite: Boolean,
        muted: Boolean,
    ): Result<Unit> = write {
        rpc(
            session,
            "set_space_preferences",
            JSONObject().put("target_space", spaceId).put("favorite", favorite).put("muted", muted),
        ).requireSuccess()
    }

    override suspend fun markChannelRead(session: UserSession, channelId: String): Result<Unit> = write {
        rpc(session, "mark_space_channel_read", JSONObject().put("target_channel", channelId)).requireSuccess()
    }

    override suspend fun leaveSpace(session: UserSession, spaceId: String): Result<Unit> = write {
        rpc(session, "leave_space", JSONObject().put("target_space", spaceId)).requireSuccess()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(session: UserSession): Flow<SpaceSignal> = callbackFlow {
        val socketUrl = AppConfig.supabaseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "/realtime/v1/websocket?apikey=${encode(AppConfig.supabaseAnonKey)}&vsn=1.0.0"
        val topic = "realtime:lunara-spaces-${session.userId}"
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
                    listOf("spaces", "space_members", "space_channels", "space_messages", "space_message_reactions", "space_channel_reads", "space_preferences")
                        .forEach { table ->
                            changes.put(JSONObject().put("event", "*").put("schema", "public").put("table", table))
                        }
                    val config = JSONObject()
                        .put("broadcast", JSONObject().put("ack", false).put("self", false))
                        .put("presence", JSONObject().put("key", session.userId))
                        .put("postgres_changes", changes)
                    val payload = JSONObject().put("config", config).put("access_token", session.accessToken)
                    webSocket.send(envelope(topic, "phx_join", payload, reference.getAndIncrement().toString()))
                    heartbeat?.cancel()
                    heartbeat = launch(Dispatchers.IO) {
                        while (isActive && !disposed.get()) {
                            delay(25_000)
                            webSocket.send(envelope("phoenix", "heartbeat", JSONObject(), reference.getAndIncrement().toString()))
                        }
                    }
                    trySend(SpaceSignal.Reconnected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = runCatching { JSONObject(text).optString("event") }.getOrDefault("")
                    if (event == "postgres_changes" || event == "broadcast" || event == "presence_diff") {
                        trySend(SpaceSignal.Refresh)
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
                    trySend(SpaceSignal.Refresh)
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

    private fun rpc(session: UserSession, function: String, body: JSONObject): HttpResponse = request(
        method = "POST",
        path = "/rest/v1/rpc/$function",
        token = session.accessToken,
        body = body.toString(),
    )

    private suspend fun <T> io(action: suspend () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching { action() } }
    private suspend fun write(action: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) { runCatching(action) }

    private fun request(method: String, path: String, token: String, body: String? = null): HttpResponse {
        val connection = URI.create(AppConfig.supabaseUrl + path).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", AppConfig.supabaseAnonKey)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input -> BufferedReader(InputStreamReader(input)).use(BufferedReader::readText) }.orEmpty()
            HttpResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.toSpaces(): List<SpaceSummary> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { add(it.toSpace()) }
    }

    private fun JSONArray.toChannels(): List<SpaceChannel> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { add(it.toChannel()) }
    }

    private fun JSONArray.toMembers(): List<SpaceMember> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { json ->
            add(
                SpaceMember(
                    profile = json.optJSONObject("profile")?.toProfile() ?: json.toProfile(),
                    role = json.optString("role").toSpaceRole(),
                    joinedAt = json.optString("joined_at"),
                    isOnline = json.optBoolean("is_online"),
                )
            )
        }
    }

    private fun JSONArray.toMessages(): List<SpaceMessage> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let { add(it.toMessage()) }
    }

    private fun JSONObject.toSpace(): SpaceSummary = SpaceSummary(
        id = optString("id", optString("space_id")),
        name = optString("name", optString("space_name")),
        description = optString("description"),
        emoji = optString("emoji", "✨"),
        accentSeed = optInt("accent_seed"),
        memberCount = optInt("member_count", 1),
        channelCount = optInt("channel_count", 1),
        unreadCount = optInt("unread_count"),
        lastActivityAt = optString("last_activity_at"),
        myRole = optString("my_role", optString("role", "member")).toSpaceRole(),
        isFavorite = optBoolean("is_favorite"),
        isMuted = optBoolean("is_muted"),
        inviteCode = optString("invite_code"),
    )

    private fun JSONObject.toChannel(): SpaceChannel = SpaceChannel(
        id = optString("id", optString("channel_id")),
        spaceId = optString("space_id"),
        name = optString("name", optString("channel_name")),
        description = optString("description"),
        kind = runCatching { SpaceChannelKind.valueOf(optString("kind", "chat").replaceFirstChar(Char::uppercase)) }.getOrDefault(SpaceChannelKind.Chat),
        unreadCount = optInt("unread_count"),
        lastMessage = optString("last_message"),
        lastMessageAt = optString("last_message_at"),
        isPinned = optBoolean("is_pinned"),
    )

    private fun JSONObject.toMessage(): SpaceMessage {
        val reactionArray = optJSONArray("reactions") ?: JSONArray()
        val reactions = buildList {
            for (index in 0 until reactionArray.length()) {
                reactionArray.optJSONObject(index)?.let { item ->
                    add(MessageReaction(item.optString("emoji"), item.optInt("count", 1), item.optBoolean("reacted_by_me")))
                }
            }
        }
        return SpaceMessage(
            id = optString("id"),
            clientId = optString("client_id", optString("id")),
            channelId = optString("channel_id"),
            sender = optJSONObject("sender")?.toProfile() ?: JSONObject()
                .put("id", optString("sender_id"))
                .put("username", optString("sender_username"))
                .put("display_name", optString("sender_display_name"))
                .put("avatar_seed", optInt("sender_avatar_seed"))
                .toProfile(),
            body = optString("body"),
            createdAt = optString("created_at"),
            editedAt = optString("edited_at"),
            replyToId = optString("reply_to_id"),
            replyPreview = optString("reply_preview"),
            reactions = reactions,
            isAnnouncement = optBoolean("is_announcement"),
        )
    }

    private fun JSONObject.toProfile(): UserProfile = UserProfile(
        id = optString("id"),
        email = optString("email"),
        username = optString("username"),
        displayName = optString("display_name"),
        bio = optString("bio"),
        avatarSeed = optInt("avatar_seed"),
        shareCode = optString("share_code"),
    )

    private fun String.toSpaceRole(): SpaceRole = runCatching { SpaceRole.valueOf(lowercase().replaceFirstChar(Char::uppercase)) }.getOrDefault(SpaceRole.Member)

    private fun String.toJsonObject(): JSONObject {
        val clean = trim()
        return when {
            clean.startsWith("[") -> JSONArray(clean).optJSONObject(0) ?: JSONObject()
            clean.startsWith("{") -> JSONObject(clean)
            else -> JSONObject()
        }
    }

    private fun String.scalarString(): String {
        val clean = trim()
        return when {
            clean.startsWith("\"") -> JSONObject("{\"value\":$clean}").optString("value")
            clean.startsWith("[") -> JSONArray(clean).optString(0)
            else -> clean
        }
    }

    private fun envelope(topic: String, event: String, payload: JSONObject, ref: String): String = JSONObject()
        .put("topic", topic)
        .put("event", event)
        .put("payload", payload)
        .put("ref", ref)
        .toString()

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

private data class HttpResponse(val code: Int, val body: String) {
    fun requireSuccess() {
        if (code !in 200..299) {
            val message = runCatching {
                val json = JSONObject(body)
                json.optString("message").ifBlank { json.optString("error_description") }.ifBlank { json.optString("hint") }
            }.getOrNull().orEmpty()
            error(message.ifBlank { "Request failed ($code)" })
        }
    }
}
