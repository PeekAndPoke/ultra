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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
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
        private fun base64Url(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        /** Default token lifetime, applied by [createJwt] unless the builder overrides `exp`. */
        const val DEFAULT_EXPIRY_MINUTES: Long = 60
    }

    /** The namespace for permissions claims */
    val permissionsNs: String get() = config.permissionsNs

    /** The namespace for user data claims */
    val userNs: String get() = config.userNs

    /**
     * Authenticates and signs tokens — one object on purpose: [JwtSignatureGate.mac] is both the
     * verify-side and the sign-side primitive, so the two cannot disagree on key bytes, algorithm or
     * charset. It also owns the key map, so `kid` selection has a single implementation.
     */
    private val gate = JwtSignatureGate(config.keys)

    /** The key this generator signs with: the first in [JwtConfig.keys]. Internal — it holds the secret. */
    internal val signingKey: JwtSigningKey get() = gate.signingKey

    /**
     * The header every token from this generator carries, pre-encoded.
     *
     * Built with kotlinx rather than string concatenation so that [JwtSigningKey.id] — a configured
     * value, but one that lands verbatim in signed output — cannot break out of the JSON.
     *
     * Member order is `kid, alg, typ` because that is what `java-jwt` 4.5.2 emitted (measured
     * 2026-07-31, with `withKeyId`). JSON object order carries no meaning, and nothing depends on
     * this — it buys one thing: `JwtWireCompatSpec` can assert our tokens are **byte-identical** to
     * that library's for the same inputs, which checks the encoder as well as the MAC.
     *
     * Internal so specs can craft tokens whose header this issuer would never write.
     */
    internal val encodedHeader: String = base64Url(
        Json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("kid", JsonPrimitive(gate.signingKey.id))
                put("alg", JsonPrimitive(gate.signingKey.alg.headerValue))
                put("typ", JsonPrimitive("JWT"))
            },
        ).toByteArray(StandardCharsets.UTF_8)
    )

    /** Verifies the given [token] and returns its claims, or throws [JwtVerificationException]. */
    fun verify(token: String): JwtPayload {
        // Authenticate first. A token that fails here is rejected with its PAYLOAD unparsed — the gate
        // reads only a length-capped header, to select the key. See JwtSignatureGate for the detail and
        // for why RFC 7515 permits the reordering.
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
     *
     * The segment-count guard is now unreachable: [JwtSignatureGate.check] pins the token at exactly
     * three non-empty segments before this runs. Kept as a local invariant, so that indexing
     * `segments[1]` cannot become an `IndexOutOfBoundsException` — i.e. a 500 — if the gate's shape
     * check is ever loosened.
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
     * - `aud`: a single string equal to the configured audience, or an array containing it.
     *
     * **`aud` array members are the one deliberate divergence, and the earlier claim here that the
     * library "ignored" non-string members was wrong** — corrected 2026-07-31 after measuring the cases
     * the original probe had not covered. `java-jwt` ran every member through Jackson's
     * `treeToValue(..., String.class)`, so it *coerced* numbers and booleans to their text (`aud: [42]`
     * genuinely matched a configured audience of `"42"`) and *threw* on an object or array member,
     * rejecting the whole token. This implementation instead ignores any non-string member.
     *
     * Kept as a divergence rather than reproduced, because type coercion inside an authorization
     * decision is a defect worth losing: `42` is not the audience `"42"`. Both differences are
     * unreachable anyway — the MAC runs first, and this issuer only ever writes a single string `aud`.
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
        // Overridable. The default expiry is derived from THIS generator's [clock], not from
        // `expiresInMinutes`' `Date()`: otherwise the clock seam is half-wired — `verify` honours the
        // injected clock while issuance ignores it, so a generator built with a fixed clock mints
        // tokens it immediately considers expired. Identical in production, where both are system time.
        .withExpiresAt(clock.instant().plusSeconds(DEFAULT_EXPIRY_MINUTES * 60))
        .apply(builder)
        // Properties that cannot be overridden by the builder. `encodeUser` and `encodePermissions`
        // CLEAR their namespace first — without that, their writes are conditional, so a claim the
        // builder set survived whenever the corresponding permission was absent. That is the direction
        // that matters: a builder-set `permissions/superuser` would outlive an unprivileged
        // UserPermissions and satisfy every permission-only auth rule.
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
        val signingInput = "$encodedHeader.${base64Url(payload.toByteArray(StandardCharsets.UTF_8))}"

        return "$signingInput.${base64Url(gate.mac(signingInput))}"
    }

    /** The MAC under this generator's signing key. Internal: specs craft tokens the issuer would not. */
    internal fun mac(signingInput: String): ByteArray = gate.mac(signingInput)

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
