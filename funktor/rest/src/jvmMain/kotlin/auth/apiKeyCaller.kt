package io.peekandpoke.funktor.rest.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*

/**
 * Installs a Ktor bearer auth provider that authenticates an API key into a [Caller.ApiKeyCaller].
 *
 * The [resolve] block receives the raw bearer token and returns the resolved caller, or null to
 * reject the credential (the request can then still fall through to [anonymous]). It runs with
 * [ApplicationCall] as receiver so the consumer can reach the per-call kontainer (or any other
 * call-scoped state) to look the key up:
 *
 * ```kotlin
 * apiKeyCaller(AUTH_API_KEY, realm = "CTB") { token ->
 *     kontainer.get<ApiKeyService>().tryResolve(token)
 * }
 * ```
 *
 * **Authorization-header overlap with JWT.** This provider reads `Authorization: Bearer <token>`,
 * the same header [jwtCaller] uses. When both are installed, every JWT-bearing request also
 * runs through [resolve] (and vice-versa). To keep the resolver fast and unambiguous, give API
 * keys a distinctive non-JWT prefix (e.g. `pk_live_...`) and reject any token that does not
 * carry it without consulting the keystore.
 *
 * **Resolver responsibilities (security-critical):**
 * - Look the key up in an authoritative store; never trust caller-supplied [Caller.ApiKeyCaller]
 *   fields like `permissions`.
 * - Compare the secret half of the token in **constant time** (e.g.
 *   `MessageDigest.isEqual` over the hashes — never `String.equals`).
 * - Store keys hashed (Argon2/bcrypt or HMAC), never plaintext.
 * - Return null for empty / malformed / wrong-prefix tokens before any DB lookup.
 * - Do not log the raw token.
 */
fun AuthenticationConfig.apiKeyCaller(
    name: String,
    realm: String? = null,
    resolve: suspend ApplicationCall.(token: String) -> Caller.ApiKeyCaller?,
) {
    bearer(name) {
        if (realm != null) this.realm = realm
        authenticate { credential -> resolve(credential.token) }
    }
}
