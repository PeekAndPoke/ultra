package io.peekandpoke.funktor.rest.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.peekandpoke.funktor.core.jwtGenerator

/**
 * Installs an auth provider that authenticates a bearer token into a [Caller.JwtCaller].
 *
 * Defaults to [tryJwtCaller], which verifies the token using the kontainer-bound
 * [io.peekandpoke.ultra.security.jwt.JwtGenerator]. That covers the standard case:
 *
 * ```kotlin
 * authentication {
 *     jwtCaller(AUTH_JWT, realm = "Funktor Demo")
 *     anonymous(AUTH_ANON)
 * }
 * ```
 *
 * Pass a custom [validate] lambda when you need extra checks (revocation list, multiple verifiers,
 * audience switching). It runs with [ApplicationCall] as receiver so the per-call kontainer is in
 * scope. Return null to reject the credential; the request then still falls through to [anonymous].
 *
 * ```kotlin
 * jwtCaller(AUTH_JWT) { token ->
 *     val payload = jwtGenerator.tryVerify(token) ?: return@jwtCaller null
 *     if (kontainer.get<RevocationList>().isRevoked(payload.subject)) return@jwtCaller null
 *     Caller.JwtCaller(payload)
 * }
 * ```
 *
 * **Authorization-header overlap with API keys.** This provider reads `Authorization: Bearer <token>`,
 * the same header [apiKeyCaller] uses. When both are installed every request runs through both
 * lambdas (whichever returns non-null first wins under Ktor's `FirstSuccessful` strategy). Keep
 * each lambda fast: reject obviously-not-mine tokens before doing any verify or DB lookup.
 *
 * **Validate-block responsibilities (security-critical):**
 * - Verify the JWT signature, issuer, audience, and expiry; never trust an unverified payload.
 * - Catch verification exceptions and return null — exceptions surface as 500s, not 401s.
 * - Do not log the raw token.
 */
fun AuthenticationConfig.jwtCaller(
    name: String,
    realm: String? = null,
    validate: suspend ApplicationCall.(token: String) -> Caller.JwtCaller? = { token -> tryJwtCaller(token) },
) {
    bearer(name) {
        if (realm != null) this.realm = realm
        authenticate { credential -> validate(credential.token) }
    }
}

/**
 * Convenience: verify [token] using the kontainer-bound `JwtGenerator` and wrap the result in a
 * [Caller.JwtCaller]. Returns null if verification fails (signature, issuer, audience, or expiry).
 */
fun ApplicationCall.tryJwtCaller(token: String): Caller.JwtCaller? =
    jwtGenerator.tryVerify(token)?.let { Caller.JwtCaller(it) }
