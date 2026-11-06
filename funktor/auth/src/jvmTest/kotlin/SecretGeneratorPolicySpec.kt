package io.peekandpoke.funktor.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.ultra.security.SecretGenerator

/** A temp password from `SecretGenerator` against a realm's real `PasswordPolicy` (ULTRA-00067). */
class SecretGeneratorPolicySpec : StringSpec({

    val generator = SecretGenerator.default

    "a temp password drawn against the default policy always meets it" {
        repeat(200) {
            PasswordPolicy.default.matches(generator.tempPassword(accept = PasswordPolicy.default::matches)) shouldBe true
        }
    }

    "a length-first policy: the default length cannot meet a longer minimum, a long enough one always does" {
        val twentyOrMore = PasswordPolicy(regex = "^.{20,}$", description = "At least 20 characters")

        shouldThrow<IllegalStateException> { generator.tempPassword(accept = twentyOrMore::matches) }

        repeat(50) {
            twentyOrMore.matches(generator.tempPassword(length = 20, accept = twentyOrMore::matches)) shouldBe true
        }
    }
})
