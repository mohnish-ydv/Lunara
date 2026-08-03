package com.mohnishraj.lunara.data.people

import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.domain.ConnectedPerson
import com.mohnishraj.lunara.domain.ConnectionDirection
import com.mohnishraj.lunara.domain.ConnectionRequest
import com.mohnishraj.lunara.domain.PeopleSnapshot
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SupabasePeopleRepository : PeopleRepository {
    override suspend fun search(session: UserSession, query: String): Result<List<UserProfile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val needle = query.trim().removePrefix("@").take(40)
                val parameters = mutableListOf(
                    "select" to "*",
                    "id" to "neq.${session.userId}",
                    "is_discoverable" to "eq.true",
                    "order" to "display_name.asc",
                    "limit" to "30",
                )
                if (needle.isNotBlank()) {
                    val safeNeedle = needle.replace("*", "").replace(",", "").replace("(", "").replace(")", "")
                    parameters += "or" to "(username.ilike.*$safeNeedle*,display_name.ilike.*$safeNeedle*,share_code.ilike.*$safeNeedle*)"
                }
                val response = request(
                    method = "GET",
                    path = "/rest/v1/profiles?${queryString(parameters)}",
                    token = session.accessToken,
                )
                response.requireSuccess()
                JSONArray(response.body).toProfiles()
            }
        }

    override suspend fun snapshot(session: UserSession): Result<PeopleSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val connectionResponse = request(
                method = "GET",
                path = "/rest/v1/connections?${queryString(
                    listOf(
                        "select" to "id,requester_id,recipient_id,status,created_at,updated_at",
                        "or" to "(requester_id.eq.${session.userId},recipient_id.eq.${session.userId})",
                        "order" to "updated_at.desc",
                    )
                )}",
                token = session.accessToken,
            )
            connectionResponse.requireSuccess()

            val blockResponse = request(
                method = "GET",
                path = "/rest/v1/blocks?${queryString(
                    listOf(
                        "select" to "blocked_id",
                        "blocker_id" to "eq.${session.userId}",
                    )
                )}",
                token = session.accessToken,
            )
            blockResponse.requireSuccess()

            val connectionRows = JSONArray(connectionResponse.body).toConnectionRows()
            val blockedIds = JSONArray(blockResponse.body).toIdSet("blocked_id")
            val profileIds = buildSet {
                connectionRows.forEach { row ->
                    add(if (row.requesterId == session.userId) row.recipientId else row.requesterId)
                }
                addAll(blockedIds)
            }
            val profiles = loadProfiles(session, profileIds).associateBy(UserProfile::id)

            val incoming = mutableListOf<ConnectionRequest>()
            val outgoing = mutableListOf<ConnectionRequest>()
            val accepted = mutableListOf<ConnectedPerson>()
            connectionRows.forEach { row ->
                val otherId = if (row.requesterId == session.userId) row.recipientId else row.requesterId
                val person = profiles[otherId] ?: return@forEach
                when (row.status) {
                    "accepted" -> accepted += ConnectedPerson(row.id, person, row.updatedAt)
                    "pending" -> {
                        if (row.recipientId == session.userId) {
                            incoming += ConnectionRequest(row.id, person, ConnectionDirection.Incoming, row.createdAt)
                        } else {
                            outgoing += ConnectionRequest(row.id, person, ConnectionDirection.Outgoing, row.createdAt)
                        }
                    }
                }
            }

            PeopleSnapshot(
                incoming = incoming.filterNot { it.person.id in blockedIds },
                outgoing = outgoing.filterNot { it.person.id in blockedIds },
                connections = accepted.filterNot { it.person.id in blockedIds },
                blocked = blockedIds.mapNotNull(profiles::get),
            )
        }
    }

    override suspend fun sendRequest(session: UserSession, personId: String): Result<Unit> = write {
        request(
            method = "POST",
            path = "/rest/v1/connections",
            token = session.accessToken,
            body = JSONObject()
                .put("requester_id", session.userId)
                .put("recipient_id", personId)
                .put("status", "pending")
                .toString(),
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    override suspend fun acceptRequest(session: UserSession, requestId: String): Result<Unit> = write {
        request(
            method = "PATCH",
            path = "/rest/v1/connections?${queryString(
                listOf("id" to "eq.$requestId", "recipient_id" to "eq.${session.userId}")
            )}",
            token = session.accessToken,
            body = JSONObject().put("status", "accepted").toString(),
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    override suspend fun rejectRequest(session: UserSession, requestId: String): Result<Unit> = deleteConnection(
        session = session,
        requestId = requestId,
        ownershipFilter = "recipient_id" to "eq.${session.userId}",
    )

    override suspend fun cancelRequest(session: UserSession, requestId: String): Result<Unit> = deleteConnection(
        session = session,
        requestId = requestId,
        ownershipFilter = "requester_id" to "eq.${session.userId}",
    )

    override suspend fun removeConnection(session: UserSession, connectionId: String): Result<Unit> = write {
        request(
            method = "DELETE",
            path = "/rest/v1/connections?${queryString(listOf("id" to "eq.$connectionId"))}",
            token = session.accessToken,
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    override suspend fun block(session: UserSession, personId: String): Result<Unit> = write {
        request(
            method = "POST",
            path = "/rest/v1/blocks",
            token = session.accessToken,
            body = JSONObject()
                .put("blocker_id", session.userId)
                .put("blocked_id", personId)
                .toString(),
            extraHeaders = mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"),
        ).requireSuccess()
    }

    override suspend fun unblock(session: UserSession, personId: String): Result<Unit> = write {
        request(
            method = "DELETE",
            path = "/rest/v1/blocks?${queryString(
                listOf(
                    "blocker_id" to "eq.${session.userId}",
                    "blocked_id" to "eq.$personId",
                )
            )}",
            token = session.accessToken,
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    override suspend fun updatePrivacy(
        session: UserSession,
        discoverable: Boolean,
        allowRequests: Boolean,
    ): Result<Unit> = write {
        request(
            method = "PATCH",
            path = "/rest/v1/profiles?${queryString(listOf("id" to "eq.${session.userId}"))}",
            token = session.accessToken,
            body = JSONObject()
                .put("is_discoverable", discoverable)
                .put("allow_requests", allowRequests)
                .toString(),
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    private suspend fun deleteConnection(
        session: UserSession,
        requestId: String,
        ownershipFilter: Pair<String, String>,
    ): Result<Unit> = write {
        request(
            method = "DELETE",
            path = "/rest/v1/connections?${queryString(listOf("id" to "eq.$requestId", ownershipFilter))}",
            token = session.accessToken,
            extraHeaders = mapOf("Prefer" to "return=minimal"),
        ).requireSuccess()
    }

    private suspend fun loadProfiles(session: UserSession, ids: Set<String>): List<UserProfile> {
        if (ids.isEmpty()) return emptyList()
        val response = request(
            method = "GET",
            path = "/rest/v1/profiles?${queryString(
                listOf(
                    "select" to "*",
                    "id" to "in.(${ids.joinToString(",")})",
                )
            )}",
            token = session.accessToken,
        )
        response.requireSuccess()
        return JSONArray(response.body).toProfiles()
    }

    private suspend fun write(action: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching(action)
    }

    private fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = URI.create(AppConfig.supabaseUrl + path).toURL().openConnection() as HttpURLConnection
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

    private fun JSONArray.toProfiles(): List<UserProfile> = buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(it.toProfile()) }
        }
    }

    private fun JSONArray.toConnectionRows(): List<ConnectionRow> = buildList {
        for (index in 0 until length()) {
            val json = optJSONObject(index) ?: continue
            add(
                ConnectionRow(
                    id = json.optString("id"),
                    requesterId = json.optString("requester_id"),
                    recipientId = json.optString("recipient_id"),
                    status = json.optString("status"),
                    createdAt = json.optString("created_at"),
                    updatedAt = json.optString("updated_at"),
                )
            )
        }
    }

    private fun JSONArray.toIdSet(key: String): Set<String> = buildSet {
        for (index in 0 until length()) {
            optJSONObject(index)?.optString(key)?.takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun JSONObject.toProfile(): UserProfile = UserProfile(
        id = optString("id"),
        email = optString("email"),
        username = optString("username"),
        displayName = optString("display_name"),
        bio = optString("bio"),
        avatarSeed = optInt("avatar_seed"),
        shareCode = optString("share_code"),
        isDiscoverable = optBoolean("is_discoverable", true),
        allowRequests = optBoolean("allow_requests", true),
    )

    private data class ConnectionRow(
        val id: String,
        val requesterId: String,
        val recipientId: String,
        val status: String,
        val createdAt: String,
        val updatedAt: String,
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
                    409 -> "A request already exists between these profiles."
                    else -> "Could not complete that action. Try again."
                }
        }
    }
}
