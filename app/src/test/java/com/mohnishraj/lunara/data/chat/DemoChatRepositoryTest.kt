package com.mohnishraj.lunara.data.chat

import com.mohnishraj.lunara.domain.ConversationSettings
import com.mohnishraj.lunara.domain.MessageModule
import com.mohnishraj.lunara.domain.MessageModuleType
import com.mohnishraj.lunara.domain.MediaAttachment
import com.mohnishraj.lunara.domain.MediaKind
import com.mohnishraj.lunara.domain.ModuleChecklistItem
import com.mohnishraj.lunara.domain.UserProfile
import com.mohnishraj.lunara.domain.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoChatRepositoryTest {
    private val session = UserSession("token", "refresh", "current-user", "me@example.com")

    @Test
    fun ensureConversationAndSendAreIdempotent() = runBlocking {
        val repository = DemoChatRepository()
        val person = UserProfile(
            id = "10000000-0000-0000-0000-000000000099",
            email = "new@preview.lunara",
            username = "new_friend",
            displayName = "New Friend",
        )

        val firstConversation = repository.ensureConversation(session, person).getOrThrow()
        val secondConversation = repository.ensureConversation(session, person).getOrThrow()
        assertEquals(firstConversation.id, secondConversation.id)

        val firstSend = repository.sendMessage(
            session = session,
            conversationId = firstConversation.id,
            clientId = "30000000-0000-0000-0000-000000000001",
            body = "Hello there",
            replyToId = null,
        ).getOrThrow()
        val repeatedSend = repository.sendMessage(
            session = session,
            conversationId = firstConversation.id,
            clientId = "30000000-0000-0000-0000-000000000001",
            body = "Hello there",
            replyToId = null,
        ).getOrThrow()

        assertEquals(firstSend.id, repeatedSend.id)
        assertEquals(1, repository.messages(session, firstConversation.id).getOrThrow().size)
    }

    @Test
    fun editReactAndDeleteLifecycleIsConsistent() = runBlocking {
        val repository = DemoChatRepository()

        repository.editMessage(session, "a2", "The pacing feels right now.").getOrThrow()
        repository.react(session, "a2", "✨").getOrThrow()
        val edited = repository.messages(session, "demo-chat-anaya").getOrThrow().single { it.id == "a2" }

        assertEquals("The pacing feels right now.", edited.body)
        assertTrue(edited.isEdited)
        assertEquals("✨", edited.reactions.single().emoji)
        assertTrue(edited.reactions.single().reactedByMe)

        repository.deleteMessage(session, "a2").getOrThrow()
        val deleted = repository.messages(session, "demo-chat-anaya").getOrThrow().single { it.id == "a2" }
        assertTrue(deleted.isDeleted)
        assertEquals("", deleted.body)
        assertTrue(deleted.reactions.isEmpty())
    }

    @Test
    fun markingConversationReadClearsUnreadCount() = runBlocking {
        val repository = DemoChatRepository()
        val before = repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }
        assertTrue(before.unreadCount > 0)

        repository.markRead(session, before.id).getOrThrow()

        val after = repository.conversations(session).getOrThrow().single { it.id == before.id }
        assertEquals(0, after.unreadCount)
        assertNotNull(repository.messages(session, before.id).getOrThrow().firstOrNull())
    }
    @Test
    fun invalidReplyAndOversizedMessagesAreRejected() = runBlocking {
        val repository = DemoChatRepository()

        val invalidReply = repository.sendMessage(
            session = session,
            conversationId = "demo-chat-anaya",
            clientId = "invalid-reply-client",
            body = "Reply",
            replyToId = "missing-message",
        )
        assertTrue(invalidReply.isFailure)

        val oversized = repository.sendMessage(
            session = session,
            conversationId = "demo-chat-anaya",
            clientId = "oversized-client",
            body = "x".repeat(4001),
            replyToId = null,
        )
        assertTrue(oversized.isFailure)
    }

    @Test
    fun editingOlderMessageDoesNotReplaceLatestConversationPreview() = runBlocking {
        val repository = DemoChatRepository()
        val before = repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }

        repository.editMessage(session, "a2", "Updated older note").getOrThrow()

        val after = repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }
        assertEquals(before.lastMessage, after.lastMessage)
        assertEquals(before.lastMessageAt, after.lastMessageAt)
    }

    @Test
    fun deletingLatestOwnMessageRefreshesConversationPreview() = runBlocking {
        val repository = DemoChatRepository()
        val sent = repository.sendMessage(
            session = session,
            conversationId = "demo-chat-anaya",
            clientId = "latest-own-client",
            body = "Temporary latest message",
            replyToId = null,
        ).getOrThrow()
        assertEquals(
            "Temporary latest message",
            repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }.lastMessage,
        )

        repository.deleteMessage(session, sent.id).getOrThrow()

        assertEquals(
            "Message removed",
            repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }.lastMessage,
        )
    }

    @Test
    fun organizationSettingsPersistAndSortPinnedFirst() = runBlocking {
        val repository = DemoChatRepository()
        val arjun = repository.conversations(session).getOrThrow().single { it.id == "demo-chat-arjun" }

        repository.updateConversationSettings(
            session,
            arjun.id,
            ConversationSettings(
                isPinned = true,
                isArchived = false,
                isMuted = false,
                labels = listOf("Weekend", "Friends", "Friends", "  "),
            ),
        ).getOrThrow()

        val conversations = repository.conversations(session).getOrThrow()
        val updated = conversations.single { it.id == arjun.id }
        assertTrue(updated.isPinned)
        assertTrue(!updated.isMuted)
        assertEquals(listOf("Weekend", "Friends"), updated.labels)
        val firstUnpinnedIndex = conversations.indexOfFirst { !it.isPinned }.let { if (it == -1) conversations.size else it }
        assertTrue(conversations.take(firstUnpinnedIndex).all { it.isPinned })
        assertTrue(conversations.drop(firstUnpinnedIndex).none { it.isPinned })
    }

    @Test
    fun bookmarksArePrivateCollectionAndDeletedMessageIsRemoved() = runBlocking {
        val repository = DemoChatRepository()

        repository.setBookmark(session, "a3", true).getOrThrow()
        val saved = repository.bookmarkedMessages(session, "demo-chat-anaya").getOrThrow()
        assertTrue(saved.any { it.id == "a3" && it.isBookmarked })

        repository.setBookmark(session, "a3", false).getOrThrow()
        assertTrue(repository.bookmarkedMessages(session, "demo-chat-anaya").getOrThrow().none { it.id == "a3" })

        repository.deleteMessage(session, "a2").getOrThrow()
        assertTrue(repository.bookmarkedMessages(session, "demo-chat-anaya").getOrThrow().none { it.id == "a2" })
    }

    @Test
    fun messageSearchIsScopedCaseInsensitiveAndSkipsRemovedMessages() = runBlocking {
        val repository = DemoChatRepository()

        val results = repository.searchMessages(session, "demo-chat-zoya", "REFERENCE").getOrThrow()
        assertEquals(1, results.size)
        assertTrue(results.single().body.contains("reference", ignoreCase = true))

        repository.deleteMessage(session, "a2").getOrThrow()
        val removed = repository.searchMessages(session, "demo-chat-anaya", "pacing").getOrThrow()
        assertTrue(removed.isEmpty())
    }

    @Test
    fun archiveStateDoesNotDestroyConversationOrMessages() = runBlocking {
        val repository = DemoChatRepository()
        val anaya = repository.conversations(session).getOrThrow().single { it.id == "demo-chat-anaya" }

        repository.updateConversationSettings(
            session,
            anaya.id,
            ConversationSettings(false, true, true, listOf("Later")),
        ).getOrThrow()

        val archived = repository.conversations(session).getOrThrow().single { it.id == anaya.id }
        assertTrue(archived.isArchived)
        assertTrue(archived.isMuted)
        assertTrue(repository.messages(session, anaya.id).getOrThrow().isNotEmpty())
    }

    @Test
    fun sharedLinksIncludeHttpAndWwwMessagesOnly() = runBlocking {
        val repository = DemoChatRepository()
        val links = repository.sharedLinks(session, "demo-chat-zoya").getOrThrow()
        assertTrue(links.isNotEmpty())
        assertTrue(links.all { it.containsLink && !it.isDeleted })
    }

    @Test
    fun moduleSendIsIdempotentAndUpdatesUseRevision() = runBlocking {
        val repository = DemoChatRepository()
        val module = MessageModule(
            type = MessageModuleType.Checklist,
            title = "Release pass",
            items = listOf(
                ModuleChecklistItem("one", "Build"),
                ModuleChecklistItem("two", "Test"),
            ),
        )
        val first = repository.sendModuleMessage(
            session,
            "demo-chat-anaya",
            "module-client",
            module,
            null,
        ).getOrThrow()
        val repeated = repository.sendModuleMessage(
            session,
            "demo-chat-anaya",
            "module-client",
            module,
            null,
        ).getOrThrow()
        assertEquals(first.id, repeated.id)

        repository.updateModule(session, first.id, 0, module.toggleChecklistItem("one")).getOrThrow()
        val updated = repository.messages(session, "demo-chat-anaya").getOrThrow().single { it.id == first.id }
        assertEquals(1, updated.moduleRevision)
        assertTrue(updated.module!!.items.first().completed)

        val stale = repository.updateModule(session, first.id, 0, module)
        assertTrue(stale.isFailure)
    }


    @Test
    fun mediaSendIsIdempotentAndAppearsInGallery() = runBlocking {
        val repository = DemoChatRepository()
        val conversation = repository.conversations(session).getOrThrow().first()
        val attachment = MediaAttachment(
            id = "media-1",
            kind = MediaKind.Document,
            fileName = "design-brief.pdf",
            mimeType = "application/pdf",
            sizeBytes = 120_000,
            localUri = "file:///tmp/design-brief.pdf",
            caption = "Latest brief",
        )
        val progress = mutableListOf<Int>()
        val first = repository.sendMediaMessage(
            session,
            conversation.id,
            "44444444-4444-4444-4444-444444444444",
            attachment,
            onProgress = progress::add,
        ).getOrThrow()
        val duplicate = repository.sendMediaMessage(
            session,
            conversation.id,
            "44444444-4444-4444-4444-444444444444",
            attachment,
        ).getOrThrow()

        assertEquals(first.id, duplicate.id)
        assertEquals(MediaKind.Document, first.attachment?.kind)
        assertEquals(100, progress.last())
        assertTrue(repository.mediaMessages(session, conversation.id).getOrThrow().any { it.id == first.id })
    }

}
