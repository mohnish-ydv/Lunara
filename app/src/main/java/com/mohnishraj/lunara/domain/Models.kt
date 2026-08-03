package com.mohnishraj.lunara.domain

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAtEpochSeconds: Long = 0L,
)

data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarSeed: Int = 0,
    val shareCode: String = "",
    val isDiscoverable: Boolean = true,
    val allowRequests: Boolean = true,
) {
    val resolvedShareCode: String
        get() = shareCode.ifBlank {
            "LN-${id.replace("-", "").take(8).uppercase()}"
        }
}

sealed interface AuthResult {
    data class SignedIn(val session: UserSession) : AuthResult
    data class ConfirmationRequired(val email: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}
