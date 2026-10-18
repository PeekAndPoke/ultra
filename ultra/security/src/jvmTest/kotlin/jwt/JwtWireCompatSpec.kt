package io.peekandpoke.ultra.security.jwt

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

/**
 * A CRYPTOGRAPHIC CROSS-CHECK against `java-jwt` 4.5.2 — that our hand-rolled signer and verifier
 * agree, byte for byte, with a known-good independent implementation.
 *
 * The fixture tokens below are hard-coded strings MINTED BY THE REMOVED LIBRARY (re-minted 2026-07-31
 * with `withKeyId`, using this spec's exact config). Their original justification was wire
 * compatibility across the library swap — that no longer applies, because `kid` is required and no
 * pre-rotation token is meant to survive. What they are still worth is independent evidence: our
 * base64url, our signing-input assembly, our HMAC and our claim validation all have to agree with an
 * implementation that was written without reference to ours. A bug would have to exist in both to
 * hide here.
 *
 * Before the dependency was removed, this spec additionally cross-verified LIVE in both directions —
 * the library accepted what we sign, and we accepted what the library signs, freshly minted — and
 * both directions were green. Those two tests went with the dependency; the fixtures pin the
 * property permanently.
 *
 * The clock is fixed at epoch 1_800_000_000 so the expiry-related fixtures behave deterministically.
 */
class JwtWireCompatSpec : StringSpec({

    val secret = "wire-compat-signing-key-rfc7518-needs-sixty-four-bytes-min!!!!!!!"
    val now = 1_800_000_000L

    val config = JwtConfig(
        keys = listOf(JwtSigningKey(id = "wire-1", secret = Redacted(secret))),
        issuer = "wire-iss",
        audience = "wire-aud",
        permissionsNs = "permissions",
        userNs = "user",
    )

    fun generatorAt(epochSecond: Long) = JwtGenerator(
        config = config,
        clock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC),
    )

    val generator = generatorAt(now)

    // Minted with the full production claim shape: registered claims + user ns + permissions ns.
    val fixtureProductionShape = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImN1c3RvbS1jbGFpbSI6ImN1c3RvbS12YWx1ZSIsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEiLCJ1c2VyL2lkIjoiYjJiX3VzZXJzL3UxIiwidXNlci9kZXNjIjoiV2lyZSBVc2VyIiwidXNlci90eXBlIjoiaHVtYW4iLCJ1c2VyL2VtYWlsIjoid2lyZUBleGFtcGxlLmNvbSIsInBlcm1pc3Npb25zL3N1cGVydXNlciI6dHJ1ZSwicGVybWlzc2lvbnMvb3JnIjoib3JnYW5pc2F0aW9uL2FjbWUiLCJwZXJtaXNzaW9ucy9hY2Nlc3NpYmxlT3JncyI6WyJvcmdhbmlzYXRpb24vYWNtZSIsIm9yZ2FuaXNhdGlvbi9nbG9iZXgiXSwicGVybWlzc2lvbnMvYnJhbmNoZXMiOlsiYjEiXSwicGVybWlzc2lvbnMvZ3JvdXBzIjpbImcxIl0sInBlcm1pc3Npb25zL3JvbGVzIjpbImFkbWluIl0sInBlcm1pc3Npb25zL3Blcm1pc3Npb25zIjpbInJlYWQiLCJ3cml0ZSJdfQ." +
        "XNf66C8NbLPg0PaCkMt1YXNzLBUaZw4mDsnpOIyOIPrvuPZMwiL5PHxDh0-QiT5DX6I2buXkavEh1XGlOtJ26w"

    val fixtureNoExp = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJ3aXJlLWlzcyIsImF1ZCI6IndpcmUtYXVkIiwic3ViIjoiYjJiX3VzZXJzL3UxIn0." +
        "MtMgxJj9QGfEb-90HEaJWSAH7dI6Qa_Looutx3VdiXB_20gB7ktWJsfm__5ccWw_yPi_EGwsodipJTMSKffzZw"

    val fixtureAudArray = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjpbIndpcmUtYXVkIiwib3RoZXItYXVkIl0sInN1YiI6ImIyYl91c2Vycy91MSJ9." +
        "YJoRbXZM_SDh0Yuyf9rt9X0CXUVk1dXtq5Z2mXFc9gYw8azl6DMjLf7IXLGPH_fZWBTtLkSl1oWx6xQoGzG5Uw"

    val fixtureExpired = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjEwMDAwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEifQ." +
        "cV9vCGjRu0IwFA04nAfcxxf2HpFQtInpirB5c1NAC--7UPumlEUUtsFWC5I4i674wEI1alVll6ksBuiXrCSYlw"

    val fixtureWrongIssuer = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6InNvbWVib2R5LWVsc2UiLCJhdWQiOiJ3aXJlLWF1ZCIsInN1YiI6ImIyYl91c2Vycy91MSJ9." +
        "Mfo_zR36_5Q7Hv_pOyNqX_J1E4JJx0hpG9HWWjxsOKAlofnC6wNkXLD3VjqiZCP_Lg39u3Z01g0yGpjTT_HqTg"

    val fixtureHs256 = "eyJraWQiOiJ3aXJlLTEiLCJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEifQ." +
        "idHTbW9xIToHW01Z3TuGw9KIYeHKTRycTS5Zo1-tDns"

    "a library-minted production-shape token verifies and extracts identically" {
        val payload = generator.verify(fixtureProductionShape)

        assertSoftly {
            payload.subject shouldBe "b2b_users/u1"
            payload.issuer shouldBe "wire-iss"
            payload.audience shouldBe listOf("wire-aud")
            payload.getClaim("custom-claim").asString() shouldBe "custom-value"

            generator.extractUserData(payload) shouldBe JwtUserData(
                id = UserId("b2b_users/u1"),
                desc = "Wire User",
                type = "human",
                email = EmailAddress("wire@example.com"),
            )

            generator.extractPermissions(payload) shouldBe UserPermissions(
                isSuperUser = true,
                org = OrgId("organisation/acme"),
                accessibleOrgs = setOf(OrgId("organisation/acme"), OrgId("organisation/globex")),
                branches = setOf("b1"),
                groups = setOf("g1"),
                roles = setOf("admin"),
                permissions = setOf("read", "write"),
            )
        }
    }

    "a token issued without exp verifies at any time — missing exp means no expiry check" {
        generator.verify(fixtureNoExp).subject shouldBe "b2b_users/u1"

        // Still valid a century later. Tightening this would be a policy change, not a refactor.
        generatorAt(now + 100L * 365 * 24 * 3600).verify(fixtureNoExp).subject shouldBe "b2b_users/u1"
    }

    "an audience issued as an array still verifies" {
        generator.verify(fixtureAudArray).subject shouldBe "b2b_users/u1"
    }

    "an expired issued token is rejected — and accepted before its expiry, so the rejection is time-driven" {
        shouldThrow<JwtVerificationException> { generator.verify(fixtureExpired) }
            .shouldNotBeInstanceOf<JwtSignatureGate.Rejected>()

        generatorAt(999_999_000L).verify(fixtureExpired).subject shouldBe "b2b_users/u1"
    }

    "an issued token from a different issuer is rejected by claim validation, not the MAC" {
        shouldThrow<JwtVerificationException> { generator.verify(fixtureWrongIssuer) }
            .shouldNotBeInstanceOf<JwtSignatureGate.Rejected>()
    }

    "an HS256-signed issued token is rejected at the gate" {
        // Rejected on the alg comparison now — the key says HS512 and the header says HS256 — where it
        // used to reach the MAC and fail there. Both are gate rejections; the earlier step is strictly
        // better, because it never computes an HMAC for a token that cannot be valid.
        shouldThrow<JwtVerificationException> { generator.verify(fixtureHs256) }
            .shouldBeInstanceOf<JwtSignatureGate.Rejected>()
    }

    "OUR tokens are byte-identical to the library's, header and payload alike" {
        // The strongest form of the cross-check: not merely "we accept theirs", but "we emit theirs".
        // This covers the encoder — base64url alphabet and padding, claim order, `aud` and `exp` wire
        // shapes, the header's member order — none of which the verify-side tests can see.
        val ours = generatorAt(now).createJwt(
            user = JwtUserData(
                id = UserId("b2b_users/u1"),
                desc = "Wire User",
                type = "human",
                email = EmailAddress("wire@example.com"),
            ),
            permissions = UserPermissions(
                isSuperUser = true,
                org = OrgId("organisation/acme"),
                accessibleOrgs = setOf(OrgId("organisation/acme"), OrgId("organisation/globex")),
                branches = setOf("b1"),
                groups = setOf("g1"),
                roles = setOf("admin"),
                permissions = setOf("read", "write"),
            ),
        ) {
            withExpiresAt(Instant.ofEpochSecond(1_834_000_000L))
            withClaim("custom-claim", "custom-value")
        }

        assertSoftly {
            withClue("header") {
                ours.split(".")[0] shouldBe fixtureProductionShape.split(".")[0]
            }
            withClue("payload — claim order included, since it is inside the signing input") {
                ours.split(".")[1] shouldBe fixtureProductionShape.split(".")[1]
            }
            withClue("signature — implied by the two above, asserted so a failure names the whole token") {
                ours shouldBe fixtureProductionShape
            }
        }
    }

    "OUR minted payload keeps the library's wire shape for aud and exp" {
        // Kept as its own row despite the byte-identity test above: this one names the two shapes that
        // actually matter to an external reader, so a failure says WHAT drifted rather than "the
        // token differs". Every other assertion in this file reads through JwtPayload.audience, which
        // normalises a string and an array to the same list — so switching `withAudience` to always
        // emit an array would keep them green while silently changing `"aud":"x"` to `"aud":["x"]`
        // for every external reader, including the JS client.
        val payload = String(
            Base64.getUrlDecoder().decode(
                generator.createJwt(user = JwtUserData(id = UserId("b2b_users/u1"), desc = "d", type = "t"))
                    .split(".")[1]
            )
        )

        assertSoftly {
            withClue("a single audience is a plain string, never an array") {
                payload shouldContain """"aud":"wire-aud""""
            }
            withClue("exp is a bare number of epoch SECONDS, not millis and not a string") {
                payload shouldContain """"exp":${now + 3600}"""
            }
        }
    }
})
