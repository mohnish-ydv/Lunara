package com.mohnishraj.lunara.data.spaces

import com.mohnishraj.lunara.domain.SpaceChannel
import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceDetail
import com.mohnishraj.lunara.domain.SpaceMessage
import com.mohnishraj.lunara.domain.SpaceSignal
import com.mohnishraj.lunara.domain.SpaceSummary
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.flow.Flow

interface SpaceRepository {
    suspend fun spaces(session: UserSession): Result<List<SpaceSummary>>
    suspend fun detail(session: UserSession, spaceId: String): Result<SpaceDetail>
    suspend fun messages(
        session: UserSession,
        channelId: String,
        before: String? = null,
        limit: Int = 60,
    ): Result<List<SpaceMessage>>

    suspend fun createSpace(
        session: UserSession,
        name: String,
        description: String,
        emoji: String,
        memberIds: List<String>,
    ): Result<SpaceDetail>

    suspend fun createChannel(
        session: UserSession,
        spaceId: String,
        name: String,
        description: String,
        kind: SpaceChannelKind,
    ): Result<SpaceChannel>

    suspend fun sendMessage(
        session: UserSession,
        channelId: String,
        clientId: String,
        body: String,
        replyToId: String? = null,
    ): Result<SpaceMessage>

    suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit>
    suspend fun setPreferences(session: UserSession, spaceId: String, favorite: Boolean, muted: Boolean): Result<Unit>
    suspend fun markChannelRead(session: UserSession, channelId: String): Result<Unit>
    suspend fun leaveSpace(session: UserSession, spaceId: String): Result<Unit>
    fun observe(session: UserSession): Flow<SpaceSignal>
}
