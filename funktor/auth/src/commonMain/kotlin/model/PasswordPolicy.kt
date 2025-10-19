package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordPolicy(
    val regex: String,
    val description: String,
) {
    companion object {
        const val DEFAULT_PASSWORD_POLICY =
            """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$"""

        const val DEFAULT_PASSWORD_POLICY_DESCRIPTION =
            "Must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"

        val default = PasswordPolicy(DEFAULT_PASSWORD_POLICY, DEFAULT_PASSWORD_POLICY_DESCRIPTION)
    }

    private val rgx by lazy { regex.toRegex() }

    fun matches(password: String): Boolean = password.matches(rgx)
}
