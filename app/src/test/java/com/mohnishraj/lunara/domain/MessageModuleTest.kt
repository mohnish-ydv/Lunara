package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageModuleTest {
    @Test
    fun checklistAndTaskActionsAreDeterministic() {
        val checklist = MessageModule(
            type = MessageModuleType.Checklist,
            title = "Launch",
            items = listOf(
                ModuleChecklistItem("one", "Review", false),
                ModuleChecklistItem("two", "Record", true),
            ),
        )
        val toggled = checklist.toggleChecklistItem("one")
        assertTrue(toggled.items.first().completed)
        assertTrue(toggled.items.last().completed)

        val task = MessageModule(MessageModuleType.Task, "Ship M5")
        assertTrue(task.toggleTask().completed)
    }

    @Test
    fun pollVoteMovesUserBetweenOptions() {
        val poll = MessageModule(
            type = MessageModuleType.Poll,
            title = "Pick one",
            options = listOf(
                ModulePollOption("a", "A", listOf("user")),
                ModulePollOption("b", "B"),
            ),
        )
        val updated = poll.vote("b", "user")
        assertTrue(updated.options.first().voterIds.isEmpty())
        assertEquals(listOf("user"), updated.options.last().voterIds)
    }

    @Test
    fun validationRequiresStructuredFields() {
        assertTrue(MessageModule(MessageModuleType.Poll, "Question").validate().isFailure)
        assertTrue(
            MessageModule(
                MessageModuleType.Poll,
                "Question",
                options = listOf(ModulePollOption("a", "A"), ModulePollOption("b", "B")),
            ).validate().isSuccess
        )
        assertTrue(MessageModule(MessageModuleType.Code, "Snippet", code = "").validate().isFailure)
        assertTrue(MessageModule(MessageModuleType.Contact, "Contact", contactName = "A", contactValue = "a@example.com").validate().isSuccess)
    }

    @Test
    fun modulePreviewIsAccessibleSearchableText() {
        val module = MessageModule(MessageModuleType.Event, "Portfolio review")
        val message = ChatMessage(
            id = "m",
            clientId = "c",
            conversationId = "conversation",
            senderId = "sender",
            body = module.previewText(),
            createdAt = "2026-08-02T00:00:00Z",
            module = module,
        )
        assertEquals("Event · Portfolio review", message.previewText)
    }
    @Test
    fun temporalAndLocationCardsRejectIncompleteOrUnsafeValues() {
        assertTrue(MessageModule(MessageModuleType.Event, "Review").validate().isFailure)
        assertTrue(MessageModule(MessageModuleType.Reminder, "Follow up").validate().isFailure)
        assertTrue(
            MessageModule(
                MessageModuleType.Location,
                "Studio",
                locationName = "Design studio",
                latitude = 91.0,
                longitude = 85.0,
            ).validate().isFailure
        )
        assertTrue(
            MessageModule(
                MessageModuleType.Location,
                "Studio",
                locationName = "Design studio",
                latitude = 25.59,
                longitude = 85.13,
            ).validate().isSuccess
        )
    }

    @Test
    fun structuredOptionIdentifiersMustBeUnique() {
        val duplicateChecklist = MessageModule(
            MessageModuleType.Checklist,
            "Review",
            items = listOf(
                ModuleChecklistItem("same", "Visual pass"),
                ModuleChecklistItem("same", "Accessibility pass"),
            ),
        )
        assertTrue(duplicateChecklist.validate().isFailure)
    }

}
