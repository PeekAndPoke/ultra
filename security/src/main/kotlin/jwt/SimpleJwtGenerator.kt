package de.peekandpoke.ultra.security.jwt

import de.peekandpoke.ultra.common.fromBase64
import io.jsonwebtoken.*
import java.util.*
import javax.crypto.spec.SecretKeySpec

open class SimpleJwtGenerator(
    private val signingAlgorithm: SignatureAlgorithm,
    private val signingKey: String
) : JwtGenerator {

    private val apiKeySecretBytes: ByteArray = signingKey.fromBase64()
    private val key = SecretKeySpec(apiKeySecretBytes, signingAlgorithm.jcaName)

    override fun createToken(builder: JwtBuilder.() -> Unit): String {
        return Jwts.builder()
            .setIssuedAt(Date())
            .apply(builder)
            .signWith(key, signingAlgorithm)
            .compact()
    }

    override fun parseToken(token: String, builder: JwtParserBuilder.() -> Unit): Jws<Claims> {
        return Jwts.parserBuilder()
            .apply(builder)
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
    }
}
