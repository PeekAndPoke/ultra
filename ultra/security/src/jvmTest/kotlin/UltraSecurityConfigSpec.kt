package io.peekandpoke.ultra.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.common.model.Redacted

class UltraSecurityConfigSpec : StringSpec({

    "toString must not disclose the csrf secret" {
        val config = UltraSecurityConfig(
            csrfSecret = Redacted("super-secret-value"),
            csrfTtlMillis = 60_000L,
        )

        val str = config.toString()

        // The hand-written redacting toString this class used to carry is gone: `Redacted` redacts
        // itself, so the GENERATED toString is safe. That matters — the hand-written one covered only
        // toString while every serializer wrote the value in full, which is how it reached insights.
        str shouldContain Redacted.PLACEHOLDER
        str shouldNotContain "super-secret-value"
        str shouldContain "60000"
    }

    "testOnly config carries a non-blank CSRF secret" {
        // Was `config.csrfSecret.isNotBlank()` — a dangling expression asserting NOTHING, which passed
        // whatever the value was. Found while converting the field.
        UltraSecurityConfig.testOnly.csrfSecret.value.isNotBlank() shouldBe true
    }
})
