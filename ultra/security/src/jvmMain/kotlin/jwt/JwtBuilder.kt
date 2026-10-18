package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.util.Date

/**
 * Collects the claims of a JWT being issued.
 *
 * The counterpart to [JwtPayload]: that one reads a verified token, this one writes a new one. It
 * exposes exactly the operations the codebase uses; [JwtGenerator.createJwt] signs the collected
 * claims into a token.
 *
 * Wire shapes match what `java-jwt` 4.5.2 produced, measured before it was removed: a single
 * audience is a plain string (an array only for several), date claims are epoch seconds rounded
 * down, and a null value writes a JSON null rather than dropping the claim.
 *
 * **JVM-only**, unlike [JwtPayload]: issuing a token requires signing, whereas reading a verified one
 * does not. That asymmetry is why a client can share the payload type but not this.
 */
class JwtBuilder internal constructor() {

    /** The collected claims, in insertion order. Read by [JwtGenerator.sign]. */
    internal val claims: MutableMap<String, JsonElement> = linkedMapOf()

    /** Sets a string claim. A null [value] is written as a JSON null. */
    fun withClaim(name: String, value: String?): JwtBuilder = apply {
        claims[name] = value?.let(::JsonPrimitive) ?: JsonNull
    }

    /** Sets a boolean claim. A null [value] is written as a JSON null. */
    fun withClaim(name: String, value: Boolean?): JwtBuilder = apply {
        claims[name] = value?.let(::JsonPrimitive) ?: JsonNull
    }

    /** Sets a claim holding an array of strings. */
    fun withArrayClaim(name: String, values: Array<String>): JwtBuilder = apply {
        claims[name] = JsonArray(values.map(::JsonPrimitive))
    }

    /** Sets the `sub` claim. */
    fun withSubject(subject: String?): JwtBuilder = withClaim(JwtPayload.SUBJECT, subject)

    /** Sets the `iss` claim. */
    fun withIssuer(issuer: String?): JwtBuilder = withClaim(JwtPayload.ISSUER, issuer)

    /** Sets the `aud` claim — a plain string for a single audience, an array for several. */
    fun withAudience(vararg audience: String): JwtBuilder = apply {
        claims[JwtPayload.AUDIENCE] = when (audience.size) {
            1 -> JsonPrimitive(audience[0])
            else -> JsonArray(audience.map(::JsonPrimitive))
        }
    }

    /** Sets the `exp` claim, as epoch seconds rounded down. */
    fun withExpiresAt(expiresAt: Date): JwtBuilder = withExpiresAt(expiresAt.toInstant())

    /** Sets the `exp` claim, as epoch seconds rounded down. Both overloads exist because callers use both. */
    fun withExpiresAt(expiresAt: Instant): JwtBuilder = apply {
        claims[JwtPayload.EXPIRES_AT] = JsonPrimitive(expiresAt.epochSecond)
    }
}
