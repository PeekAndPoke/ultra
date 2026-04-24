package io.peekandpoke.funktor.rest.auth

import com.auth0.jwt.interfaces.Payload
import io.peekandpoke.ultra.security.user.UserPermissions

/**
 * Principal produced by funktor's authentication providers.
 *
 * One variant per supported auth mechanism. Code that needs to know "how was this request
 * authenticated" pattern-matches on this; code that only needs identity goes through
 * [io.peekandpoke.ultra.security.user.UserRecord] (built via [currentUserProvider]).
 *
 * Caller is intentionally *not* `@Serializable` and is JVM-only — it is request-scoped state
 * that must never leave the server boundary. Persist [io.peekandpoke.ultra.security.user.UserRecord]
 * for audit/insights, never the [Caller] itself.
 */
sealed interface Caller {

    /**
     * End-user authenticated via a JWT (the auth0 [Payload] is kept request-scoped only).
     *
     * The payload may carry sensitive claims; do not log it or persist it. Identity for storage
     * goes through [io.peekandpoke.ultra.security.user.UserRecord.LoggedIn] via
     * [currentUserProvider].
     */
    data class JwtCaller(val payload: Payload) : Caller

    /**
     * Caller authenticated via an API key.
     *
     * Carries only **non-secret** key metadata — the raw key value is verified inside the
     * [apiKeyCaller] resolver and discarded. [permissions] determines what the key may do; a
     * misconfigured resolver that returns [UserPermissions.system] grants full superuser
     * privileges, so resolvers must derive permissions from an authoritative store, never from
     * client input.
     */
    data class ApiKeyCaller(
        val keyId: String,
        val keyName: String? = null,
        val userId: String,
        val email: String? = null,
        val desc: String? = null,
        val type: String? = null,
        val permissions: UserPermissions = UserPermissions.anonymous,
    ) : Caller

    /** Terminal fallback used when no auth header is present (or all providers declined). */
    data object AnonymousCaller : Caller
}
