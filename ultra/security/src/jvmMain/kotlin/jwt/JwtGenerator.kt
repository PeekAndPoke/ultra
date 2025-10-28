package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Payload
import de.peekandpoke.ultra.security.user.User
import de.peekandpoke.ultra.security.user.UserPermissions

class JwtGenerator(
    /** The configuration */
    val config: JwtConfig,
    /** Signing algorithm to be used */
    private val signingAlgorithm: Algorithm = Algorithm.HMAC512(config.signingKey),
) {
    val verifier: JWTVerifier = JWT
        .require(signingAlgorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

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

    fun extractUserData(payload: Payload): JwtUserData {
        return payload.extractUser(config.userNs)
    }

    fun extractPermissions(payload: Payload): UserPermissions {
        return payload.extractPermissions(config.permissionsNs)
    }

    fun extractUser(clientIp: String, jwt: Payload): User {
        return User(
            record = extractUserData(jwt).toUserRecord(clientIp),
            permissions = extractPermissions(jwt),
        )
    }
}
