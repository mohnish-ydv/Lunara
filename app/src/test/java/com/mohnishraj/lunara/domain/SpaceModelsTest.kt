package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceModelsTest {
    @Test
    fun spaceNameValidationNormalizesWhitespace() {
        assertEquals("Design Circle", validateSpaceName("  Design   Circle ").getOrThrow())
    }

    @Test
    fun channelValidationCreatesSafeSlug() {
        assertEquals("release-notes", validateChannelName(" Release Notes ").getOrThrow())
    }

    @Test
    fun invalidNamesAndMessagesAreRejected() {
        assertTrue(validateSpaceName("x").isFailure)
        assertTrue(validateChannelName("!").isFailure)
        assertTrue(validateSpaceMessage(" ").isFailure)
        assertTrue(validateSpaceMessage("x".repeat(4001)).isFailure)
    }

    @Test
    fun spaceSearchIncludesDescriptionAndInviteCode() {
        val space = SpaceSummary("one", "Lunara Studio", "Calm product work", inviteCode = "LUNA-7X4Q")
        assertTrue(space.matches("product"))
        assertTrue(space.matches("7x4q"))
        assertTrue(space.matches("studio"))
    }
}
