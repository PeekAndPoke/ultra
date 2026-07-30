package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResendActivationRequest(
    val provider: String,
    /**
     * The `resendToken` from [AuthSignInResponse.ActivationRequired].
     *
     * Deliberately NOT an email address. An email-keyed resend is reachable by anyone who can guess or
     * harvest an address, which makes it an anonymous, indefinitely repeatable mail primitive aimed at
     * a mailbox the caller does not own — see `.claude/tasks/20260727-signup-mail-throttle.md`.
     */
    val token: String,
)
