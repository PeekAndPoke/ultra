package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthActivateAccountResponse(
    val success: Boolean,
    /**
     * The session for the account just activated. Best-effort, as after sign-up: null when the realm could not
     * sign the user in (e.g. an org realm and a user with no org yet), while [success] is still true.
     */
    val signIn: AuthSignInResponse? = null,
    /**
     * Why the INPUT was refused, so the page can say so and keep its form; the link is still usable. Null on
     * success, and for every refusal of the link itself (used, unknown, expired), which stay indistinguishable.
     */
    val failure: Failure? = null,
) {
    /** A refusal of what the user typed. Both are checked before the token is looked at, so neither tells anything about it. */
    @Serializable
    enum class Failure {
        /** The password does not meet the realm's password policy. */
        WeakPassword,

        /** The display name does not meet [AuthDisplayName]. */
        InvalidDisplayName,
    }
}
