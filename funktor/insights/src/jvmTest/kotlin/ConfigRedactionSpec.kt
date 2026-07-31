package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * **INTERIM — delete with [ConfigRedaction] when `Redacted<T>` lands.**
 *
 * The shapes here are the real ones, read out of an actual record on 2026-07-31: the JWT signing key and
 * the CSRF secret were written verbatim into every insights record, and `@JsonIgnore` — which protects
 * four other fields elsewhere — covered neither.
 */
class ConfigRedactionSpec : StringSpec({

    "the JWT signing key does not survive, at the depth it really sits" {
        val config = mapOf(
            "funktor" to mapOf(
                "auth" to mapOf(
                    "jwt" to mapOf(
                        "signingKey" to "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG",
                        "issuer" to "Funktor-All | Test",
                    )
                ),
                "security" to mapOf("csrfSecret" to "CHANGE_ME"),
            ),
            "arangodb" to mapOf("user" to "root", "password" to "SECRET-PW", "host" to "localhost"),
        )

        val out = ConfigRedaction.redact(config).toString()

        out.contains("ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG") shouldBe false
        out.contains("CHANGE_ME") shouldBe false
        out.contains("SECRET-PW") shouldBe false
        // and the non-secret neighbours survive, so this is redaction and not blanket removal
        out.contains("Funktor-All | Test") shouldBe true
        out.contains("localhost") shouldBe true
        out.contains("root") shouldBe true
    }

    "a sensitive name redacts its whole subtree, not just a scalar" {
        // The inner names are deliberately INNOCUOUS — `account` and `region` match nothing. An earlier
        // version of this test used `accessKey` inside, which the recursion redacts on its own, so it
        // passed even when subtree redaction was removed. Caught by mutation.
        val out = ConfigRedaction.redact(
            mapOf("credentials" to mapOf("account" to "SECRET-ACCT", "region" to "eu-central-1"))
        ).toString()

        out.contains("SECRET-ACCT") shouldBe false
        out.contains("eu-central-1") shouldBe false
    }

    "the header policy alone would NOT have caught signingKey" {
        // Why this object exists rather than reusing HeaderLogging: its pattern is tuned for header
        // names and matches `api[-_]?key`, not a bare `key`. `signingkey` slips straight through it.
        HeaderLogging.defaults.actionFor("signingKey") shouldBe HeaderAction.LOG
        ConfigRedaction.isSensitive("signingKey") shouldBe true
    }

    "values inside lists are redacted too" {
        val out = ConfigRedaction.redact(
            mapOf("clients" to listOf(mapOf("id" to "a", "clientSecret" to "SECRET-1")))
        ).toString()

        out.contains("SECRET-1") shouldBe false
        out.contains("id=a") shouldBe true
    }

    "ordinary config is untouched" {
        val config = mapOf("host" to "localhost", "port" to 8080, "enabled" to true)

        ConfigRedaction.redact(config) shouldBe config
    }
})
