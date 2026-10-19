package io.peekandpoke.funktor.auth.model

import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.Serializable

/**
 * Whether a mail actually went out.
 *
 * Honest rather than neutral, and that is safe BECAUSE the request is authorized by a single-use
 * token: the caller already passed the password check for this account, so the answer tells them
 * nothing they did not put in.
 *
 * - `sent = true` — a new link is on its way.
 * - `sent = false` with [nextResendAt] — too soon: the account's growing resend delay has not elapsed.
 * - `sent = false` without it — not possible (the token was unknown or used, the account is activated
 *   or expired) or the mail could not be sent.
 */
@Serializable
data class AuthResendActivationResponse(
    val sent: Boolean,
    /** When the next resend becomes possible: set when [sent], and when refused as too soon — never otherwise. */
    val nextResendAt: MpInstant? = null,
)
