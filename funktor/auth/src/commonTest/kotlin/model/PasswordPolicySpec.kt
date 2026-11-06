package io.peekandpoke.funktor.auth.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** No realm accepts a password longer than 256 characters, whatever its regex says (ULTRA-00296). */
class PasswordPolicySpec : StringSpec({

    val anything = PasswordPolicy(regex = "^.*$", description = "Anything")

    "256 characters pass, 257 do not: also for a policy whose regex takes anything" {
        anything.matches("a".repeat(256)) shouldBe true
        anything.matches("a".repeat(257)) shouldBe false

        PasswordPolicy.isTooLong("a".repeat(256)) shouldBe false
        PasswordPolicy.isTooLong("a".repeat(257)) shouldBe true
        anything.refusalDescription("a".repeat(257)) shouldBe "Must be at most 256 characters long"
        anything.refusalDescription("a".repeat(256)) shouldBe "Anything"
    }

    "the default policy too: a strong password of exactly 256 passes, one character more does not" {
        val strong = "Aa1!" + "x".repeat(252)

        PasswordPolicy.default.matches(strong) shouldBe true
        PasswordPolicy.default.matches(strong + "x") shouldBe false
    }

    "the cap is 256, and its message says so" {
        PasswordPolicy.MAX_LENGTH shouldBe 256
        PasswordPolicy.TOO_LONG_DESCRIPTION shouldBe "Must be at most 256 characters long"
    }
})
