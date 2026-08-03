package com.mohnishraj.lunara.data.auth

import com.mohnishraj.lunara.core.AppConfig
import com.mohnishraj.lunara.domain.AuthResult
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

class SupabaseAuthRepository : AuthRepository {
    override suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        requestAuth(
            path = "/auth/v1/token?grant_type=password",
            payload = JSONObject().put("email", email).put("password", password),
            confirmationEmail = null,
        )
    }

    override suspend fun signUp(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        requestAuth(
            path = "/auth/v1/signup",
            payload = JSONObject().put("email", email).put("password", password),
            confirmationEmail = email,
        )
    }

    override suspend fun saveProfile(session: UserSession, profile: UserProfile): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONArray().put(
                    JSONObject()
                        .put("id", profile.id)
                        .put("email", profile.email)
                        .put("username", profile.username)
                        .put("display_name", profile.displayName)
                        .put("bio", profile.bio)
                        .put("avatar_seed", profile.avatarSeed)
                        .put("is_discoverable", profile.isDiscoverable)
                        .put("allow_requests", profile.allowRequests)
                )
                val response = request(
                    method = "POST",
                    path = "/rest/v1/profiles?on_conflict=id",
                    token = session.accessToken,
                    body = payload.toString(),
                    extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation"),
                )
                if (response.code !in 200..299) error(response.errorMessage())
                val objectJson = JSONArray(response.body).optJSONObject(0)
                objectJson?.toProfile() ?: profile
            }
        }

    override suspend fun loadProfile(session: UserSession): Result<UserProfile?> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request(
                method = "GET",
                path = "/rest/v1/profiles?id=eq.${session.userId}&select=*",
                token = session.accessToken,
            )
            if (response.code !in 200..299) error(response.errorMessage())
            JSONArray(response.body).optJSONObject(0)?.toProfile()
        }
    }

    private fun requestAuth(path: String, payload: JSONObject, confirmationEmail: String?): AuthResult {
        return try {
        val response = request("POST", path, body = payload.toString())
        if (response.code !in 200..299) return AuthResult.Failure(response.errorMessage())
        val json = JSONObject(response.body)
        val accessToken = json.optString("access_token")
        val user = json.optJSONObject("user")
        if (accessToken.isBlank()) {
            return AuthResult.ConfirmationRequired(confirmationEmail ?: user?.optString("email").orEmpty())
        }
        AuthResult.SignedIn(
            UserSession(
                accessToken = accessToken,
                refreshToken = json.optString("refresh_token"),
                userId = user?.optString("id").orEmpty(),
                email = user?.optString("email").orEmpty(),
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + json.optLong("expires_in", 3600L),
            )
        )
        } catch (error: Exception) {
            AuthResult.Failure(error.userFacingMessage())
        }
    }

    private fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = URI.create(AppConfig.supabaseUrl + path).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", AppConfig.supabaseAnonKey)
            connection.setRequestProperty("Authorization", "Bearer ${token ?: AppConfig.supabaseAnonKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            extraHeaders.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input -> BufferedReader(InputStreamReader(input)).use { reader -> reader.readText() } }.orEmpty()
            HttpResponse(code, text)
        } finally {
            connection.disconnect()
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

    private data class HttpResponse(val code: Int, val body: String) {
        fun errorMessage(): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            return json?.optString("msg")?.takeIf(String::isNotBlank)
                ?: json?.optString("message")?.takeIf(String::isNotBlank)
                ?: json?.optString("error_description")?.takeIf(String::isNotBlank)
                ?: "Something went wrong. Please try again."
        }
    }

    private fun Throwable.userFacingMessage(): String = when (this) {
        is java.net.SocketTimeoutException -> "The connection took too long. Try again."
        is java.net.UnknownHostException -> "No internet connection"
        else -> message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again."
    }
}
