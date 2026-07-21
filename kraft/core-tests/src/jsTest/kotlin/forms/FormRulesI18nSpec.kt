package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.forms.validation.equalTo
import io.peekandpoke.kraft.forms.validation.numbers.inRange
import io.peekandpoke.kraft.forms.validation.or
import io.peekandpoke.kraft.forms.validation.strings.minLength
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.forms.validation.strings.validEmail
import io.peekandpoke.kraft.i18n.installKraftForms
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.Locale

class FormRulesI18nSpec : StringSpec({

    fun i18n(lang: String) = I18n(Locale.parse(lang), fallback = Locale("en")) { installKraftForms() }

    "a default rule message resolves via the kraft catalog and translates" {
        val rule = minLength<String>(5)

        rule.getMessage("ab", i18n("en").translate) shouldBe "Must be at least 5 characters"
        rule.getMessage("ab", i18n("de").translate) shouldBe "Muss mindestens 5 Zeichen lang sein"
    }

    "the 1-arg getMessage still returns the plain English default (back-compat)" {
        minLength<String>(5).getMessage("ab") shouldBe "Must be at least 5 characters"
    }

    "a custom message is never translated" {
        val rule = notBlank<String>("Bitte ausfüllen")

        rule.getMessage("", i18n("de").translate) shouldBe "Bitte ausfüllen"
        rule.getMessage("") shouldBe "Bitte ausfüllen"
    }

    "plurals: minLength(1) uses the singular form" {
        minLength<String>(1).getMessage("", i18n("en").translate) shouldBe "Must be at least 1 character"
        minLength<String>(1).getMessage("", i18n("de").translate) shouldBe "Muss mindestens 1 Zeichen lang sein"
    }

    "placeholders: inRange resolves from/to in both languages" {
        val rule = inRange<Int>(1, 10)

        rule.getMessage(0, i18n("en").translate) shouldBe "Must be in range 1 .. 10"
        rule.getMessage(0, i18n("de").translate) shouldBe "Muss im Bereich 1 .. 10 liegen"
    }

    "SECURITY: equalTo never echoes the compared value (confirm-password secret)" {
        val secret = "s3cr3t-password"
        val rule = equalTo<String> { secret }

        rule.getMessage("typo", i18n("en").translate) shouldBe "The values must match"
        rule.getMessage("typo", i18n("de").translate) shouldBe "Die Werte müssen übereinstimmen"
        // neither the i18n path nor the plain fallback may leak the operand
        rule.getMessage("typo", i18n("en").translate).contains(secret) shouldBe false
        rule.getMessage("typo").contains(secret) shouldBe false
    }

    "composite `or` rules translate their child messages" {
        val rule = notBlank<String>() or validEmail()

        rule.getMessage("", i18n("en").translate) shouldBe "Must not be blank or Must be a valid email"
        rule.getMessage("", i18n("de").translate) shouldBe
                "Darf nicht leer sein oder Muss eine gültige E-Mail-Adresse sein"
    }
})
