package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

class JwtAnonymous(private val issuer: String) : DecodedJWT {
    override fun getIssuer(): String = issuer

    override fun getSubject(): String = ""

    override fun getAudience(): MutableList<String> = mutableListOf()

    override fun getExpiresAt(): Date? = null

    override fun getNotBefore(): Date? = null

    override fun getIssuedAt(): Date? = null

    override fun getId(): String = "anonymous"

    override fun getClaim(name: String?): Claim = JwtNullClaim

    override fun getClaims(): MutableMap<String, Claim> = mutableMapOf()

    override fun getAlgorithm(): String? = null

    override fun getType(): String? = null

    override fun getContentType(): String? = null

    override fun getKeyId(): String? = null

    override fun getHeaderClaim(name: String?): Claim = JwtNullClaim

    override fun getToken(): String? = null

    override fun getHeader(): String? = null

    override fun getPayload(): String? = null

    override fun getSignature(): String? = null
}
