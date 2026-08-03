package com.mohnishraj.lunara.data.spaces

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant
import java.util.UUID

class DemoSpaceRepository : SpaceRepository {
    private val now = Instant.now()
    private val signals = MutableSharedFlow<SpaceSignal>(extraBufferCapacity = 24)

    private val me = UserProfile("me", "me@preview.lunara", "mohnish", "Mohnish Raj", "Building Lunara one calm screen at a time.", 2)
    private val anaya = UserProfile("10000000-0000-0000-0000-000000000001", "anaya@preview.lunara", "anaya", "Anaya Mehta", "Ideas, photos and unfinished playlists.", 3)
    private val arjun = UserProfile("10000000-0000-0000-0000-000000000004", "arjun@preview.lunara", "arjun_s", "Arjun Shah", "Coffee, code and Sunday football.", 0)
    private val zoya = UserProfile("10000000-0000-0000-0000-000000000006", "zoya@preview.lunara", "zoya", "Zoya Khan", "Design systems and good typography.", 5)
    private val kabir = UserProfile("10000000-0000-0000-0000-000000000008", "kabir@preview.lunara", "kabir", "Kabir Rao", "Building tiny things that feel useful.", 1)

    private val summaries = linkedMapOf(
        "space-lunara" to SpaceSummary(
            id = "space-lunara",
            name = "Lunara Studio",
            description = "Product ideas, design decisions and the release pulse.",
            emoji = "🌙",
            accentSeed = 4,
            memberCount = 4,
            channelCount = 3,
            unreadCount = 5,
            lastActivityAt = at(-540),
            myRole = SpaceRole.Owner,
            isFavorite = true,
            inviteCode = "LUNA-7X4Q",
        ),
        "space-weekend" to SpaceSummary(
            id = "space-weekend",
            name = "Weekend Crew",
            description = "Plans that survive the group chat.",
            emoji = "⚡",
            accentSeed = 1,
            memberCount = 3,
            channelCount = 2,
            unreadCount = 1,
            lastActivityAt = at(-7_200),
            myRole = SpaceRole.Admin,
            inviteCode = "WKND-22PR",
        ),
        "space-focus" to SpaceSummary(
            id = "space-focus",
            name = "Deep Work Circle",
            description = "Quiet accountability and small wins.",
            emoji = "🎯",
            accentSeed = 6,
            memberCount = 4,
            channelCount = 2,
            lastActivityAt = at(-86_400),
            myRole = SpaceRole.Member,
            isMuted = true,
            inviteCode = "FOCS-91TY",
        ),
    )

    private val channels = linkedMapOf(
        "space-lunara" to mutableListOf(
            SpaceChannel("channel-lunara-general", "space-lunara", "general", "Daily product conversation", SpaceChannelKind.Chat, 3, "The shared space feels much clearer now.", at(-540), true),
            SpaceChannel("channel-lunara-releases", "space-lunara", "releases", "Important milestones and build notes", SpaceChannelKind.Announcements, 2, "Release candidate review at 7 pm.", at(-1_800), true),
            SpaceChannel("channel-lunara-planning", "space-lunara", "planning", "Decisions, owners and next actions", SpaceChannelKind.Planning, 0, "Three items moved to ready.", at(-7_200)),
        ),
        "space-weekend" to mutableListOf(
            SpaceChannel("channel-weekend-general", "space-weekend", "general", "Plans and chaos", SpaceChannelKind.Chat, 1, "Sunday brunch at eleven?", at(-7_200), true),
            SpaceChannel("channel-weekend-polls", "space-weekend", "polls", "Lock the plan without 80 messages", SpaceChannelKind.Planning, 0, "Venue vote closes tonight.", at(-18_000)),
        ),
        "space-focus" to mutableListOf(
            SpaceChannel("channel-focus-checkin", "space-focus", "check-in", "One intention, one result", SpaceChannelKind.Chat, 0, "Wrapped the first focus block.", at(-86_400), true),
            SpaceChannel("channel-focus-wins", "space-focus", "wins", "Small progress deserves a place", SpaceChannelKind.Announcements, 0, "Kabir shipped the prototype.", at(-172_800)),
        ),
    )

    private val members = linkedMapOf(
        "space-lunara" to mutableListOf(
            SpaceMember(me, SpaceRole.Owner, at(-2_592_000), true),
            SpaceMember(anaya, SpaceRole.Admin, at(-2_500_000), true),
            SpaceMember(zoya, SpaceRole.Member, at(-2_000_000), true),
            SpaceMember(kabir, SpaceRole.Member, at(-1_200_000), false),
        ),
        "space-weekend" to mutableListOf(
            SpaceMember(me, SpaceRole.Admin, at(-3_000_000), true),
            SpaceMember(arjun, SpaceRole.Owner, at(-3_200_000), true),
            SpaceMember(anaya, SpaceRole.Member, at(-2_800_000), true),
        ),
        "space-focus" to mutableListOf(
            SpaceMember(me, SpaceRole.Member, at(-1_900_000), true),
            SpaceMember(kabir, SpaceRole.Owner, at(-2_100_000), false),
            SpaceMember(zoya, SpaceRole.Admin, at(-2_000_000), true),
            SpaceMember(arjun, SpaceRole.Member, at(-1_600_000), true),
        ),
    )

    private val messageMap = linkedMapOf(
        "channel-lunara-general" to mutableListOf(
            message("sg1", "channel-lunara-general", anaya, "I tightened the space cards and reduced the visual noise.", -4_200),
            message("sg2", "channel-lunara-general", me, "Nice. The hierarchy finally feels intentional.", -3_600),
            message("sg3", "channel-lunara-general", zoya, "The shared space feels much clearer now.", -540, reactions = listOf(MessageReaction("✨", 3, true))),
        ),
        "channel-lunara-releases" to mutableListOf(
            message("sr1", "channel-lunara-releases", me, "Release candidate review at 7 pm. Please add blockers before then.", -1_800, announcement = true),
        ),
        "channel-lunara-planning" to mutableListOf(
            message("sp1", "channel-lunara-planning", kabir, "Three items moved to ready. Camera polish still needs one pass.", -7_200),
        ),
        "channel-weekend-general" to mutableListOf(
            message("wg1", "channel-weekend-general", arjun, "Sunday brunch at eleven?", -7_200),
        ),
        "channel-weekend-polls" to mutableListOf(
            message("wp1", "channel-weekend-polls", anaya, "Venue vote closes tonight.", -18_000),
        ),
        "channel-focus-checkin" to mutableListOf(
            message("fc1", "channel-focus-checkin", me, "Wrapped the first focus block.", -86_400),
        ),
        "channel-focus-wins" to mutableListOf(
            message("fw1", "channel-focus-wins", kabir, "Kabir shipped the prototype.", -172_800, announcement = true),
        ),
    )

    override suspend fun spaces(session: UserSession): Result<List<SpaceSummary>> {
        delay(90)
        return Result.success(
            summaries.values.sortedWith(
                compareByDescending<SpaceSummary> { it.isFavorite }
                    .thenByDescending { it.lastActivityAt }
            )
        )
    }

    override suspend fun detail(session: UserSession, spaceId: String): Result<SpaceDetail> {
        delay(100)
        return runCatching {
            val summary = summaries[spaceId] ?: error("Space not found")
            SpaceDetail(summary, channels[spaceId].orEmpty().sortedWith(compareByDescending<SpaceChannel> { it.isPinned }.thenByDescending { it.lastMessageAt }), members[spaceId].orEmpty())
        }
    }

    override suspend fun messages(session: UserSession, channelId: String, before: String?, limit: Int): Result<List<SpaceMessage>> {
        delay(90)
        return runCatching {
            var items = messageMap[channelId]?.toList() ?: error("Channel not found")
            if (!before.isNullOrBlank()) items = items.filter { it.createdAt < before }
            items.takeLast(limit.coerceIn(1, 100))
        }
    }

    override suspend fun createSpace(
        session: UserSession,
        name: String,
        description: String,
        emoji: String,
        memberIds: List<String>,
    ): Result<SpaceDetail> {
        delay(180)
        return runCatching {
            val cleanName = validateSpaceName(name).getOrThrow()
            val cleanDescription = description.trim().take(240)
            val id = "space-${UUID.randomUUID()}"
            val selected = listOf(anaya, arjun, zoya, kabir).filter { it.id in memberIds }.take(24)
            val summary = SpaceSummary(
                id = id,
                name = cleanName,
                description = cleanDescription,
                emoji = emoji.trim().takeIf(String::isNotBlank)?.take(4) ?: "✨",
                accentSeed = summaries.size % 8,
                memberCount = selected.size + 1,
                channelCount = 1,
                lastActivityAt = Instant.now().toString(),
                myRole = SpaceRole.Owner,
                isFavorite = true,
                inviteCode = "LN-${UUID.randomUUID().toString().take(6).uppercase()}",
            )
            val general = SpaceChannel("channel-${UUID.randomUUID()}", id, "general", "The main conversation", SpaceChannelKind.Chat, isPinned = true)
            summaries[id] = summary
            channels[id] = mutableListOf(general)
            members[id] = (listOf(SpaceMember(me, SpaceRole.Owner, Instant.now().toString(), true)) + selected.map { SpaceMember(it, SpaceRole.Member, Instant.now().toString(), false) }).toMutableList()
            messageMap[general.id] = mutableListOf()
            signals.tryEmit(SpaceSignal.Refresh)
            SpaceDetail(summary, listOf(general), members.getValue(id))
        }
    }

    override suspend fun createChannel(
        session: UserSession,
        spaceId: String,
        name: String,
        description: String,
        kind: SpaceChannelKind,
    ): Result<SpaceChannel> {
        delay(160)
        return runCatching {
            val space = summaries[spaceId] ?: error("Space not found")
            check(space.canManage) { "Only owners and admins can create channels" }
            val cleanName = validateChannelName(name).getOrThrow()
            check(channels[spaceId].orEmpty().none { it.name.equals(cleanName, true) }) { "A channel with this name already exists" }
            val channel = SpaceChannel(
                id = "channel-${UUID.randomUUID()}",
                spaceId = spaceId,
                name = cleanName,
                description = description.trim().take(160),
                kind = kind,
            )
            channels.getOrPut(spaceId, ::mutableListOf).add(channel)
            messageMap[channel.id] = mutableListOf()
            summaries[spaceId] = space.copy(channelCount = channels.getValue(spaceId).size, lastActivityAt = Instant.now().toString())
            signals.tryEmit(SpaceSignal.Refresh)
            channel
        }
    }

    override suspend fun sendMessage(
        session: UserSession,
        channelId: String,
        clientId: String,
        body: String,
        replyToId: String?,
    ): Result<SpaceMessage> {
        delay(150)
        return runCatching {
            val list = messageMap[channelId] ?: error("Channel not found")
            list.firstOrNull { it.clientId == clientId }?.let { return@runCatching it }
            val channel = channels.values.flatten().firstOrNull { it.id == channelId } ?: error("Channel not found")
            val space = summaries[channel.spaceId] ?: error("Space not found")
            check(channel.kind != SpaceChannelKind.Announcements || space.canManage) { "Only owners and admins can post announcements" }
            val clean = validateSpaceMessage(body).getOrThrow()
            val reply = replyToId?.takeIf(String::isNotBlank)?.let { id -> list.firstOrNull { it.id == id } ?: error("The replied message is no longer available") }
            val sender = me.copy(id = session.userId, email = session.email)
            val sent = SpaceMessage(
                id = UUID.randomUUID().toString(),
                clientId = clientId,
                channelId = channelId,
                sender = sender,
                body = clean,
                createdAt = Instant.now().toString(),
                replyToId = reply?.id.orEmpty(),
                replyPreview = reply?.body.orEmpty(),
                isAnnouncement = channel.kind == SpaceChannelKind.Announcements,
            )
            list += sent
            updateChannelPreview(channel, sent)
            signals.tryEmit(SpaceSignal.Refresh)
            sent
        }
    }

    override suspend fun react(session: UserSession, messageId: String, emoji: String?): Result<Unit> = mutate {
        val (list, index) = find(messageId)
        val item = list[index]
        val reactions = item.reactions.toMutableList()
        val mine = reactions.indexOfFirst { it.reactedByMe }
        if (mine >= 0) {
            val previous = reactions[mine]
            if (previous.count <= 1) reactions.removeAt(mine) else reactions[mine] = previous.copy(count = previous.count - 1, reactedByMe = false)
        }
        if (!emoji.isNullOrBlank()) {
            val same = reactions.indexOfFirst { it.emoji == emoji }
            if (same >= 0) reactions[same] = reactions[same].copy(count = reactions[same].count + 1, reactedByMe = true)
            else reactions += MessageReaction(emoji, 1, true)
        }
        list[index] = item.copy(reactions = reactions)
    }

    override suspend fun setPreferences(session: UserSession, spaceId: String, favorite: Boolean, muted: Boolean): Result<Unit> = mutate {
        val current = summaries[spaceId] ?: error("Space not found")
        summaries[spaceId] = current.copy(isFavorite = favorite, isMuted = muted)
    }

    override suspend fun markChannelRead(session: UserSession, channelId: String): Result<Unit> = mutate {
        val channel = channels.values.flatten().firstOrNull { it.id == channelId } ?: return@mutate
        val list = channels[channel.spaceId] ?: return@mutate
        val index = list.indexOfFirst { it.id == channelId }
        if (index >= 0) list[index] = list[index].copy(unreadCount = 0)
        val total = list.sumOf(SpaceChannel::unreadCount)
        summaries[channel.spaceId]?.let { summaries[channel.spaceId] = it.copy(unreadCount = total) }
    }

    override suspend fun leaveSpace(session: UserSession, spaceId: String): Result<Unit> = mutate {
        val summary = summaries[spaceId] ?: error("Space not found")
        check(summary.myRole != SpaceRole.Owner) { "Transfer ownership before leaving this space" }
        summaries.remove(spaceId)
        channels.remove(spaceId)?.forEach { messageMap.remove(it.id) }
        members.remove(spaceId)
    }

    override fun observe(session: UserSession): Flow<SpaceSignal> = signals

    private suspend fun mutate(block: () -> Unit): Result<Unit> {
        delay(120)
        return runCatching(block).onSuccess { signals.tryEmit(SpaceSignal.Refresh) }
    }

    private fun find(messageId: String): Pair<MutableList<SpaceMessage>, Int> {
        messageMap.values.forEach { list ->
            val index = list.indexOfFirst { it.id == messageId }
            if (index >= 0) return list to index
        }
        error("Message not found")
    }

    private fun updateChannelPreview(channel: SpaceChannel, message: SpaceMessage) {
        val list = channels[channel.spaceId] ?: return
        val index = list.indexOfFirst { it.id == channel.id }
        if (index >= 0) list[index] = list[index].copy(lastMessage = message.body, lastMessageAt = message.createdAt, unreadCount = 0)
        summaries[channel.spaceId]?.let { current -> summaries[channel.spaceId] = current.copy(lastActivityAt = message.createdAt) }
    }

    private fun message(
        id: String,
        channelId: String,
        sender: UserProfile,
        body: String,
        seconds: Long,
        reactions: List<MessageReaction> = emptyList(),
        announcement: Boolean = false,
    ) = SpaceMessage(id, id, channelId, sender, body, at(seconds), reactions = reactions, isAnnouncement = announcement)

    private fun at(seconds: Long): String = now.plusSeconds(seconds).toString()
}
