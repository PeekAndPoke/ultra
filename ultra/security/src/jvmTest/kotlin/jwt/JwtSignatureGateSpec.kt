package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.security.user.UserId
import java.util.Base64

/**
 * The gate that authenticates a token before anything parses it.
 *
 * The tests that matter most are the last two: they are the exact probes that showed `java-jwt` parsing
 * unauthenticated JSON, and they must now fail on the MAC instead. The rest exist so that the gate
 * cannot reject anything the library would have accepted — a gate that is subtly stricter than the
 * issuer breaks every login, and one that is looser is pointless.
 */
class JwtSignatureGateSpec : StringSpec({

    val secret = "test-signing-key-for-the-gate-spec"

    val config = JwtConfig(
        signingKey = Redacted(secret),
        issuer = "test-issuer",
        audience = "test-audience",
        permissionsNs = "permissions",
        userNs = "user",
    )

    val generator = JwtGenerator(config)
    val gate = JwtSignatureGate(secret)

    val b64 = Base64.getUrlEncoder().withoutPadding()

    fun handMadeToken(header: String, payload: String, signature: String = "not-a-valid-signature") =
        "${b64.encodeToString(header.toByteArray())}.${b64.encodeToString(payload.toByteArray())}.$signature"

    /** A genuine token, produced by the very code path that signs in production. */
    fun realToken(): String = generator.createJwt(user = JwtUserData(id = UserId("u1"), desc = "d", type = "t"))

    // ── the property this exists for ────────────────────────────────────────────────────────────────

    "a malformed payload is rejected WITHOUT being parsed" {
        // Before the gate this raised JWTDecodeException — a PARSE error, proving Jackson ran on
        // unauthenticated input. It must now be a gate rejection instead.
        val thrown = shouldThrow<JWTVerificationException> {
            generator.verify(handMadeToken("""{"alg":"HS512","typ":"JWT"}""", """{"sub": {{{ """))
        }

        thrown.shouldBeInstanceOf<JwtSignatureGate.Rejected>()
        withClue("a decode error here would mean the parser ran before the MAC") {
            (thrown is JWTDecodeException) shouldBe false
        }
    }

    "a malformed header is rejected WITHOUT being parsed" {
        val thrown = shouldThrow<JWTVerificationException> {
            generator.verify(handMadeToken("""{"alg": [[[ """, """{"sub":"x"}"""))
        }

        thrown.shouldBeInstanceOf<JwtSignatureGate.Rejected>()
    }

    "an oversized token is rejected before it is hashed, let alone parsed" {
        // Previously this 20 MB string was fully parsed and allocated, and only THEN failed the
        // signature check. Now it does not even reach the MAC.
        val huge = handMadeToken("""{"alg":"HS512","typ":"JWT"}""", """{"s":"""" + "A".repeat(20_000_000) + """"}""")

        shouldThrow<JwtSignatureGate.Rejected> { generator.verify(huge) }
            .message shouldBe "Token exceeds the maximum accepted length"
    }

    "a deeply nested payload never reaches a parser" {
        val nested = handMadeToken("""{"alg":"HS512","typ":"JWT"}""", "[".repeat(10_000) + "]".repeat(10_000))

        shouldThrow<JwtSignatureGate.Rejected> { generator.verify(nested) }
    }

    // ── the gate must not be stricter than the issuer ───────────────────────────────────────────────

    "a genuine token passes the gate and still verifies" {
        // Wire compatibility: the gate must compute the SAME signing input as the library, or every
        // real token breaks. This is the test that catches a re-encoding mistake.
        val token = realToken()

        gate.check(token)

        generator.verify(token).subject shouldBe "u1"
        // tryVerify must agree with verify — compared by a claim, since the decoder has no equals()
        generator.tryVerify(token)?.subject shouldBe "u1"
    }

    "a token signed by the library directly also passes" {
        // Built through auth0's own builder rather than JwtGenerator, so nothing about our own
        // construction can mask a mismatch.
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("u2")
            .sign(Algorithm.HMAC512(secret))

        gate.check(token)
    }

    // ── rejections ──────────────────────────────────────────────────────────────────────────────────

    "a tampered payload is rejected" {
        val real = realToken()
        val parts = real.split(".")
        val tampered = "${parts[0]}.${b64.encodeToString("""{"sub":"admin"}""".toByteArray())}.${parts[2]}"

        shouldThrow<JwtSignatureGate.Rejected> { gate.check(tampered) }
    }

    "a tampered signature is rejected" {
        val real = realToken()
        val lastDot = real.lastIndexOf('.')

        // Flip a character in the MIDDLE of the signature, not the last one. A 64-byte HMAC-SHA512
        // encodes to 86 base64url characters = 516 bits, so the FINAL character carries 4 unused bits:
        // changing it can leave the decoded signature bytes identical, and the token still verifies —
        // correctly, since the bytes are what authenticate. An earlier version of this test flipped the
        // last character and failed for exactly that reason.
        val at = lastDot + 1 + (real.length - lastDot - 1) / 2
        val flipped = real.replaceRange(at, at + 1, if (real[at] == 'A') "B" else "A")

        withClue("flipping a middle signature character must change the decoded bytes") {
            (flipped != real) shouldBe true
        }

        shouldThrow<JwtSignatureGate.Rejected> { gate.check(flipped) }
    }

    "the wrong key is rejected" {
        shouldThrow<JwtSignatureGate.Rejected> { JwtSignatureGate("a-different-key").check(realToken()) }
    }

    "structurally broken tokens are rejected, not crashed on" {
        // Each of these would be an exception of some other type if the gate did not guard the shape.
        listOf(
            "" to "empty",
            "." to "just a dot",
            "onlyonesegment" to "no dots",
            "header.payload." to "empty signature",
            ".payload.signature" to "empty header",
            "a.b.!!!not-base64!!!" to "signature is not base64url",
        ).forEach { (token, why) ->
            withClue(why) { shouldThrow<JwtSignatureGate.Rejected> { gate.check(token) } }
        }
    }

    // ── algorithm confusion is unreachable, not merely blocked ──────────────────────────────────────

    "alg:none in the header buys nothing, because the header is never read" {
        // The classic attack: claim `none` and send no signature. The gate does not consult `alg` at
        // all — the token dies because it carries no valid HMAC512, which is a stronger property than
        // rejecting the algorithm by name.
        shouldThrow<JwtSignatureGate.Rejected> {
            generator.verify(handMadeToken("""{"alg":"none","typ":"JWT"}""", """{"sub":"admin"}""", ""))
        }

        shouldThrow<JwtSignatureGate.Rejected> {
            gate.check("""${b64.encodeToString("""{"alg":"none"}""".toByteArray())}.${b64.encodeToString("""{"sub":"admin"}""".toByteArray())}.x""")
        }
    }

    "claiming a weaker algorithm in the header buys nothing either" {
        // HS256-signed token presented to an HS512 verifier: rejected on the MAC.
        val hs256 = JWT.create().withSubject("admin").sign(Algorithm.HMAC256(secret))

        shouldThrow<JwtSignatureGate.Rejected> { gate.check(hs256) }
    }

    // ── the library still does its job afterwards ───────────────────────────────────────────────────

    "the gate does not replace claim validation — a wrong issuer still fails" {
        // Correctly signed, so it passes the gate; the library must still reject it.
        val wrongIssuer = JWT.create()
            .withIssuer("somebody-else")
            .withAudience(config.audience)
            .withSubject("u3")
            .sign(Algorithm.HMAC512(secret))

        gate.check(wrongIssuer)

        val thrown = shouldThrow<JWTVerificationException> { generator.verify(wrongIssuer) }
        (thrown is JwtSignatureGate.Rejected) shouldBe false
    }

    "an expired token passes the gate and is rejected by the library" {
        val expired = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("u4")
            .withExpiresAt(java.util.Date(0))
            .sign(Algorithm.HMAC512(secret))

        gate.check(expired)

        shouldThrow<JWTVerificationException> { generator.verify(expired) }
    }

    "tryVerify still returns null rather than throwing, for gate rejections too" {
        // Rejected extends JWTVerificationException precisely so this keeps working.
        generator.tryVerify(handMadeToken("""{"alg":"HS512"}""", """{"sub": {{{ """)) shouldBe null
        generator.tryVerify("garbage") shouldBe null
    }
})
