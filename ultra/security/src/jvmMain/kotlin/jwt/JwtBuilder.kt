package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWTCreator
import java.time.Instant
import java.util.Date

/**
 * Collects the claims of a JWT being issued.
 *
 * The counterpart to [JwtPayload]: that one reads a verified token, this one writes a new one. Both
 * exist so the signing library stays an internal detail of this module rather than a type in the
 * framework's published API — `JwtGenerator.createJwt` takes `JwtBuilder.() -> Unit`, so an
 * application customising a token is not coupled to whatever signs it.
 *
 * Deliberately a thin pass-through rather than an independent claim model. It exposes exactly the
 * operations the codebase uses, so there is no behaviour of its own that could drift from the signer's.
 *
 * **JVM-only**, unlike [JwtPayload]: issuing a token requires signing, whereas reading a verified one
 * does not. That asymmetry is why a client can share the payload type but not this.
 */
class JwtBuilder internal constructor(
    @PublishedApi internal val delegate: JWTCreator.Builder,
) {
    /** Sets a string claim. */
    fun withClaim(name: String, value: String?): JwtBuilder = apply { delegate.withClaim(name, value) }

    /** Sets a boolean claim. */
    fun withClaim(name: String, value: Boolean?): JwtBuilder = apply { delegate.withClaim(name, value) }

    /** Sets a claim holding an array of strings. */
    fun withArrayClaim(name: String, values: Array<String>): JwtBuilder =
        apply { delegate.withArrayClaim(name, values) }

    /** Sets the `sub` claim. */
    fun withSubject(subject: String?): JwtBuilder = apply { delegate.withSubject(subject) }

    /** Sets the `iss` claim. */
    fun withIssuer(issuer: String?): JwtBuilder = apply { delegate.withIssuer(issuer) }

    /** Sets the `aud` claim. */
    fun withAudience(vararg audience: String): JwtBuilder = apply { delegate.withAudience(*audience) }

    /** Sets the `exp` claim. */
    fun withExpiresAt(expiresAt: Date): JwtBuilder = apply { delegate.withExpiresAt(expiresAt) }

    /** Sets the `exp` claim. Both overloads exist because callers use both. */
    fun withExpiresAt(expiresAt: Instant): JwtBuilder = apply { delegate.withExpiresAt(expiresAt) }
}
