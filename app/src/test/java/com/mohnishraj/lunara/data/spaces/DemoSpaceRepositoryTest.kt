package com.mohnishraj.lunara.data.spaces

import com.mohnishraj.lunara.domain.SpaceChannelKind
import com.mohnishraj.lunara.domain.SpaceRole
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSpaceRepositoryTest {
    private val session = UserSession("token", "refresh", "current-user", "me@example.com")

    @Test
    fun favoritesSortBeforeOtherSpaces() = runBlocking {
        val repository = DemoSpaceRepository()
        val spaces = repository.spaces(session).getOrThrow()
        val firstNonFavorite = spaces.indexOfFirst { !it.isFavorite }.let { if (it == -1) spaces.size else it }
        assertTrue(spaces.take(firstNonFavorite).all { it.isFavorite })
        assertTrue(spaces.drop(firstNonFavorite).none { it.isFavorite })
    }

    @Test
    fun createSpaceAddsOwnerAndGeneralChannel() = runBlocking {
        val repository = DemoSpaceRepository()
        val created = repository.createSpace(session, "Launch Crew", "Ship together", "🚀", emptyList()).getOrThrow()
        assertEquals("Launch Crew", created.space.name)
        assertEquals(SpaceRole.Owner, created.space.myRole)
        assertEquals("general", created.channels.single().name)
        assertEquals(SpaceRole.Owner, created.members.single().role)
    }

    @Test
    fun messageSendIsIdempotentAndUpdatesChannelPreview() = runBlocking {
        val repository = DemoSpaceRepository()
        val first = repository.sendMessage(session, "channel-lunara-general", "same-client", "Hello space", null).getOrThrow()
        val repeated = repository.sendMessage(session, "channel-lunara-general", "same-client", "Hello space", null).getOrThrow()
        assertEquals(first.id, repeated.id)
        assertEquals(1, repository.messages(session, "channel-lunara-general").getOrThrow().count { it.clientId == "same-client" })
        assertEquals("Hello space", repository.detail(session, "space-lunara").getOrThrow().channels.single { it.id == "channel-lunara-general" }.lastMessage)
    }

    @Test
    fun invalidReplyAndOversizedMessageAreRejected() = runBlocking {
        val repository = DemoSpaceRepository()
        assertTrue(repository.sendMessage(session, "channel-lunara-general", "bad-reply", "reply", "missing").isFailure)
        assertTrue(repository.sendMessage(session, "channel-lunara-general", "too-long", "x".repeat(4001), null).isFailure)
    }

    @Test
    fun preferencesAndReadStatePersist() = runBlocking {
        val repository = DemoSpaceRepository()
        repository.setPreferences(session, "space-weekend", favorite = true, muted = true).getOrThrow()
        repository.markChannelRead(session, "channel-weekend-general").getOrThrow()
        val detail = repository.detail(session, "space-weekend").getOrThrow()
        assertTrue(detail.space.isFavorite)
        assertTrue(detail.space.isMuted)
        assertEquals(0, detail.channels.single { it.id == "channel-weekend-general" }.unreadCount)
    }

    @Test
    fun managersCanCreateChannelsAndMembersCannotPostAnnouncements() = runBlocking {
        val repository = DemoSpaceRepository()
        val channel = repository.createChannel(session, "space-lunara", "QA Notes", "Build checks", SpaceChannelKind.Planning).getOrThrow()
        assertEquals("qa-notes", channel.name)

        val blocked = repository.sendMessage(session, "channel-focus-wins", "member-announcement", "I should not post", null)
        assertTrue(blocked.isFailure)
    }

    @Test
    fun reactionsReplaceCurrentUsersPreviousReaction() = runBlocking {
        val repository = DemoSpaceRepository()
        repository.react(session, "sg1", "✨").getOrThrow()
        repository.react(session, "sg1", "❤️").getOrThrow()
        val message = repository.messages(session, "channel-lunara-general").getOrThrow().single { it.id == "sg1" }
        assertEquals(1, message.reactions.count { it.reactedByMe })
        assertEquals("❤️", message.reactions.single { it.reactedByMe }.emoji)
    }
}
