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
 * WIRE COMPATIBILITY with tokens issued by `java-jwt` 4.5.2 — the property that decides whether
 * every logged-in user survives the library swap.
 *
 * The fixture tokens below are hard-coded strings MINTED BY THE REMOVED LIBRARY (2026-07-31, with
 * this spec's exact config), so the property stays pinned long after the library is gone: if our
 * verifier ever stops accepting them, tokens issued before the swap stop working on deploy.
 *
 * Before the dependency was removed, this spec additionally cross-verified LIVE in both directions
 * — the library accepted what we sign, and we accepted what the library signs, freshly minted — and
 * both directions were green. Those two tests went with the dependency; the fixtures pin the
 * property permanently.
 *
 * The clock is fixed at epoch 1_800_000_000 so the expiry-related fixtures behave deterministically.
 */
class JwtWireCompatSpec : StringSpec({

    val secret = "wire-compat-signing-key-rfc7518-needs-sixty-four-bytes-min!!!!!!!"
    val now = 1_800_000_000L

    val config = JwtConfig(
        signingKey = Redacted(secret),
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
    val fixtureProductionShape = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImN1c3RvbS1jbGFpbSI6ImN1c3RvbS12YWx1ZSIsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEiLCJ1c2VyL2lkIjoiYjJiX3VzZXJzL3UxIiwidXNlci9kZXNjIjoiV2lyZSBVc2VyIiwidXNlci90eXBlIjoiaHVtYW4iLCJ1c2VyL2VtYWlsIjoid2lyZUBleGFtcGxlLmNvbSIsInBlcm1pc3Npb25zL3N1cGVydXNlciI6dHJ1ZSwicGVybWlzc2lvbnMvb3JnIjoib3JnYW5pc2F0aW9uL2FjbWUiLCJwZXJtaXNzaW9ucy9hY2Nlc3NpYmxlT3JncyI6WyJvcmdhbmlzYXRpb24vYWNtZSIsIm9yZ2FuaXNhdGlvbi9nbG9iZXgiXSwicGVybWlzc2lvbnMvYnJhbmNoZXMiOlsiYjEiXSwicGVybWlzc2lvbnMvZ3JvdXBzIjpbImcxIl0sInBlcm1pc3Npb25zL3JvbGVzIjpbImFkbWluIl0sInBlcm1pc3Npb25zL3Blcm1pc3Npb25zIjpbInJlYWQiLCJ3cml0ZSJdfQ." +
        "YPmZRUdAMOat5OB4rPdP7xCDnCBtyaGooxyiH26Y8jW_8uLi1qnLhfqf2ee7xMC91qW6qlebZZu_sBlk7sM-3A"

    val fixtureNoExp = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJ3aXJlLWlzcyIsImF1ZCI6IndpcmUtYXVkIiwic3ViIjoiYjJiX3VzZXJzL3UxIn0." +
        "Jwar_tzV5znJTu-Or4YucjXrqiHBTi9S4GyUQAoXt-26YnT4DndQFaf86tKKnCkuB9eXbXlH6TkVhxcwAVDOWg"

    val fixtureAudArray = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjpbIndpcmUtYXVkIiwib3RoZXItYXVkIl0sInN1YiI6ImIyYl91c2Vycy91MSJ9." +
        "hKa4gSQbZUTzZZZBE9CqV_w3HXDYEysmat15REQOhxI0be4y3hiraJTJLqpFs1wqFmTJhrmc6EuHPYJymDMoZw"

    val fixtureExpired = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjEwMDAwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEifQ." +
        "HlhjFChx1ggvo5Et7QKKoIjvw78vDKLLc9debQF8_Hcq5VH7NYKJNLqjYQWLdSX2-Nurkl1FN8cjjJIlyOdeuQ"

    val fixtureWrongIssuer = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6InNvbWVib2R5LWVsc2UiLCJhdWQiOiJ3aXJlLWF1ZCIsInN1YiI6ImIyYl91c2Vycy91MSJ9." +
        "QBWaQ-OdyyK1hk0jDm633Um02flZ8rCxk_Zj5QjTU1gIe1NfqE_avtGhA8_pd73zDXxovmexd6z_Hu9oarMXIg"

    val fixtureHs256 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJleHAiOjE4MzQwMDAwMDAsImlzcyI6IndpcmUtaXNzIiwiYXVkIjoid2lyZS1hdWQiLCJzdWIiOiJiMmJfdXNlcnMvdTEifQ." +
        "QdyoPHNKWE-2AfYpJPgXeL_tIbmXdDFwGTraLhbq_xI"

    "an already-issued production-shape token verifies and extracts identically" {
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

    "an HS256-signed issued token is rejected on the MAC" {
        shouldThrow<JwtVerificationException> { generator.verify(fixtureHs256) }
            .shouldBeInstanceOf<JwtSignatureGate.Rejected>()
    }

    "our header is byte-identical to the library's" {
        val ours = generator
            .createJwt(user = JwtUserData(id = UserId("b2b_users/u1"), desc = "d", type = "t"))
            .split(".")[0]

        ours shouldBe fixtureProductionShape.split(".")[0]
    }

    "OUR minted payload keeps the library's wire shape for aud and exp" {
        // The fixtures above pin what the LIBRARY emitted; nothing pinned what WE emit. Every other
        // assertion reads through JwtPayload.audience, which normalises a string and an array to the
        // same list — so switching `withAudience` to always emit an array would keep the whole suite
        // green while silently changing `"aud":"x"` to `"aud":["x"]` for every external reader,
        // including the JS client. Assert on the raw payload segment instead.
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
