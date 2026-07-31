package io.peekandpoke.ultra.security.jwt

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * The claims of a **verified** JWT.
 *
 * Deliberately nothing more than typed access to a [JsonObject]. That is all the previous vendor type
 * was used for — five operations across the whole codebase: [subject], [getClaim], and a claim's
 * [JwtClaim.asString], [JwtClaim.asBoolean] and [JwtClaim.asStringList].
 *
 * ### Why not simply expose the library's type
 *
 * It was reaching every consumer through `Caller.JwtCaller`, so a third-party class sat in this
 * framework's published API and any change of library became a breaking change for applications. It
 * also offered `as(Class)` and `asMap()` — **reflective deserialization into arbitrary types** — which
 * is exactly what should never be pointed at token content.
 *
 * ### Only ever built from verified input
 *
 * Every instance is constructed after the signature has been checked. Nothing here validates a
 * signature, an expiry or an issuer, and nothing here should: by the time a payload exists, that has
 * happened.
 *
 * This is `commonMain` because it needs no crypto and no JVM: a client that only decodes a token to
 * read its claims can use the same type.
 */
data class JwtPayload(
    /** The raw claim set. */
    val claims: JsonObject,
) {
    /** The `sub` claim, or null. */
    val subject: String? get() = getClaim(SUBJECT).asString()

    /** The `iss` claim, or null. Validated by the verifier; exposed so callers can assert on it. */
    val issuer: String? get() = getClaim(ISSUER).asString()

    /**
     * The `aud` claim, always as a list.
     *
     * RFC 7519 §4.1.3 permits either a single string or an array of them, so a caller that assumed one
     * shape would break on the other.
     */
    val audience: List<String>
        get() = getClaim(AUDIENCE).let { it.asStringList() ?: listOfNotNull(it.asString()) }

    companion object {
        const val SUBJECT = "sub"
        const val ISSUER = "iss"
        const val AUDIENCE = "aud"
        const val EXPIRES_AT = "exp"
        const val NOT_BEFORE = "nbf"
        const val ISSUED_AT = "iat"
    }

    /**
     * The claim called [name] — **never null**, absent claims yield an empty [JwtClaim].
     *
     * Matches the vendor type's contract, where a missing claim is a null-valued claim rather than a
     * null reference. Call sites in `extract.kt` rely on it: they read `getClaim(x).asString()` with no
     * safe call, and a nullable return would push `?.` into a dozen places for no gain.
     */
    fun getClaim(name: String): JwtClaim = JwtClaim(claims[name])
}

/**
 * One claim's value, read leniently.
 *
 * **Every accessor degrades to null or empty rather than throwing.** Claims are attacker-supplied: a
 * token whose `superuser` claim is the string `"yes"` instead of a boolean, or whose `roles` is an
 * object instead of an array, must produce "no permission" — not an exception that becomes a 500. This
 * mirrors what `extract.kt` documents about degrading to "no identity" rather than failing loudly.
 */
data class JwtClaim(
    /** The raw value, or null when the claim is absent. */
    val value: JsonElement?,
) {
    /** True when the claim is absent or explicitly null. */
    val isNull: Boolean get() = value == null || value is JsonNull

    /** The value as a string, or null when it is absent, null, or not a JSON string. */
    fun asString(): String? = (value as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.contentOrNull

    /** The value as a boolean, or null when it is absent, null, or not a JSON boolean. */
    fun asBoolean(): Boolean? = (value as? JsonPrimitive)
        ?.takeIf { !it.isString }
        ?.booleanOrNull

    /**
     * The value as a list of strings, or null when it is not a JSON array.
     *
     * Non-string elements are dropped rather than failing the whole list — one malformed entry in a
     * roles array must not cost the caller every other role.
     */
    fun asStringList(): List<String>? = (value as? JsonArray)
        ?.mapNotNull { JwtClaim(it).asString() }
}

/** This claim as a [Set] of strings; empty when absent or not an array of strings. */
fun JwtClaim.asStringSet(): Set<String> = asStringList()?.toSet() ?: emptySet()
