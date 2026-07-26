package io.peekandpoke.funktor.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Pins the client-side claim decoder that `AuthState.readJwt` runs by default.
 *
 * Before this existed the default was `{ emptyMap() }`, so every claim-derived feature silently
 * no-opped: the selected org, the token user id, and the expiry were all null. Nothing failed
 * loudly — the Members page just reported "no organisation is selected" forever. These tests exist
 * so that never returns quietly.
 *
 * The type assertions are not pedantry: `readJwt` reads the claims back with `as?` casts, so the
 * runtime type a claim decodes to IS the contract. `exp` in particular used to be read as
 * `as? Int` — see the test below.
 */
class JwtClaimsSpec : StringSpec({

    /**
     * Builds an unsigned token whose payload is [payloadJson]; only the middle segment is read.
     *
     * The JSON is UTF-8 encoded BEFORE base64: `btoa` throws on any char above U+00FF, so a naive
     * `btoa(json)` cannot express the multi-byte case this file needs to cover.
     */
    fun tokenOf(payloadJson: String): String {
        val percentEncoded = js("encodeURIComponent")(payloadJson) as String
        val utf8Bytes = Regex("%([0-9A-Fa-f]{2})").replace(percentEncoded) { m ->
            m.groupValues[1].toInt(16).toChar().toString()
        }
        val b64 = js("btoa")(utf8Bytes) as String
        val payload = b64.trimEnd('=').replace('+', '-').replace('/', '_')
        return "header.$payload.signature"
    }

    "decodes a payload into a plain map" {
        val claims = decodeJwtClaims(tokenOf("""{"sub":"b2b_users/u1","desc":"Multi Org User"}"""))

        claims["sub"] shouldBe "b2b_users/u1"
        claims["desc"] shouldBe "Multi Org User"
    }

    "the namespaced permission claims survive with the types readJwt casts to" {
        val claims = decodeJwtClaims(
            tokenOf(
                """{"permissions/superuser":true,"permissions/org":"saas_organisations/globex",""" +
                        """"permissions/accessibleOrgs":["saas_organisations/acme","saas_organisations/globex"],""" +
                        """"permissions/roles":["admin"]}"""
            )
        )

        // `as? Boolean` in readJwt — the mapping that was missing entirely until now.
        (claims["permissions/superuser"] as? Boolean) shouldBe true
        (claims["permissions/org"] as? String) shouldBe "saas_organisations/globex"

        @Suppress("UNCHECKED_CAST")
        (claims["permissions/accessibleOrgs"] as? List<String>) shouldBe
                listOf("saas_organisations/acme", "saas_organisations/globex")

        @Suppress("UNCHECKED_CAST")
        (claims["permissions/roles"] as? List<String>) shouldBe listOf("admin")
    }

    "exp decodes as a Number — NOT reliably as an Int" {
        // JSON.parse produces a JS number. `readJwt` used to read this with `as? Int`, which is both
        // representation-dependent and breaks past 2038 (beyond Int32). If this ever regressed to
        // `as? Int`, tokenExpires would go null and the whole session-refresh lifecycle would
        // silently stop again — the exact class of failure this file guards.
        val claims = decodeJwtClaims(tokenOf("""{"exp":1785046462}"""))

        (claims["exp"] as? Number)?.toDouble() shouldBe 1785046462.0
    }

    "an exp beyond 2038 still decodes" {
        val claims = decodeJwtClaims(tokenOf("""{"exp":4102444800}"""))

        (claims["exp"] as? Number)?.toDouble() shouldBe 4102444800.0
    }

    "multi-byte UTF-8 survives the base64url round trip" {
        // `atob` yields one char per byte, so without the percent-encoding step this would mangle.
        val claims = decodeJwtClaims(tokenOf("""{"desc":"Jörg Müller","email":"jorg@münchen.example"}"""))

        claims["desc"] shouldBe "Jörg Müller"
        claims["email"] shouldBe "jorg@münchen.example"
    }

    "base64url padding is restored" {
        // Payload lengths that are not a multiple of 4 exercise the re-padding.
        listOf("""{"a":"1"}""", """{"ab":"12"}""", """{"abc":"123"}""", """{"abcd":"1234"}""")
            .forEach { json ->
                decodeJwtClaims(tokenOf(json)).isEmpty() shouldBe false
            }
    }

    "malformed input yields an empty map instead of throwing" {
        // A bad token must never be able to break sign-in.
        decodeJwtClaims("") shouldBe emptyMap()
        decodeJwtClaims("not-a-jwt") shouldBe emptyMap()
        decodeJwtClaims("only.two") shouldBe emptyMap()
        decodeJwtClaims("header..signature") shouldBe emptyMap()
        decodeJwtClaims("header.!!!not-base64!!!.signature") shouldBe emptyMap()
        decodeJwtClaims("header.${js("btoa")("not json") as String}.signature") shouldBe emptyMap()
    }
})
