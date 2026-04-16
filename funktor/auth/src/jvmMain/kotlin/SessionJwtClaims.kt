package io.peekandpoke.funktor.auth

import com.auth0.jwt.JWTCreator
import com.auth0.jwt.interfaces.Payload

/**
 * Claim key used to embed a DB-backed session id in a JWT issued by funktor/auth.
 *
 * The auth middleware reads this claim and resolves it against [SessionStore] on every request
 * (cached), so revoking the session row logs the holder out regardless of the JWT's expiry.
 *
 * Prefixed with the library name to avoid collisions with other claims a caller might add.
 */
const val SESSION_ID_CLAIM: String = "funktor:sid"

/** Attach a funktor session id to the JWT being built. */
fun JWTCreator.Builder.withSessionId(sessionId: String): JWTCreator.Builder =
    withClaim(SESSION_ID_CLAIM, sessionId)

/** Reads the funktor session id from a verified JWT payload, or null if the claim is absent. */
fun Payload.sessionIdClaim(): String? = getClaim(SESSION_ID_CLAIM).asString()
