package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * Whether a mail actually went out.
 *
 * Honest rather than neutral, and that is safe BECAUSE the request is authorized by a single-use
 * token: the caller already passed the password check for this account, so `sent` tells them nothing
 * they did not put in. An email-keyed endpoint would have to lie here to avoid being an enumeration
 * oracle — and lying is what made the first cut tell users "a new link is on its way" when the
 * cooldown had silently suppressed it.
 *
 * `sent = false` means: the token was unknown or used, the account is already activated, or the
 * cooldown window is still open.
 */
@Serializable
data class AuthResendActivationResponse(
    val sent: Boolean,
)
