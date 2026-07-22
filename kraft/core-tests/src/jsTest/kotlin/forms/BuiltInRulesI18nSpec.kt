package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.forms.validation.allRulesOf
import io.peekandpoke.kraft.forms.validation.anyOf
import io.peekandpoke.kraft.forms.validation.anyRuleOf
import io.peekandpoke.kraft.forms.validation.equalTo
import io.peekandpoke.kraft.forms.validation.nonNull
import io.peekandpoke.kraft.forms.validation.nonNullAnd
import io.peekandpoke.kraft.forms.validation.noneOf
import io.peekandpoke.kraft.forms.validation.notEqualTo
import io.peekandpoke.kraft.forms.validation.nullOrElse
import io.peekandpoke.kraft.forms.validation.numbers.greaterThan
import io.peekandpoke.kraft.forms.validation.numbers.greaterThanOrEqual
import io.peekandpoke.kraft.forms.validation.numbers.inRange
import io.peekandpoke.kraft.forms.validation.numbers.lessThan
import io.peekandpoke.kraft.forms.validation.numbers.lessThanOrEqual
import io.peekandpoke.kraft.forms.validation.or
import io.peekandpoke.kraft.forms.validation.strings.blank
import io.peekandpoke.kraft.forms.validation.strings.empty
import io.peekandpoke.kraft.forms.validation.strings.exactLength
import io.peekandpoke.kraft.forms.validation.strings.maxLength
import io.peekandpoke.kraft.forms.validation.strings.minLength
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.forms.validation.strings.notEmpty
import io.peekandpoke.kraft.forms.validation.strings.validEmail
import io.peekandpoke.kraft.forms.validation.strings.validSlug
import io.peekandpoke.kraft.forms.validation.strings.validUrlWithProtocol
import io.peekandpoke.kraft.forms.validation.collections.exactCount
import io.peekandpoke.kraft.forms.validation.collections.maxCount
import io.peekandpoke.kraft.forms.validation.collections.minCount
import io.peekandpoke.kraft.forms.validation.collections.notEmpty as collectionNotEmpty
import io.peekandpoke.kraft.i18n.installKraftForms
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.Locale

/**
 * Exhaustive i18n coverage for every built-in form rule: each default rule message must resolve
 * from the kraft catalog in both English (the fallback / API surface) and German (full parity).
 *
 * This proves, per rule, that (a) the generated accessor `t.forms.<key>(...)` exists and is wired
 * to the correct catalog key, and (b) both the `en` and `de` catalog carry that key with the right
 * placeholders/plurals. `ValidationRulesSpec` covers `check()` + the plain (1-arg) English message;
 * this spec covers the translated (2-arg) path.
 */
class BuiltInRulesI18nSpec : StringSpec({

    fun i18n(lang: String) = I18n(Locale.parse(lang), fallback = Locale("en")) { installKraftForms() }
    val en = i18n("en").translate
    val de = i18n("de").translate

    // Asserts a rule resolves to [expectedEn] under English and [expectedDe] under German. The
    // [failing] value is only there to feed getMessage — it never affects a default rule's text.
    suspend fun <T> Rule<T>.resolves(failing: T, expectedEn: String, expectedDe: String) {
        getMessage(failing, en) shouldBe expectedEn
        getMessage(failing, de) shouldBe expectedDe
    }

    // --- string rules ---------------------------------------------------------------------------

    "empty" {
        empty<String>().resolves("x", "Must be empty", "Muss leer sein")
    }
    "notEmpty (string)" {
        notEmpty<String>().resolves("", "Must not be empty", "Darf nicht leer sein")
    }
    "blank" {
        blank<String>().resolves("x", "Must be blank", "Muss leer sein")
    }
    "notBlank" {
        notBlank<String>().resolves(" ", "Must not be blank", "Darf nicht leer sein")
    }
    "minLength (plural)" {
        minLength<String>(5).resolves("ab", "Must be at least 5 characters", "Muss mindestens 5 Zeichen lang sein")
    }
    "minLength (singular)" {
        minLength<String>(1).resolves("", "Must be at least 1 character", "Muss mindestens 1 Zeichen lang sein")
    }
    "maxLength (plural)" {
        maxLength<String>(5).resolves("abcdef", "Must be at most 5 characters", "Darf höchstens 5 Zeichen lang sein")
    }
    "maxLength (singular)" {
        maxLength<String>(1).resolves("ab", "Must be at most 1 character", "Darf höchstens 1 Zeichen lang sein")
    }
    "exactLength (plural)" {
        exactLength<String>(5).resolves("ab", "Must be 5 characters", "Muss genau 5 Zeichen lang sein")
    }
    "exactLength (singular)" {
        exactLength<String>(1).resolves("ab", "Must be 1 character", "Muss genau 1 Zeichen lang sein")
    }
    "validEmail" {
        validEmail<String>().resolves("nope", "Must be a valid email", "Muss eine gültige E-Mail-Adresse sein")
    }
    "validUrlWithProtocol" {
        validUrlWithProtocol<String>().resolves("nope", "Must be a valid url", "Muss eine gültige URL sein")
    }
    "validSlug" {
        validSlug<String>().resolves(
            "Not A Slug",
            "Must be lowercase letters, digits and hyphens (no leading or trailing hyphen)",
            "Muss aus Kleinbuchstaben, Ziffern und Bindestrichen bestehen (kein Bindestrich am Anfang oder Ende)",
        )
    }

    // --- generic rules --------------------------------------------------------------------------

    "nonNull" {
        nonNull<String?>().resolves(null, "Must not be empty", "Darf nicht leer sein")
    }
    "equalTo" {
        equalTo("expected").resolves("typo", "The values must match", "Die Werte müssen übereinstimmen")
    }
    "notEqualTo" {
        notEqualTo("forbidden").resolves(
            "forbidden", "The values must not match", "Die Werte dürfen nicht übereinstimmen",
        )
    }
    "anyOf" {
        anyOf(listOf("a", "b")).resolves("z", "Must be a valid input", "Muss eine gültige Eingabe sein")
    }
    "noneOf" {
        noneOf(listOf("a", "b")).resolves("a", "Must be a valid input", "Muss eine gültige Eingabe sein")
    }

    // --- number rules ---------------------------------------------------------------------------

    "inRange" {
        inRange<Int>(1, 10).resolves(0, "Must be in range 1 .. 10", "Muss im Bereich 1 .. 10 liegen")
    }
    "greaterThan" {
        greaterThan<Int>(5).resolves(1, "Must be greater than 5", "Muss größer als 5 sein")
    }
    "greaterThanOrEqual" {
        greaterThanOrEqual<Int>(5).resolves(1, "Must be greater than 5 or equal", "Muss größer oder gleich 5 sein")
    }
    "lessThan" {
        lessThan<Int>(5).resolves(9, "Must be less than 5", "Muss kleiner als 5 sein")
    }
    "lessThanOrEqual" {
        lessThanOrEqual<Int>(5).resolves(9, "Must be less than 5 or equal", "Muss kleiner oder gleich 5 sein")
    }

    // --- collection rules -----------------------------------------------------------------------

    "notEmpty (collection)" {
        collectionNotEmpty<List<String>>().resolves(emptyList(), "Must not be empty", "Darf nicht leer sein")
    }
    "minCount (plural)" {
        minCount<List<String>>(3).resolves(
            emptyList(), "Must have at least 3 items", "Muss mindestens 3 Einträge haben",
        )
    }
    "minCount (singular)" {
        minCount<List<String>>(1).resolves(
            emptyList(), "Must have at least 1 item", "Muss mindestens 1 Eintrag haben",
        )
    }
    "maxCount (plural)" {
        maxCount<List<String>>(3).resolves(
            listOf("a", "b", "c", "d"), "Must have at most 3 items", "Darf höchstens 3 Einträge haben",
        )
    }
    "maxCount (singular)" {
        maxCount<List<String>>(1).resolves(
            listOf("a", "b"), "Must have at most 1 item", "Darf höchstens 1 Eintrag haben",
        )
    }
    "exactCount (plural)" {
        exactCount<List<String>>(3).resolves(emptyList(), "Must have 3 items", "Muss genau 3 Einträge haben")
    }
    "exactCount (singular)" {
        exactCount<List<String>>(1).resolves(emptyList(), "Must have 1 item", "Muss genau 1 Eintrag haben")
    }

    // --- composite rules ------------------------------------------------------------------------

    "or joins both child messages with the translated joiner" {
        (notBlank<String>() or validEmail()).resolves(
            "",
            "Must not be blank or Must be a valid email",
            "Darf nicht leer sein oder Muss eine gültige E-Mail-Adresse sein",
        )
    }
    "anyRuleOf includes the FIRST rule in the message (regression: it used to be dropped)" {
        anyRuleOf(notBlank<String>(), validEmail()).resolves(
            "",
            "Must not be blank or Must be a valid email",
            "Darf nicht leer sein oder Muss eine gültige E-Mail-Adresse sein",
        )
    }
    "allRulesOf includes the FIRST rule in the message and joins with `and`" {
        allRulesOf(notBlank<String>(), minLength(5)).resolves(
            "",
            "Must not be blank and Must be at least 5 characters",
            "Darf nicht leer sein und Muss mindestens 5 Zeichen lang sein",
        )
    }
    "nullOrElse delegates to the inner rule's translated message for a non-null value" {
        nullOrElse(minLength<String>(5)).resolves(
            "ab", "Must be at least 5 characters", "Muss mindestens 5 Zeichen lang sein",
        )
    }
    "nonNullAnd reports the not-empty message for null and delegates for a non-null value" {
        nonNullAnd(minLength<String>(5)).resolves(null, "Must not be empty", "Darf nicht leer sein")
        nonNullAnd(minLength<String>(5)).resolves(
            "ab", "Must be at least 5 characters", "Muss mindestens 5 Zeichen lang sein",
        )
    }
})
