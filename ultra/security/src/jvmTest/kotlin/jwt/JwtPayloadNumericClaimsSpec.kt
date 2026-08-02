package io.peekandpoke.ultra.security.jwt

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * [JwtClaim.asLong] and [JwtPayload.expiresAt].
 *
 * These exist so a client can be told when its session ends without decoding the token itself. They read
 * the same claims as the verifier's `numericDate`, but degrade instead of throwing — a [JwtPayload] only
 * exists after verification, so anything malformed here is defence in depth and must not become a 500.
 */
class JwtPayloadNumericClaimsSpec : FreeSpec() {

    private fun payloadOf(build: JsonObjectBuilder.() -> Unit): JwtPayload =
        JwtPayload(buildJsonObject(build))

    init {

        "asLong reads a JSON number" {
            JwtClaim(JsonPrimitive(1785492930L)).asLong() shouldBe 1785492930L
        }

        "asLong survives values beyond Int32 — exp passes it in 2038" {
            // 4102444800 = 2100-01-01. An `as? Int` cast would silently fail or wrap here.
            JwtClaim(JsonPrimitive(4102444800L)).asLong() shouldBe 4102444800L
        }

        "asLong floors a fractional NumericDate — RFC 7519 4.1.4 does not restrict it to integers" {
            JwtClaim(JsonPrimitive(1785492930.75)).asLong() shouldBe 1785492930L
            // Floor, not truncate-toward-zero: they differ for negatives.
            JwtClaim(JsonPrimitive(-1.5)).asLong() shouldBe -2L
        }

        "asLong REJECTS a numeric string rather than coercing it" {
            withClue("a claim of \"123\" is a string, not a number — coercing inside a security type is the leniency worth losing") {
                JwtClaim(JsonPrimitive("123")).asLong().shouldBeNull()
            }
        }

        "asLong degrades to null for every non-numeric shape" {
            JwtClaim(null).asLong().shouldBeNull()
            JwtClaim(JsonPrimitive(true)).asLong().shouldBeNull()
            JwtClaim(buildJsonArray { add(JsonPrimitive(1)) }).asLong().shouldBeNull()
            JwtClaim(JsonObject(emptyMap())).asLong().shouldBeNull()
        }

        "expiresAt reads the exp claim" {
            payloadOf { put(JwtPayload.EXPIRES_AT, 1785492930L) }.expiresAt shouldBe 1785492930L
        }

        "expiresAt is null when exp is absent" {
            withClue("an absent exp means 'no expiry stated', not an error — the verifier already decided") {
                payloadOf { put(JwtPayload.SUBJECT, "u1") }.expiresAt.shouldBeNull()
            }
        }

        "expiresAt is null when exp is malformed, and does NOT throw" {
            payloadOf { put(JwtPayload.EXPIRES_AT, "not-a-date") }.expiresAt.shouldBeNull()
        }
    }
}
