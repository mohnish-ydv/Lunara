package com.mohnishraj.lunara.data.auth

import com.mohnishraj.lunara.domain.AuthResult
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.delay
import java.util.UUID

class DemoAuthRepository : AuthRepository {
    private var profile: UserProfile? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        delay(650)
        return AuthResult.SignedIn(
            UserSession(
                accessToken = "preview-token",
                refreshToken = "preview-refresh",
                userId = profile?.id ?: UUID.nameUUIDFromBytes(email.toByteArray()).toString(),
                email = email,
            )
        )
    }

    override suspend fun signUp(email: String, password: String): AuthResult = signIn(email, password)

    override suspend fun saveProfile(session: UserSession, profile: UserProfile): Result<UserProfile> {
        delay(500)
        this.profile = profile
        return Result.success(profile)
    }

    override suspend fun loadProfile(session: UserSession): Result<UserProfile?> {
        delay(250)
        return Result.success(profile)
    }
}
