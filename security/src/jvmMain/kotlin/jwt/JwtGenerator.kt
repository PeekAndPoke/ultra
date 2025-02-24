package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.auth0.jwt.interfaces.Payload
import de.peekandpoke.ultra.security.user.UserPermissions

class JwtGenerator(
    /** The configuration */
    private val config: JwtConfig,
    /** Signing algorithm to be used */
    private val signingAlgorithm: Algorithm = Algorithm.HMAC512(config.singingKey),
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

    fun extractUser(payload: Payload): JwtUserData = payload.extractUser(config.userNs)

    fun extractPermissions(payload: Payload): UserPermissions = payload.extractPermissions(config.permissionsNs)
}
