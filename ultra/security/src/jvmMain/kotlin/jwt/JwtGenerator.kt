package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Base64
import kotlin.math.floor

/** Creates, signs, and verifies JWTs encoding user data and permissions. */
class JwtGenerator(
    /** The configuration */
    internal val config: JwtConfig,
    /** The clock `exp`/`nbf`/`iat` are validated against. Injectable so expiry tests need no sleeping. */
    private val clock: Clock = Clock.systemUTC(),
) {
    companion object {
        /**
         * The only header this issuer writes, pre-encoded. Byte-identical to the header `java-jwt`
         * 4.5.2 emitted for HMAC512 (measured 2026-07-31), so nothing reading our tokens can see the
         * library swap.
         */
        private val ENCODED_HEADER: String =
            base64Url("""{"alg":"HS512","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))

        private fun base64Url(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** The namespace for permissions claims */
    val permissionsNs: String get() = config.permissionsNs

    /** The namespace for user data claims */
    val userNs: String get() = config.userNs

    /**
     * Authenticates and signs tokens — one object on purpose: [JwtSignatureGate.mac] is both the
     * verify-side and the sign-side primitive, so the two cannot disagree on key bytes or charset.
     */
    private val gate = JwtSignatureGate(config.signingKey.value)

    /** Verifies the given [token] and returns its claims, or throws [JwtVerificationException]. */
    fun verify(token: String): JwtPayload {
        // Authenticate the raw bytes first. A token that fails here is rejected having parsed
        // nothing — see JwtSignatureGate for why that matters and why RFC 7515 permits it.
        gate.check(token)

        // Everything below runs on authenticated bytes only.
        val claims = decodeClaims(token)

        validateClaims(claims)

        return JwtPayload(claims = claims)
    }

    /** Verifies the given [token]; returns its claims, or null if verification fails. */
    fun tryVerify(token: String): JwtPayload? = try {
        verify(token)
    } catch (_: JwtVerificationException) {
        null
    }

    /**
     * Parses the claim set out of a token whose MAC has already checked out.
     *
     * Failures here are only producible by the signing-key holder — the MAC ran first — but they
     * still reject rather than degrade: `java-jwt` also rejected structurally broken tokens, and an
     * empty claim set would merely fail issuer validation less legibly.
     */
    private fun decodeClaims(token: String): JsonObject {
        val segments = token.split('.')

        if (segments.size != 3) {
            throw JwtVerificationException("Token is not a well-formed JWS compact serialization")
        }

        return try {
            Json.parseToJsonElement(
                String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8)
            ).jsonObject
        } catch (_: IllegalArgumentException) {
            throw JwtVerificationException("Token payload is not a JSON object")
        } catch (_: SerializationException) {
            throw JwtVerificationException("Token payload is not a JSON object")
        }
    }

    /**
     * Validates the registered claims exactly as `java-jwt` 4.5.2 did — measured with a fixed clock
     * immediately before that library was removed, not assumed (see `JwtClaimValidationSpec`):
     *
     * - `exp`: absent or JSON null means no expiry check; otherwise the token is valid strictly
     *   before `exp` (`exp == now` is already expired). No leeway.
     * - `nbf` and `iat`: absent or null means no check; otherwise valid from that second on
     *   (`== now` is valid).
     * - `iss`: must be a JSON string equal to the configured issuer.
     * - `aud`: a single string equal to the configured audience, or an array containing it —
     *   non-string array members are ignored, as the library ignored them.
     */
    private fun validateClaims(claims: JsonObject) {
        val now = clock.instant().epochSecond

        numericDate(claims[JwtPayload.EXPIRES_AT], JwtPayload.EXPIRES_AT)?.let {
            if (it <= now) throw JwtVerificationException("The token has expired")
        }

        numericDate(claims[JwtPayload.NOT_BEFORE], JwtPayload.NOT_BEFORE)?.let {
            if (it > now) throw JwtVerificationException("The token cannot be used yet")
        }

        numericDate(claims[JwtPayload.ISSUED_AT], JwtPayload.ISSUED_AT)?.let {
            if (it > now) throw JwtVerificationException("The token is issued in the future")
        }

        val issuer = (claims[JwtPayload.ISSUER] as? JsonPrimitive)?.takeIf { it.isString }?.content

        if (issuer != config.issuer) {
            throw JwtVerificationException("The token issuer is not accepted")
        }

        val audienceOk = when (val aud = claims[JwtPayload.AUDIENCE]) {
            is JsonPrimitive -> aud.isString && aud.content == config.audience
            is JsonArray -> aud.any { it is JsonPrimitive && it.isString && it.content == config.audience }
            else -> false
        }

        if (!audienceOk) {
            throw JwtVerificationException("The token audience is not accepted")
        }
    }

    /**
     * Reads a NumericDate claim: null when the claim is absent or JSON null (it is then not
     * validated, matching the library), epoch seconds rounded down otherwise. Non-numeric or
     * beyond-Long-range values are rejected — also matching the library, which raised a decode
     * error for those rather than skipping the check.
     */
    private fun numericDate(value: JsonElement?, name: String): Long? {
        if (value == null || value is JsonNull) return null

        val primitive = (value as? JsonPrimitive)?.takeIf { !it.isString }
            ?: throw JwtVerificationException("The claim '$name' is not a valid NumericDate")

        return primitive.longOrNull
            ?: primitive.doubleOrNull
                ?.takeIf { it.isFinite() && it >= Long.MIN_VALUE.toDouble() && it <= Long.MAX_VALUE.toDouble() }
                ?.let { floor(it).toLong() }
            ?: throw JwtVerificationException("The claim '$name' is not a valid NumericDate")
    }

    /** Creates a signed JWT string for the given [user] and [permissions]. */
    fun createJwt(
        user: JwtUserData,
        permissions: UserPermissions = UserPermissions(),
        builder: JwtBuilder.() -> Unit = {},
    ): String = JwtBuilder()
        // overridable properties
        .expiresInMinutes(60)
        .apply(builder)
        // properties that cannot be overridden by the builder
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(user.id.value)
        .encodeUser(config.userNs, user)
        .encodePermissions(config.permissionsNs, permissions)
        .let { sign(it.claims) }

    /**
     * Signs a claim set into a JWS compact token.
     *
     * The MAC comes from [JwtSignatureGate.mac] — the very method [verify] checks with — so issuer
     * and verifier cannot drift apart. Internal so tests can mint shapes [createJwt] refuses to
     * (a token without a subject, say); production issuance stays funnelled through [createJwt].
     */
    internal fun sign(claims: Map<String, JsonElement>): String {
        val payload = Json.encodeToString(JsonObject.serializer(), JsonObject(claims))
        val signingInput = "$ENCODED_HEADER.${base64Url(payload.toByteArray(StandardCharsets.UTF_8))}"

        return "$signingInput.${base64Url(gate.mac(signingInput))}"
    }

    /** Extracts [JwtUserData] from the given JWT [payload]. */
    fun extractUserData(payload: JwtPayload): JwtUserData {
        return payload.extractUser(config.userNs)
    }

    /** Extracts [UserPermissions] from the given JWT [payload]. */
    fun extractPermissions(payload: JwtPayload): UserPermissions {
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
    fun extractUser(clientIp: String, jwt: JwtPayload): User {
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
