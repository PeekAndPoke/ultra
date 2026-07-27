package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * Deliberately carries NOTHING.
 *
 * The endpoint is anonymous, so the answer must be identical whether the address has an account, has
 * an ALREADY-ACTIVATED account, or was asked for again inside the cooldown window. Anything else is
 * an account-enumeration oracle. Same reasoning as [AuthRecoverAccountResponse.InitPasswordReset].
 */
@Serializable
data object AuthResendActivationResponse
