package io.peekandpoke.funktor.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.model.PasswordPolicy

/** The password forms' rule: the server's check, and a message that names the reason (ULTRA-00296). */
class PasswordPolicyFormRuleSpec : StringSpec({

    val rule = PasswordPolicy.default.asFormRule()
    val longest = "Aa1!" + "x".repeat(252)

    "256 characters pass, 257 are refused with the length message, a weak one with the policy's" {
        rule.check(longest) shouldBe true

        rule.check(longest + "x") shouldBe false
        rule.getMessage(longest + "x") shouldBe PasswordPolicy.TOO_LONG_DESCRIPTION

        rule.check("weak") shouldBe false
        rule.getMessage("weak") shouldBe PasswordPolicy.DEFAULT_PASSWORD_POLICY_DESCRIPTION
    }
})
