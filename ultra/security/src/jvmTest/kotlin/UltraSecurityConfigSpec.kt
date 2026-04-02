package io.peekandpoke.ultra.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class UltraSecurityConfigSpec : StringSpec({

    "toString must redact csrfSecret" {
        val config = UltraSecurityConfig(
            csrfSecret = "super-secret-value",
            csrfTtlMillis = 60_000L,
        )

        val str = config.toString()
        str shouldContain "REDACTED"
        str shouldNotContain "super-secret-value"
        str shouldContain "60000"
    }

    "testOnly config must have a non-blank CSRF secret" {
        val config = UltraSecurityConfig.testOnly
        config.csrfSecret.isNotBlank()
    }
})
