package com.mohnishraj.lunara.domain

object Validators {
    private val usernamePattern = Regex("^[a-z0-9_]{3,20}$")
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(value: String): String? = when {
        value.isBlank() -> "Enter your email"
        !emailPattern.matches(value.trim()) -> "Enter a valid email"
        else -> null
    }

    fun passwordError(value: String): String? = when {
        value.isBlank() -> "Enter your password"
        value.length < 8 -> "Use at least 8 characters"
        !value.any(Char::isLetter) || !value.any(Char::isDigit) -> "Add at least one letter and one number"
        else -> null
    }

    fun displayNameError(value: String): String? = when {
        value.trim().length < 2 -> "Name must have at least 2 characters"
        value.trim().length > 40 -> "Keep the name under 40 characters"
        else -> null
    }

    fun usernameError(value: String): String? {
        val normalized = normalizeUsername(value)
        return when {
            normalized.length < 3 -> "Use at least 3 characters"
            normalized.length > 20 -> "Keep it under 20 characters"
            !usernamePattern.matches(normalized) -> "Use lowercase letters, numbers or underscores"
            else -> null
        }
    }

    fun normalizeUsername(value: String): String = value.trim().lowercase().removePrefix("@")
}
