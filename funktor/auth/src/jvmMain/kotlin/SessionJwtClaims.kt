package io.peekandpoke.funktor.auth

import io.peekandpoke.ultra.security.jwt.JwtBuilder
import io.peekandpoke.ultra.security.jwt.JwtPayload

/**
 * Claim key for a DB-backed session id in a JWT issued by funktor/auth.
 *
 * **NOT WIRED YET.** Nothing issues this claim and nothing reads it: [withSessionId] and
 * [sessionIdClaim] have no callers, and no production code registers a [SessionStore]. The intended
 * end state is that the auth middleware resolves the claim against the store on every request
 * (cached), so revoking the row logs the holder out regardless of the JWT's own expiry.
 *
 * What DOES exist: [SessionStore] with `revoke` / `revokeAllForUser` / `listForUser`, its `Null`,
 * `Vault` and `Cached` implementations, device fields on `AuthRecord.Session`, and TTL-index pruning
 * of expired rows in both DB backends. The missing half is purely the JWT integration —
 * `.claude/tasks/20260728-session-revocation-wiring.md`.
 *
 * Prefixed with the library name to avoid collisions with other claims a caller might add.
 */
const val SESSION_ID_CLAIM: String = "funktor:sid"

/** Attach a funktor session id to the JWT being built. */
fun JwtBuilder.withSessionId(sessionId: String): JwtBuilder =
    withClaim(SESSION_ID_CLAIM, sessionId)

/** Reads the funktor session id from a verified JWT payload, or null if the claim is absent. */
fun JwtPayload.sessionIdClaim(): String? = getClaim(SESSION_ID_CLAIM).asString()
