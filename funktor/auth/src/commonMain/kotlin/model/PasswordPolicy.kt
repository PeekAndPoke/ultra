package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordPolicy(
    val regex: String,
    val description: String,
) {
    companion object {
        /**
         * The longest password any realm accepts, whatever its [regex] says (ULTRA-00296). Counted in UTF-16 units,
         * as Kotlin's `String.length` is. Far above NIST's "at least 64", and it keeps a request from making the server
         * hash or match megabytes: sign-in and every password change refuse anything longer before hashing it.
         */
        const val MAX_LENGTH = 256

        /** Shown when a password is longer than [MAX_LENGTH]. */
        const val TOO_LONG_DESCRIPTION = "Must be at most $MAX_LENGTH characters long"

        /** True when [password] is longer than [MAX_LENGTH]: refused by every realm. The one place the rule lives. */
        fun isTooLong(password: String): Boolean = password.length > MAX_LENGTH

        const val DEFAULT_PASSWORD_POLICY =
            """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$"""

        const val DEFAULT_PASSWORD_POLICY_DESCRIPTION =
            "Must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"

        val default = PasswordPolicy(DEFAULT_PASSWORD_POLICY, DEFAULT_PASSWORD_POLICY_DESCRIPTION)
    }

    private val rgx by lazy { regex.toRegex() }

    /** True when [password] is at most [MAX_LENGTH] long and matches [regex]; the length is checked first. */
    fun matches(password: String): Boolean = !isTooLong(password) && password.matches(rgx)

    /** [TOO_LONG_DESCRIPTION] for a too-long [password], else this policy's [description]: why it was refused. */
    fun refusalDescription(password: String): String = if (isTooLong(password)) TOO_LONG_DESCRIPTION else description
}
