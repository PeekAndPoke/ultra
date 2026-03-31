package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Payload
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions

/** Creates, signs, and verifies JWTs encoding user data and permissions. */
class JwtGenerator(
    /** The configuration */
    internal val config: JwtConfig,
    /** Signing algorithm to be used */
    private val signingAlgorithm: Algorithm = Algorithm.HMAC512(config.signingKey),
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

    /** Verifies the given [token] and returns the decoded payload. */
    fun verify(token: String): Payload = verifier.verify(token)

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
        .withSubject(user.id)
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

    /** Extracts a full [User] from the given [jwt] payload, attaching the [clientIp]. */
    fun extractUser(clientIp: String, jwt: Payload): User {
        return User(
            record = extractUserData(jwt).toUserRecord(clientIp),
            permissions = extractPermissions(jwt),
        )
    }
}
