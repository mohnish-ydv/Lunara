package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationOrganizationTest {
    private val person = UserProfile(
        id = "person",
        email = "person@example.com",
        username = "design_friend",
        displayName = "Design Friend",
    )

    @Test
    fun conversationSearchMatchesPersonMessageLabelsAndDraft() {
        val conversation = ChatConversation(
            id = "conversation",
            person = person,
            lastMessage = "Review the polished interaction",
            labels = listOf("Portfolio", "Design"),
            draft = "Remember motion notes",
        )

        listOf("friend", "polished", "portfolio", "motion").forEach { query ->
            assertTrue("Expected query to match: $query", conversation.matches(query))
        }
        assertTrue(!conversation.matches("finance"))
    }

    @Test
    fun blankSearchAlwaysMatchesConversation() {
        assertTrue(ChatConversation(id = "conversation", person = person).matches("   "))
    }

    @Test
    fun settingsPreserveIndependentOrganizationFlags() {
        val settings = ConversationSettings(
            isPinned = true,
            isArchived = false,
            isMuted = true,
            labels = listOf("Friends", "Focus"),
        )
        assertTrue(settings.isPinned)
        assertTrue(settings.isMuted)
        assertEquals(listOf("Friends", "Focus"), settings.labels)
    }

    @Test
    fun linkDetectionRecognizesWebLinksOnly() {
        fun message(body: String) = ChatMessage(
            id = body.hashCode().toString(),
            clientId = body.hashCode().toString(),
            conversationId = "conversation",
            senderId = "person",
            body = body,
            createdAt = "2026-08-02T00:00:00Z",
        )
        assertTrue(message("Open https://lunara.example/demo").containsLink)
        assertTrue(message("Open www.example.com").containsLink)
        assertTrue(!message("No link in this note").containsLink)
    }
}
