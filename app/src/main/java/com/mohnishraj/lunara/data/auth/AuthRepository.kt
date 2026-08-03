package com.mohnishraj.lunara.data.auth

import com.mohnishraj.lunara.domain.AuthResult
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun saveProfile(session: UserSession, profile: UserProfile): Result<UserProfile>
    suspend fun loadProfile(session: UserSession): Result<UserProfile?>
}
