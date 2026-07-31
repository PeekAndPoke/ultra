package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Payload
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

/** Creates, signs, and verifies JWTs encoding user data and permissions. */
class JwtGenerator(
    /** The configuration */
    internal val config: JwtConfig,
    /** Signing algorithm to be used */
    private val signingAlgorithm: Algorithm = Algorithm.HMAC512(config.signingKey.value),
) {
    /** The namespace for permissions claims */
    val permissionsNs: String get() = config.permissionsNs

    /** The namespace for user data claims */
    val userNs: String get() = config.userNs

    /** Verifier configured with the issuer and audience from [config]. */
    val verifier: JWTVerifier = JWT
        .require(signingAlgorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    /**
     * Authenticates [token] BEFORE any of it is parsed — see [JwtSignatureGate] for why that matters
     * and why RFC 7515 permits it.
     */
    private val gate = JwtSignatureGate(config.signingKey.value)

    /** Verifies the given [token] and returns the decoded payload. */
    fun verify(token: String): Payload {
        // Authenticate the raw bytes first. A token that fails here is rejected having parsed nothing;
        // only afterwards does the library decode the header and payload JSON.
        gate.check(token)

        return verifier.verify(token)
    }

    /** Verifies the given [token]; returns the decoded payload, or null if verification fails. */
    fun tryVerify(token: String): Payload? = try {
        verify(token)
    } catch (_: JWTVerificationException) {
        null
    }

    /** Creates a signed JWT string for the given [user] and [permissions]. */
    fun createJwt(
        user: JwtUserData,
        permissions: UserPermissions = UserPermissions(),
        builder: JWTCreator.Builder.() -> Unit = {},
    ): String = JWT.create()
        // overridable properties
        .expiresInMinutes(60)
        .apply(builder)
        // properties that cannot be overridden but the builder
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(user.id.value)
        .encodeUser(config.userNs, user)
        .encodePermissions(config.permissionsNs, permissions)
        .sign(signingAlgorithm)

    /** Extracts [JwtUserData] from the given JWT [payload]. */
    fun extractUserData(payload: Payload): JwtUserData {
        return payload.extractUser(config.userNs)
    }

    /** Extracts [UserPermissions] from the given JWT [payload]. */
    fun extractPermissions(payload: Payload): UserPermissions {
        return payload.extractPermissions(config.permissionsNs)
    }

    /**
     * Extracts a full [User] from the given [jwt] payload, attaching the [clientIp].
     *
     * The anonymous subject NEVER carries permissions. That sentinel is what an absent or malformed
     * id claim degrades to (see [io.peekandpoke.ultra.security.jwt.extractUser]), and identity and
     * permissions are read from independent claim sets — so keeping the token's permissions here
     * would let a token bearing permission claims but no usable id still satisfy every
     * permission-only auth rule. Degrade both together, and return a real
     * [UserRecord.Anonymous] rather than a [UserRecord.LoggedIn] that merely reports
     * `isAnonymous() == true`.
     */
    fun extractUser(clientIp: String, jwt: Payload): User {
        val data = extractUserData(jwt)

        if (data.id == UserRecord.ANONYMOUS_ID) {
            return User(
                record = UserRecord.Anonymous(clientIp = clientIp),
                permissions = UserPermissions.anonymous,
            )
        }

        return User(
            record = data.toUserRecord(clientIp),
            permissions = extractPermissions(jwt),
        )
    }
}
