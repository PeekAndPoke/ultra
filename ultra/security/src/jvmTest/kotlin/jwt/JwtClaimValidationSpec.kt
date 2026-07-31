package io.peekandpoke.ultra.security.jwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.peekandpoke.ultra.common.model.Redacted
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

/**
 * Pins the registered-claim validation to the contract MEASURED against `java-jwt` 4.5.2 with a
 * fixed clock (2026-07-31), immediately before that library was removed. Every rule here is a probe
 * result, not an assumption, and the boundary rows are exact because the clock is fixed.
 *
 * Every rejection also asserts the failure is NOT a gate rejection — the token got past the MAC, so
 * the claim check did the rejecting. Removing any single check in `validateClaims` fails its rows.
 */
class JwtClaimValidationSpec : StringSpec({

    val now = 1_800_000_000L

    val config = JwtConfig(
        signingKey = Redacted("test-signing-key-rfc7518-requires-sixty-four-bytes-minimum!!!!!!!"),
        issuer = "iss",
        audience = "aud",
        permissionsNs = "permissions",
        userNs = "user",
    )

    val generator = JwtGenerator(
        config = config,
        clock = Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC),
    )

    fun token(vararg claims: Pair<String, JsonElement>): String = generator.sign(mapOf(*claims))

    fun base(vararg extra: Pair<String, JsonElement>): String = token(
        "iss" to JsonPrimitive("iss"),
        "aud" to JsonPrimitive("aud"),
        *extra,
    )

    fun accepted(clue: String, token: String) {
        withClue(clue) { generator.verify(token) }
    }

    fun rejectedByClaims(clue: String, token: String) {
        withClue(clue) {
            shouldThrow<JwtVerificationException> { generator.verify(token) }
                .shouldNotBeInstanceOf<JwtSignatureGate.Rejected>()
        }
    }

    // ── exp ─────────────────────────────────────────────────────────────────────────────────────────

    "exp: absent or JSON null means no expiry check" {
        accepted("absent", base())
        accepted("null", base("exp" to JsonNull))
    }

    "exp: valid strictly before exp — exp == now is already expired, leeway 0" {
        accepted("now+1", base("exp" to JsonPrimitive(now + 1)))
        rejectedByClaims("now", base("exp" to JsonPrimitive(now)))
        rejectedByClaims("now-1", base("exp" to JsonPrimitive(now - 1)))
    }

    "exp: fractional values round down" {
        // floor(now + 0.5) == now -> already expired. java-jwt floored too (measured).
        rejectedByClaims("now+0.5", base("exp" to JsonPrimitive(now + 0.5)))
        accepted("now+1.5", base("exp" to JsonPrimitive(now + 1.5)))
    }

    "exp: non-numeric or out-of-range values are rejected, not skipped" {
        // Skipping would fail open: an unparseable exp would verify forever. java-jwt rejected these
        // as decode errors (measured); we reject them as verification failures.
        rejectedByClaims("string number", base("exp" to JsonPrimitive("${now + 9999}")))
        rejectedByClaims("string", base("exp" to JsonPrimitive("abc")))
        rejectedByClaims("boolean", base("exp" to JsonPrimitive(true)))
        rejectedByClaims("object", base("exp" to buildJsonObject { }))
        rejectedByClaims("array", base("exp" to buildJsonArray { }))
        rejectedByClaims("beyond Long range", base("exp" to JsonPrimitive(1e300)))
    }

    // ── nbf / iat ───────────────────────────────────────────────────────────────────────────────────

    "nbf: valid from that second on — nbf == now is valid" {
        accepted("now", base("nbf" to JsonPrimitive(now)))
        accepted("now-1", base("nbf" to JsonPrimitive(now - 1)))
        accepted("null", base("nbf" to JsonNull))
        rejectedByClaims("now+1", base("nbf" to JsonPrimitive(now + 1)))
        rejectedByClaims("string", base("nbf" to JsonPrimitive("abc")))
    }

    "iat: same rule as nbf — a token issued in the future is rejected" {
        // Our issuer never writes iat, but java-jwt validated it (measured), so accepting a future
        // iat would widen the accepted set.
        accepted("now", base("iat" to JsonPrimitive(now)))
        accepted("now-3600", base("iat" to JsonPrimitive(now - 3600)))
        accepted("null", base("iat" to JsonNull))
        rejectedByClaims("now+1", base("iat" to JsonPrimitive(now + 1)))
        rejectedByClaims("now+3600", base("iat" to JsonPrimitive(now + 3600)))
        rejectedByClaims("string", base("iat" to JsonPrimitive("abc")))
    }

    // ── iss ─────────────────────────────────────────────────────────────────────────────────────────

    "iss: must be a JSON string equal to the configured issuer" {
        accepted("correct", base())
        rejectedByClaims("missing", token("aud" to JsonPrimitive("aud")))
        rejectedByClaims("wrong", token("iss" to JsonPrimitive("other"), "aud" to JsonPrimitive("aud")))
        rejectedByClaims("number", token("iss" to JsonPrimitive(42), "aud" to JsonPrimitive("aud")))
        rejectedByClaims("null", token("iss" to JsonNull, "aud" to JsonPrimitive("aud")))
        rejectedByClaims(
            "array",
            token("iss" to buildJsonArray { add(JsonPrimitive("iss")) }, "aud" to JsonPrimitive("aud")),
        )
    }

    // ── aud ─────────────────────────────────────────────────────────────────────────────────────────

    "aud: a single string equal to the audience, or an array containing it" {
        accepted("string", base())
        accepted(
            "array",
            token("iss" to JsonPrimitive("iss"), "aud" to buildJsonArray { add(JsonPrimitive("aud")) }),
        )
        accepted(
            "array with extras",
            token(
                "iss" to JsonPrimitive("iss"),
                "aud" to buildJsonArray { add(JsonPrimitive("aud")); add(JsonPrimitive("other")) },
            ),
        )
        // Non-string members are IGNORED here. This is a deliberate divergence from java-jwt, not a
        // reproduction of it — see the `validateClaims` KDoc. Measured 2026-07-31 against 4.5.2:
        // the library coerced numbers/booleans to text (so `aud:[42]` matched a required "42") and
        // THREW on an object or array member. An earlier comment here claimed it "ignored" them,
        // generalising from the one row below, which both implementations happen to accept.
        accepted(
            "mixed array containing it",
            token(
                "iss" to JsonPrimitive("iss"),
                "aud" to buildJsonArray { add(JsonPrimitive(42)); add(JsonPrimitive("aud")) },
            ),
        )
        accepted(
            "object member alongside a matching string — java-jwt REJECTED this one",
            token(
                "iss" to JsonPrimitive("iss"),
                "aud" to buildJsonArray { add(buildJsonObject { }); add(JsonPrimitive("aud")) },
            ),
        )
        accepted(
            "array member alongside a matching string — java-jwt REJECTED this one too",
            token(
                "iss" to JsonPrimitive("iss"),
                "aud" to buildJsonArray { add(buildJsonArray { }); add(JsonPrimitive("aud")) },
            ),
        )
    }

    "aud: a number is NOT the string that renders like it — java-jwt coerced, we do not" {
        // The divergence only shows when the configured audience is itself "42": java-jwt ran members
        // through Jackson's treeToValue(..., String.class), so `aud:[42]` matched a required "42"
        // (measured 2026-07-31). Type coercion inside an authorization decision is what we chose to
        // lose. Needs its own generator because the audience must be "42" for the case to exist at all.
        val numericAud = JwtGenerator(
            config = config.copy(audience = "42"),
            clock = Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC),
        )

        val token = numericAud.sign(
            mapOf(
                "iss" to JsonPrimitive("iss"),
                "aud" to buildJsonArray { add(JsonPrimitive(42)) },
            )
        )

        withClue("a JSON number must not satisfy a string audience") {
            shouldThrow<JwtVerificationException> { numericAud.verify(token) }
                .shouldNotBeInstanceOf<JwtSignatureGate.Rejected>()
        }

        withClue("the string \"42\" still matches, so the rejection is about TYPE, not the value") {
            numericAud.verify(
                numericAud.sign(
                    mapOf(
                        "iss" to JsonPrimitive("iss"),
                        "aud" to buildJsonArray { add(JsonPrimitive("42")) },
                    )
                )
            )
        }

        rejectedByClaims("missing", token("iss" to JsonPrimitive("iss")))
        rejectedByClaims("wrong", token("iss" to JsonPrimitive("iss"), "aud" to JsonPrimitive("other")))
        rejectedByClaims("empty array", token("iss" to JsonPrimitive("iss"), "aud" to buildJsonArray { }))
        rejectedByClaims("null", token("iss" to JsonPrimitive("iss"), "aud" to JsonNull))
        rejectedByClaims("number", token("iss" to JsonPrimitive("iss"), "aud" to JsonPrimitive(42)))
    }

    // ── structure of correctly signed tokens ────────────────────────────────────────────────────────

    "structurally broken tokens that nevertheless carry a valid MAC are rejected at the decode step" {
        // Only the key holder can even reach this branch — the MAC runs first — but java-jwt rejected
        // these shapes (measured) and degrading them to an empty claim set would be less legible.
        val gate = JwtSignatureGate(config.signingKey.value)
        val b64 = Base64.getUrlEncoder().withoutPadding()

        fun craft(signingInput: String): String = "$signingInput.${b64.encodeToString(gate.mac(signingInput))}"

        val header = b64.encodeToString("""{"alg":"HS512","typ":"JWT"}""".toByteArray())

        listOf(
            craft(header) to "2 segments",
            craft("$header.a.b") to "4 segments",
            craft("$header.${b64.encodeToString("[1,2]".toByteArray())}") to "payload is a JSON array",
            craft("$header.${b64.encodeToString("42".toByteArray())}") to "payload is a JSON number",
            craft("$header.!!!") to "payload segment is not base64url",
        ).forEach { (token, why) -> rejectedByClaims(why, token) }
    }
})
