package io.peekandpoke.ultra.security.jwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.security.user.UserId
import java.util.Base64
import java.util.Date

/**
 * The gate that authenticates a token before its payload is parsed, and selects the key by `kid`.
 *
 * Two properties are load-bearing here and are tested separately:
 *
 * 1. **The payload is never parsed before the MAC.** The malformed-payload and 20 MB probes are the
 *    exact ones that showed the removed `java-jwt` parsing unauthenticated JSON.
 * 2. **The header selects a KEY, never an ALGORITHM.** `alg` is compared against the configured key
 *    and a mismatch rejects, so algorithm confusion cannot start.
 */
class JwtSignatureGateSpec : StringSpec({

    val secret = "test-signing-key-for-the-gate-spec-rfc7518-sixty-four-bytes!!!!!!"
    val otherSecret = "a-different-key-rfc7518-requires-sixty-four-bytes-minimum!!!!!!!!"

    fun key(id: String, s: String = secret) = JwtSigningKey(id = id, secret = Redacted(s))

    val config = JwtConfig(
        keys = listOf(key("gate-1")),
        issuer = "test-issuer",
        audience = "test-audience",
        permissionsNs = "permissions",
        userNs = "user",
    )

    val generator = JwtGenerator(config)
    val gate = JwtSignatureGate(config.keys)

    val b64 = Base64.getUrlEncoder().withoutPadding()

    fun encode(s: String) = b64.encodeToString(s.toByteArray())

    /**
     * A wrong signature that is nevertheless well-formed: 64 zero bytes, the right size for
     * HMAC-SHA512. Using a literal like `"not-a-valid-signature"` instead made these tokens die in
     * the base64url decoder, one step BEFORE the MAC comparison — so every row using it silently
     * stopped testing what it named. Caught by asserting on the rejection message.
     */
    val wrongSignature = b64.encodeToString(ByteArray(64))

    fun handMadeToken(header: String, payload: String, signature: String = wrongSignature) =
        "${encode(header)}.${encode(payload)}.$signature"

    /** A hand-made token carrying a genuine MAC — only reachable by a key holder. */
    fun signedToken(header: String, payload: String, signWith: JwtSigningKey = config.keys.first()): String {
        val signingInput = "${encode(header)}.${encode(payload)}"

        return "$signingInput.${b64.encodeToString(JwtSignatureGate(listOf(signWith)).mac(signingInput))}"
    }

    /** A genuine token, produced by the very code path that signs in production. */
    fun realToken(): String = generator.createJwt(user = JwtUserData(id = UserId("u1"), desc = "d", type = "t"))

    // ── the key set must be usable at all ───────────────────────────────────────────────────────────

    "a secret shorter than its algorithm's floor is refused, with an actionable message" {
        // RFC 7518 §3.2 makes >= hash-output size a MUST for HMAC. It matters because there is no KDF
        // between the configured string and the MAC key material, so guessing costs one HMAC per
        // attempt and a captured token is an offline oracle.
        listOf(
            "" to "empty",
            "short" to "obviously too short",
            "a".repeat(63) to "one byte under the limit",
        ).forEach { (s, why) ->
            withClue(why) {
                val thrown = shouldThrow<IllegalArgumentException> { JwtSignatureGate(listOf(key("k", s))) }

                thrown.message!! shouldContain "RFC 7518"
                withClue("the message must name the offending key, since there can be several") {
                    thrown.message!! shouldContain "'k'"
                }
                withClue("the message must say HOW to fix it, not just that it is wrong") {
                    thrown.message!! shouldContain "openssl rand -base64 64"
                }
            }
        }

        withClue("exactly 64 bytes is accepted — the boundary is inclusive") {
            JwtSignatureGate(listOf(key("k", "a".repeat(64))))
        }
    }

    "an empty key list is refused" {
        shouldThrow<IllegalArgumentException> { JwtSignatureGate(emptyList()) }
            .message!! shouldContain "no signing keys"
    }

    "duplicate key ids are refused" {
        // A `kid` selects exactly one key. With duplicates the map would silently keep the last one,
        // so a token signed under the FIRST key of that id would stop verifying.
        val thrown = shouldThrow<IllegalArgumentException> {
            JwtSignatureGate(listOf(key("dup"), key("other"), key("dup", otherSecret)))
        }

        thrown.message!! shouldContain "[dup]"
    }

    "a blank key id is refused" {
        shouldThrow<IllegalArgumentException> { JwtSignatureGate(listOf(key(" "))) }
            .message!! shouldContain "blank id"
    }

    "the boot check and the constructor agree" {
        // FunktorRestBuilder.jwt() calls requireUsableKeys eagerly, because the kontainer binding is
        // lazy and would otherwise defer the failure to the first bearer request on a live server.
        shouldThrow<IllegalArgumentException> { JwtSignatureGate.requireUsableKeys(emptyList()) }
        shouldThrow<IllegalArgumentException> { JwtSignatureGate.requireUsableKeys(listOf(key("k", "too-short"))) }
        shouldThrow<IllegalArgumentException> { JwtSignatureGate.requireUsableKeys(listOf(key("a"), key("a"))) }

        JwtSignatureGate.requireUsableKeys(config.keys)
    }

    // ── the property this exists for ────────────────────────────────────────────────────────────────

    "a malformed payload is rejected WITHOUT being parsed" {
        val thrown = shouldThrow<JwtVerificationException> {
            generator.verify(handMadeToken("""{"kid":"gate-1","alg":"HS512","typ":"JWT"}""", """{"sub": {{{ """))
        }

        // If the payload parsed before the MAC, this malformed one would surface as the decode
        // rejection ("payload is not a JSON object") instead of a gate rejection. The TYPE is what
        // pins the order of the two steps.
        thrown.shouldBeInstanceOf<JwtSignatureGate.Rejected>()
        thrown.message shouldBe "Token signature is invalid"
    }

    "an oversized token is rejected before it is hashed, let alone parsed" {
        // Under java-jwt this 20 MB string was fully parsed and allocated, and only THEN failed the
        // signature check. It does not even reach the MAC.
        val huge = handMadeToken(
            """{"kid":"gate-1","alg":"HS512","typ":"JWT"}""",
            """{"s":"""" + "A".repeat(20_000_000) + """"}""",
        )

        shouldThrow<JwtSignatureGate.Rejected> { generator.verify(huge) }
            .message shouldBe "Token exceeds the maximum accepted length"
    }

    "a deeply nested payload never reaches a parser" {
        val nested = handMadeToken(
            """{"kid":"gate-1","alg":"HS512","typ":"JWT"}""",
            "[".repeat(10_000) + "]".repeat(10_000),
        )

        shouldThrow<JwtSignatureGate.Rejected> { generator.verify(nested) }
    }

    // ── the header IS parsed, and is bounded before it is ───────────────────────────────────────────

    "an oversized header is rejected BEFORE it is decoded" {
        // The cap is on the ENCODED segment, which is the only length available before the work it
        // bounds. Padded out with an unknown member so the header would otherwise be perfectly valid:
        // the rejection is about size, not shape.
        val fat = """{"kid":"gate-1","alg":"HS512","typ":"JWT","x":"${"A".repeat(4096)}"}"""

        withClue("the encoded segment must really exceed the cap, or this proves nothing") {
            (encode(fat).length > JwtSignatureGate.MAX_HEADER_LENGTH) shouldBe true
        }

        shouldThrow<JwtSignatureGate.Rejected> { gate.check(handMadeToken(fat, """{"sub":"x"}""")) }
            .message shouldBe "Token header exceeds the maximum accepted length"
    }

    "a header just under the cap is still parsed and evaluated on its merits" {
        // The complement of the row above: proves the cap is a cap and not a blanket rejection of
        // anything with an unknown member.
        val padding = "A".repeat(600)
        val header = """{"kid":"gate-1","alg":"HS512","typ":"JWT","x":"$padding"}"""

        withClue("this header must fit under the cap") {
            (encode(header).length <= JwtSignatureGate.MAX_HEADER_LENGTH) shouldBe true
        }

        // Reaches the MAC — the failure is the signature, not the size. And with a real MAC it passes.
        shouldThrow<JwtSignatureGate.Rejected> { gate.check(handMadeToken(header, """{"sub":"x"}""")) }
            .message shouldBe "Token signature is invalid"

        gate.check(signedToken(header, """{"sub":"x"}"""))
    }

    "a malformed or non-JSON header is rejected as such" {
        listOf(
            """{"alg": [[[ """ to "not JSON at all",
            """[1,2,3]""" to "JSON, but not an object",
            """{"kid":42,"alg":"HS512"}""" to "kid is a number, not a string",
            """{"kid":"gate-1","alg":["HS512"]}""" to "alg is an array",
        ).forEach { (header, why) ->
            withClue(why) {
                shouldThrow<JwtSignatureGate.Rejected> { gate.check(handMadeToken(header, """{"sub":"x"}""")) }
                    .message shouldBe "Token header is not valid"
            }
        }

        withClue("a header segment that is not base64url at all") {
            shouldThrow<JwtSignatureGate.Rejected> { gate.check("!!!.${encode("""{"sub":"x"}""")}.sig") }
                .message shouldBe "Token header is not valid"
        }
    }

    "unknown header members are ignored — only a key holder can add any" {
        gate.check(signedToken("""{"kid":"gate-1","alg":"HS512","typ":"JWT","crit":["x"],"x":1}""", """{"sub":"x"}"""))
    }

    // ── kid selection ───────────────────────────────────────────────────────────────────────────────

    "the kid selects which key verifies" {
        val a = key("a")
        val b = key("b", otherSecret)
        val both = JwtSignatureGate(listOf(a, b))

        // Each token verifies only under the key its kid names, even though both keys are configured.
        both.check(signedToken("""{"kid":"a","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}""", signWith = a))
        both.check(signedToken("""{"kid":"b","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}""", signWith = b))

        withClue("a token signed with key b but claiming kid a must fail — the kid is not a hint") {
            shouldThrow<JwtSignatureGate.Rejected> {
                both.check(signedToken("""{"kid":"a","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}""", signWith = b))
            }
        }
    }

    "the FIRST configured key signs — not the last, and not the newest by `issued`" {
        // List order is the contract, deliberately not derived from `issued`: editing a date must not
        // silently change who signs. Both other keys here carry a LATER `issued` than the first, so a
        // date-driven implementation would pick one of them and fail this.
        val keys = listOf(
            key("current").copy(issued = "2026-01-01"),
            key("previous", otherSecret).copy(issued = "2026-09-09"),
            key("older", otherSecret).copy(issued = "2026-12-31"),
        )

        val rotating = JwtGenerator(config.copy(keys = keys))

        rotating.signingKey.id shouldBe "current"

        val header = String(Base64.getUrlDecoder().decode(rotating.createJwt(user = JwtUserData(id = UserId("u"), desc = "d", type = "t")).split(".")[0]))

        header shouldContain """"kid":"current""""
    }

    "rotation: a token survives the deploy that adds a key, and dies with the deploy that drops it" {
        val old = key("old")
        val new = key("new", otherSecret)

        // Issued before the rotation, under the only key there was.
        val issuedUnderOld = JwtGenerator(config.copy(keys = listOf(old)))
            .createJwt(user = JwtUserData(id = UserId("u1"), desc = "d", type = "t"))

        withClue("deploy 1 — the new key is prepended, the old one still verifies") {
            val during = JwtGenerator(config.copy(keys = listOf(new, old)))

            during.verify(issuedUnderOld).subject shouldBe "u1"
            withClue("and new tokens are already signed under the new key") {
                during.signingKey.id shouldBe "new"
            }
        }

        withClue("deploy 2 — the old key is dropped and its tokens stop verifying") {
            shouldThrow<JwtSignatureGate.Rejected> {
                JwtGenerator(config.copy(keys = listOf(new))).verify(issuedUnderOld)
            }
        }
    }

    "an unknown kid is rejected outright — every configured key is NOT tried" {
        // The whole point of kid selection: a forged token costs ONE lookup, not one HMAC per key.
        // A valid MAC under a configured key still dies, because the kid names something else.
        val both = JwtSignatureGate(listOf(key("a"), key("b", otherSecret)))

        shouldThrow<JwtSignatureGate.Rejected> {
            both.check(signedToken("""{"kid":"nope","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}"""))
        }
    }

    "an absent kid is rejected — there is no default key" {
        // No backward compatibility with pre-rotation tokens is wanted, so a header without a kid is
        // simply a header that names no key.
        shouldThrow<JwtSignatureGate.Rejected> {
            gate.check(signedToken("""{"alg":"HS512","typ":"JWT"}""", """{"sub":"x"}"""))
        }

        shouldThrow<JwtSignatureGate.Rejected> {
            gate.check(signedToken("""{"kid":null,"alg":"HS512","typ":"JWT"}""", """{"sub":"x"}"""))
        }
    }

    "an unknown kid and a bad signature are indistinguishable in the message" {
        // The caller is unauthenticated. Telling them WHICH half failed tells them which to keep
        // trying. (Timing still separates the two; kid values are public, so that is accepted — see
        // the class KDoc.)
        val unknownKid = shouldThrow<JwtSignatureGate.Rejected> {
            gate.check(signedToken("""{"kid":"nope","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}"""))
        }
        val badMac = shouldThrow<JwtSignatureGate.Rejected> {
            gate.check(handMadeToken("""{"kid":"gate-1","alg":"HS512","typ":"JWT"}""", """{"sub":"x"}"""))
        }

        unknownKid.message shouldBe badMac.message
        badMac.message shouldBe "Token signature is invalid"
    }

    // ── algorithm confusion cannot start ────────────────────────────────────────────────────────────

    "the header's alg must AGREE with the key's — it never selects" {
        // The inverse of algorithm confusion. Every one of these carries a genuine HMAC-SHA512 under
        // the configured key, so only the alg comparison can reject them.
        listOf(
            """{"kid":"gate-1","alg":"none","typ":"JWT"}""" to "the classic alg:none",
            """{"kid":"gate-1","alg":"HS256","typ":"JWT"}""" to "a weaker HMAC",
            """{"kid":"gate-1","alg":"RS256","typ":"JWT"}""" to "an asymmetric algorithm",
            """{"kid":"gate-1","alg":"hs512","typ":"JWT"}""" to "the right algorithm, wrong case",
            """{"kid":"gate-1","typ":"JWT"}""" to "no alg at all",
        ).forEach { (header, why) ->
            withClue(why) {
                shouldThrow<JwtSignatureGate.Rejected> { gate.check(signedToken(header, """{"sub":"admin"}""")) }
            }
        }

        withClue("the matching alg passes, so the rows above fail on the comparison and nothing else") {
            gate.check(signedToken("""{"kid":"gate-1","alg":"HS512","typ":"JWT"}""", """{"sub":"admin"}"""))
        }
    }

    "alg:none with no signature at all buys nothing either" {
        shouldThrow<JwtSignatureGate.Rejected> {
            generator.verify(handMadeToken("""{"kid":"gate-1","alg":"none","typ":"JWT"}""", """{"sub":"admin"}""", ""))
        }
    }

    // ── the gate must not be stricter than the issuer ───────────────────────────────────────────────

    "a genuine token passes the gate and still verifies" {
        // Wire compatibility: the gate must compute the SAME signing input as the issuer, or every
        // real token breaks. This is the test that catches a re-encoding mistake.
        val token = realToken()

        gate.check(token)

        generator.verify(token).subject shouldBe "u1"
        // tryVerify must agree with verify — compared by a claim, since the decoder has no equals()
        generator.tryVerify(token)?.subject shouldBe "u1"
    }

    "a token minted by java-jwt 4.5.2 with a kid still passes — cryptographic cross-check" {
        // Hard-coded fixture generated by the removed library (2026-07-31) with this spec's config and
        // `withKeyId("gate-1")`. Back-compat is no longer the reason to keep it: it is independent
        // evidence that our HMAC, our base64url and our signing-input assembly agree with a known-good
        // implementation. A bug in any of the three would have to be present in both to hide here.
        val libraryMinted = "eyJraWQiOiJnYXRlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJ0ZXN0LWlzc3VlciIsImF1ZCI6InRlc3QtYXVkaWVuY2UiLCJzdWIiOiJ1MiJ9." +
            "rzvIJQflC4fnSgNgJJqHl3jvQI3hVWExWvJjFNXe6iPqCtEOm1J9KRApqKCVZCEo7jj3Yb3j69npDrYBHdIUEg"

        gate.check(libraryMinted)

        generator.verify(libraryMinted).subject shouldBe "u2"
    }

    // ── rejections ──────────────────────────────────────────────────────────────────────────────────

    "a tampered payload is rejected" {
        val real = realToken()
        val parts = real.split(".")
        val tampered = "${parts[0]}.${encode("""{"sub":"admin"}""")}.${parts[2]}"

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

        // Guard on the DECODED BYTES, not on the strings. `flipped != real` is true by construction and
        // would still pass if `at` were moved back onto the final character — the exact bug the comment
        // above records — leaving only the real assertion to fail, with a misleading message.
        withClue("flipping a middle signature character must change the decoded bytes") {
            val decode = { t: String -> Base64.getUrlDecoder().decode(t.substringAfterLast('.')) }
            decode(flipped).contentEquals(decode(real)) shouldBe false
        }

        shouldThrow<JwtSignatureGate.Rejected> { gate.check(flipped) }
    }

    "the wrong key is rejected — same kid, different secret" {
        // The id matching is not what authenticates; the MAC is.
        shouldThrow<JwtSignatureGate.Rejected> {
            JwtSignatureGate(listOf(key("gate-1", otherSecret))).check(realToken())
        }
    }

    "structurally broken tokens are rejected, not crashed on" {
        // Each of these would be an exception of some other type if the gate did not guard the shape.
        listOf(
            "" to "empty",
            "." to "just a dot",
            ".." to "two dots, nothing else",
            "onlyonesegment" to "no dots",
            "header.payload" to "only one dot",
            "header.payload." to "empty signature",
            ".payload.signature" to "empty header",
            "header..signature" to "empty payload",
            "a.b.c.d" to "four segments",
            "a.b.!!!not-base64!!!" to "signature is not base64url",
        ).forEach { (token, why) ->
            withClue(why) { shouldThrow<JwtSignatureGate.Rejected> { gate.check(token) } }
        }
    }

    "a valid MAC over the wrong number of segments is still rejected — by shape, before the MAC" {
        // Only a key holder can reach this, but the shape check must run first regardless: it is what
        // lets `decodeClaims` index segment 1 without a bounds check becoming a 500.
        val header = encode("""{"kid":"gate-1","alg":"HS512","typ":"JWT"}""")

        fun craft(signingInput: String) = "$signingInput.${b64.encodeToString(gate.mac(signingInput))}"

        listOf(
            craft(header) to "2 segments",
            craft("$header.a.b") to "4 segments",
        ).forEach { (token, why) ->
            withClue(why) {
                shouldThrow<JwtSignatureGate.Rejected> { gate.check(token) }
                    .message shouldBe "Token is not a well-formed JWS compact serialization"
            }
        }
    }

    // ── claim validation still does its job afterwards ──────────────────────────────────────────────

    "the gate does not replace claim validation — a wrong issuer still fails" {
        // Correctly signed with the SAME key, so it passes the gate; claim validation must reject it.
        val wrongIssuer = JwtGenerator(config.copy(issuer = "somebody-else"))
            .createJwt(user = JwtUserData(id = UserId("u3"), desc = "d", type = "t"))

        gate.check(wrongIssuer)

        val thrown = shouldThrow<JwtVerificationException> { generator.verify(wrongIssuer) }
        (thrown is JwtSignatureGate.Rejected) shouldBe false
    }

    "an expired token passes the gate and is rejected by the claim validation" {
        val expired = generator.createJwt(user = JwtUserData(id = UserId("u4"), desc = "d", type = "t")) {
            withExpiresAt(Date(0))
        }

        gate.check(expired)

        shouldThrow<JwtVerificationException> { generator.verify(expired) }
    }

    "tryVerify still returns null rather than throwing, for gate rejections too" {
        // Rejected extends JwtVerificationException precisely so this keeps working.
        generator.tryVerify(handMadeToken("""{"kid":"gate-1","alg":"HS512"}""", """{"sub": {{{ """)) shouldBe null
        generator.tryVerify("garbage") shouldBe null
    }
})
