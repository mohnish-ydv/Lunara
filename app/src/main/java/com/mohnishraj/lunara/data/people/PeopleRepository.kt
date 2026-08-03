package com.mohnishraj.lunara.data.people

import com.mohnishraj.lunara.domain.PeopleSnapshot
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession

interface PeopleRepository {
    suspend fun search(session: UserSession, query: String): Result<List<UserProfile>>
    suspend fun snapshot(session: UserSession): Result<PeopleSnapshot>
    suspend fun sendRequest(session: UserSession, personId: String): Result<Unit>
    suspend fun acceptRequest(session: UserSession, requestId: String): Result<Unit>
    suspend fun rejectRequest(session: UserSession, requestId: String): Result<Unit>
    suspend fun cancelRequest(session: UserSession, requestId: String): Result<Unit>
    suspend fun removeConnection(session: UserSession, connectionId: String): Result<Unit>
    suspend fun block(session: UserSession, personId: String): Result<Unit>
    suspend fun unblock(session: UserSession, personId: String): Result<Unit>
    suspend fun updatePrivacy(
        session: UserSession,
        discoverable: Boolean,
        allowRequests: Boolean,
    ): Result<Unit>
}
