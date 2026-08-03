package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {
    @Test
    fun validEmailPasses() {
        assertNull(Validators.emailError("hello@example.com"))
    }

    @Test
    fun malformedEmailFails() {
        assertEquals("Enter a valid email", Validators.emailError("hello@"))
    }

    @Test
    fun strongPasswordPasses() {
        assertNull(Validators.passwordError("Lunara2026"))
    }

    @Test
    fun passwordNeedsLettersAndNumbers() {
        assertEquals("Add at least one letter and one number", Validators.passwordError("abcdefgh"))
    }

    @Test
    fun usernameIsNormalized() {
        assertEquals("mohnish_raj", Validators.normalizeUsername(" @Mohnish_Raj "))
    }

    @Test
    fun usernameRejectsSpaces() {
        assertEquals(
            "Use lowercase letters, numbers or underscores",
            Validators.usernameError("hello world"),
        )
    }
}
