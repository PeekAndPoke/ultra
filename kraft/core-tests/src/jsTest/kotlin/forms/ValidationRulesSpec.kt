package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.forms.validation.allRulesOf
import io.peekandpoke.kraft.forms.validation.anyOf
import io.peekandpoke.kraft.forms.validation.anyRuleOf
import io.peekandpoke.kraft.forms.validation.collections.exactCount
import io.peekandpoke.kraft.forms.validation.collections.maxCount
import io.peekandpoke.kraft.forms.validation.collections.minCount
import io.peekandpoke.kraft.forms.validation.equalTo
import io.peekandpoke.kraft.forms.validation.given
import io.peekandpoke.kraft.forms.validation.nonNull
import io.peekandpoke.kraft.forms.validation.nonNullAnd
import io.peekandpoke.kraft.forms.validation.noneOf
import io.peekandpoke.kraft.forms.validation.notEqualTo
import io.peekandpoke.kraft.forms.validation.nullOrElse
import io.peekandpoke.kraft.forms.validation.or
import io.peekandpoke.kraft.forms.validation.strings.blank
import io.peekandpoke.kraft.forms.validation.strings.empty
import io.peekandpoke.kraft.forms.validation.strings.exactLength
import io.peekandpoke.kraft.forms.validation.strings.maxLength
import io.peekandpoke.kraft.forms.validation.strings.minLength
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.forms.validation.strings.notEmpty
import io.peekandpoke.kraft.forms.validation.strings.validEmail
import io.peekandpoke.kraft.forms.validation.strings.validUrlWithProtocol
import io.peekandpoke.kraft.forms.validation.collections.notEmpty as collectionNotEmpty
import io.peekandpoke.kraft.forms.validation.comparable.greaterThan as comparableGreaterThan
import io.peekandpoke.kraft.forms.validation.comparable.greaterThanOrEqual as comparableGreaterThanOrEqual
import io.peekandpoke.kraft.forms.validation.comparable.inRange as comparableInRange
import io.peekandpoke.kraft.forms.validation.comparable.lessThan as comparableLessThan
import io.peekandpoke.kraft.forms.validation.comparable.lessThanOrEqual as comparableLessThanOrEqual
import io.peekandpoke.kraft.forms.validation.numbers.greaterThan as numberGreaterThan
import io.peekandpoke.kraft.forms.validation.numbers.greaterThanOrEqual as numberGreaterThanOrEqual
import io.peekandpoke.kraft.forms.validation.numbers.inRange as numberInRange
import io.peekandpoke.kraft.forms.validation.numbers.lessThan as numberLessThan
import io.peekandpoke.kraft.forms.validation.numbers.lessThanOrEqual as numberLessThanOrEqual

class ValidationRulesSpec : StringSpec({

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: empty
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string empty() - passes for empty string" {
        val rule: Rule<String> = empty()
        rule.check("") shouldBe true
    }

    "string empty() - fails for non-empty string" {
        val rule: Rule<String> = empty()
        rule.check("hello") shouldBe false
    }

    "string empty() - passes for whitespace-only string" {
        val rule: Rule<String> = empty()
        rule.check("   ") shouldBe false
    }

    "string empty() - default message" {
        val rule: Rule<String> = empty()
        rule.getMessage("hello") shouldBe "Must be empty"
    }

    "string empty() - custom message" {
        val rule: Rule<String> = empty("Field must be empty")
        rule.getMessage("hello") shouldBe "Field must be empty"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: notEmpty
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string notEmpty() - passes for non-empty string" {
        val rule: Rule<String> = notEmpty()
        rule.check("hello") shouldBe true
    }

    "string notEmpty() - fails for empty string" {
        val rule: Rule<String> = notEmpty()
        rule.check("") shouldBe false
    }

    "string notEmpty() - passes for whitespace-only string" {
        val rule: Rule<String> = notEmpty()
        rule.check("   ") shouldBe true
    }

    "string notEmpty() - default message" {
        val rule: Rule<String> = notEmpty()
        rule.getMessage("") shouldBe "Must not be empty"
    }

    "string notEmpty() - custom message" {
        val rule: Rule<String> = notEmpty("Please enter a value")
        rule.getMessage("") shouldBe "Please enter a value"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: blank
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string blank() - passes for empty string" {
        val rule: Rule<String> = blank()
        rule.check("") shouldBe true
    }

    "string blank() - passes for whitespace-only string" {
        val rule: Rule<String> = blank()
        rule.check("   ") shouldBe true
    }

    "string blank() - fails for non-blank string" {
        val rule: Rule<String> = blank()
        rule.check("hello") shouldBe false
    }

    "string blank() - default message" {
        val rule: Rule<String> = blank()
        rule.getMessage("hello") shouldBe "Must be blank"
    }

    "string blank() - custom message" {
        val rule: Rule<String> = blank("Should be blank")
        rule.getMessage("hello") shouldBe "Should be blank"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: notBlank
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string notBlank() - passes for non-blank string" {
        val rule: Rule<String> = notBlank()
        rule.check("hello") shouldBe true
    }

    "string notBlank() - fails for empty string" {
        val rule: Rule<String> = notBlank()
        rule.check("") shouldBe false
    }

    "string notBlank() - fails for whitespace-only string" {
        val rule: Rule<String> = notBlank()
        rule.check("   ") shouldBe false
    }

    "string notBlank() - default message" {
        val rule: Rule<String> = notBlank()
        rule.getMessage("") shouldBe "Must not be blank"
    }

    "string notBlank() - custom message" {
        val rule: Rule<String> = notBlank("Cannot be blank")
        rule.getMessage("") shouldBe "Cannot be blank"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: minLength
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string minLength() - passes when length equals minimum" {
        val rule: Rule<String> = minLength(3)
        rule.check("abc") shouldBe true
    }

    "string minLength() - passes when length exceeds minimum" {
        val rule: Rule<String> = minLength(3)
        rule.check("abcdef") shouldBe true
    }

    "string minLength() - fails when length is below minimum" {
        val rule: Rule<String> = minLength(3)
        rule.check("ab") shouldBe false
    }

    "string minLength() - fails for empty string with minLength > 0" {
        val rule: Rule<String> = minLength(1)
        rule.check("") shouldBe false
    }

    "string minLength() - passes for empty string with minLength 0" {
        val rule: Rule<String> = minLength(0)
        rule.check("") shouldBe true
    }

    "string minLength() - default message" {
        val rule: Rule<String> = minLength(5)
        rule.getMessage("ab") shouldBe "Must be at least 5 characters"
    }

    "string minLength() - custom message" {
        val rule: Rule<String> = minLength(5, "Too short")
        rule.getMessage("ab") shouldBe "Too short"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: maxLength
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string maxLength() - passes when length equals maximum" {
        val rule: Rule<String> = maxLength(5)
        rule.check("abcde") shouldBe true
    }

    "string maxLength() - passes when length is below maximum" {
        val rule: Rule<String> = maxLength(5)
        rule.check("abc") shouldBe true
    }

    "string maxLength() - fails when length exceeds maximum" {
        val rule: Rule<String> = maxLength(5)
        rule.check("abcdef") shouldBe false
    }

    "string maxLength() - passes for empty string" {
        val rule: Rule<String> = maxLength(5)
        rule.check("") shouldBe true
    }

    "string maxLength() - default message" {
        val rule: Rule<String> = maxLength(5)
        rule.getMessage("abcdef") shouldBe "Must be at most 5 characters"
    }

    "string maxLength() - custom message" {
        val rule: Rule<String> = maxLength(5, "Too long")
        rule.getMessage("abcdef") shouldBe "Too long"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: exactLength
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string exactLength() - passes when length matches exactly" {
        val rule: Rule<String> = exactLength(5)
        rule.check("abcde") shouldBe true
    }

    "string exactLength() - fails when length is shorter" {
        val rule: Rule<String> = exactLength(5)
        rule.check("abc") shouldBe false
    }

    "string exactLength() - fails when length is longer" {
        val rule: Rule<String> = exactLength(5)
        rule.check("abcdef") shouldBe false
    }

    "string exactLength() - passes for empty string with exactLength 0" {
        val rule: Rule<String> = exactLength(0)
        rule.check("") shouldBe true
    }

    "string exactLength() - default message" {
        val rule: Rule<String> = exactLength(5)
        rule.getMessage("abc") shouldBe "Must be 5 characters"
    }

    "string exactLength() - custom message" {
        val rule: Rule<String> = exactLength(5, "Exactly 5 chars needed")
        rule.getMessage("abc") shouldBe "Exactly 5 chars needed"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: validEmail
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string validEmail() - passes for valid email" {
        val rule: Rule<String> = validEmail()
        rule.check("user@example.com") shouldBe true
    }

    "string validEmail() - fails for empty string" {
        val rule: Rule<String> = validEmail()
        rule.check("") shouldBe false
    }

    "string validEmail() - fails for blank string" {
        val rule: Rule<String> = validEmail()
        rule.check("   ") shouldBe false
    }

    "string validEmail() - fails for string without @" {
        val rule: Rule<String> = validEmail()
        rule.check("userexample.com") shouldBe false
    }

    "string validEmail() - default message" {
        val rule: Rule<String> = validEmail()
        rule.getMessage("bad") shouldBe "Must be a valid email"
    }

    "string validEmail() - custom message" {
        val rule: Rule<String> = validEmail("Enter valid email")
        rule.getMessage("bad") shouldBe "Enter valid email"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // String rules: validUrlWithProtocol
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "string validUrlWithProtocol() - passes for valid https url" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.check("https://example.com") shouldBe true
    }

    "string validUrlWithProtocol() - passes for valid http url" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.check("http://example.com") shouldBe true
    }

    "string validUrlWithProtocol() - fails for url without protocol" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.check("example.com") shouldBe false
    }

    "string validUrlWithProtocol() - fails for empty string" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.check("") shouldBe false
    }

    "string validUrlWithProtocol() - fails for blank string" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.check("   ") shouldBe false
    }

    "string validUrlWithProtocol() - default message" {
        val rule: Rule<String> = validUrlWithProtocol()
        rule.getMessage("bad") shouldBe "Must be a valid url"
    }

    "string validUrlWithProtocol() - custom message" {
        val rule: Rule<String> = validUrlWithProtocol("Enter a URL")
        rule.getMessage("bad") shouldBe "Enter a URL"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Number rules: inRange
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "number inRange() - passes for value within range" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.check(5) shouldBe true
    }

    "number inRange() - passes for value at lower bound" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.check(1) shouldBe true
    }

    "number inRange() - passes for value at upper bound" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.check(10) shouldBe true
    }

    "number inRange() - fails for value below range" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.check(0) shouldBe false
    }

    "number inRange() - fails for value above range" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.check(11) shouldBe false
    }

    "number inRange() - works with doubles" {
        val rule: Rule<Double> = numberInRange(1.5, 3.5)
        rule.check(2.0) shouldBe true
        rule.check(1.0) shouldBe false
        rule.check(4.0) shouldBe false
    }

    "number inRange() - default message" {
        val rule: Rule<Int> = numberInRange(1, 10)
        rule.getMessage(0) shouldBe "Must be in range 1 .. 10"
    }

    "number inRange() - custom message" {
        val rule: Rule<Int> = numberInRange(1, 10, "Out of range")
        rule.getMessage(0) shouldBe "Out of range"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Number rules: greaterThan
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "number greaterThan() - passes for value above threshold" {
        val rule: Rule<Int> = numberGreaterThan(5)
        rule.check(6) shouldBe true
    }

    "number greaterThan() - fails for value equal to threshold" {
        val rule: Rule<Int> = numberGreaterThan(5)
        rule.check(5) shouldBe false
    }

    "number greaterThan() - fails for value below threshold" {
        val rule: Rule<Int> = numberGreaterThan(5)
        rule.check(4) shouldBe false
    }

    "number greaterThan() - works with zero boundary" {
        val rule: Rule<Int> = numberGreaterThan(0)
        rule.check(1) shouldBe true
        rule.check(0) shouldBe false
        rule.check(-1) shouldBe false
    }

    "number greaterThan() - default message" {
        val rule: Rule<Int> = numberGreaterThan(5)
        rule.getMessage(3) shouldBe "Must be greater than 5"
    }

    "number greaterThan() - custom message" {
        val rule: Rule<Int> = numberGreaterThan(5, "Too small")
        rule.getMessage(3) shouldBe "Too small"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Number rules: greaterThanOrEqual
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "number greaterThanOrEqual() - passes for value above threshold" {
        val rule: Rule<Int> = numberGreaterThanOrEqual(5)
        rule.check(6) shouldBe true
    }

    "number greaterThanOrEqual() - passes for value equal to threshold" {
        val rule: Rule<Int> = numberGreaterThanOrEqual(5)
        rule.check(5) shouldBe true
    }

    "number greaterThanOrEqual() - fails for value below threshold" {
        val rule: Rule<Int> = numberGreaterThanOrEqual(5)
        rule.check(4) shouldBe false
    }

    "number greaterThanOrEqual() - default message" {
        val rule: Rule<Int> = numberGreaterThanOrEqual(5)
        rule.getMessage(3) shouldBe "Must be greater than 5 or equal"
    }

    "number greaterThanOrEqual() - custom message" {
        val rule: Rule<Int> = numberGreaterThanOrEqual(5, "Must be >= 5")
        rule.getMessage(3) shouldBe "Must be >= 5"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Number rules: lessThan
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "number lessThan() - passes for value below threshold" {
        val rule: Rule<Int> = numberLessThan(5)
        rule.check(4) shouldBe true
    }

    "number lessThan() - fails for value equal to threshold" {
        val rule: Rule<Int> = numberLessThan(5)
        rule.check(5) shouldBe false
    }

    "number lessThan() - fails for value above threshold" {
        val rule: Rule<Int> = numberLessThan(5)
        rule.check(6) shouldBe false
    }

    "number lessThan() - works with negative threshold" {
        val rule: Rule<Int> = numberLessThan(-1)
        rule.check(-2) shouldBe true
        rule.check(-1) shouldBe false
        rule.check(0) shouldBe false
    }

    "number lessThan() - default message" {
        val rule: Rule<Int> = numberLessThan(5)
        rule.getMessage(6) shouldBe "Must be less than 5"
    }

    "number lessThan() - custom message" {
        val rule: Rule<Int> = numberLessThan(5, "Too large")
        rule.getMessage(6) shouldBe "Too large"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Number rules: lessThanOrEqual
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "number lessThanOrEqual() - passes for value below threshold" {
        val rule: Rule<Int> = numberLessThanOrEqual(5)
        rule.check(4) shouldBe true
    }

    "number lessThanOrEqual() - passes for value equal to threshold" {
        val rule: Rule<Int> = numberLessThanOrEqual(5)
        rule.check(5) shouldBe true
    }

    "number lessThanOrEqual() - fails for value above threshold" {
        val rule: Rule<Int> = numberLessThanOrEqual(5)
        rule.check(6) shouldBe false
    }

    "number lessThanOrEqual() - default message" {
        val rule: Rule<Int> = numberLessThanOrEqual(5)
        rule.getMessage(6) shouldBe "Must be less than 5 or equal"
    }

    "number lessThanOrEqual() - custom message" {
        val rule: Rule<Int> = numberLessThanOrEqual(5, "Must be <= 5")
        rule.getMessage(6) shouldBe "Must be <= 5"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Comparable rules: inRange
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "comparable inRange() - passes for value within range" {
        val rule: Rule<String> = comparableInRange("b", "d") { "Out of range" }
        rule.check("c") shouldBe true
    }

    "comparable inRange() - passes for value at lower bound" {
        val rule: Rule<String> = comparableInRange("b", "d") { "Out of range" }
        rule.check("b") shouldBe true
    }

    "comparable inRange() - passes for value at upper bound" {
        val rule: Rule<String> = comparableInRange("b", "d") { "Out of range" }
        rule.check("d") shouldBe true
    }

    "comparable inRange() - fails for value below range" {
        val rule: Rule<String> = comparableInRange("b", "d") { "Out of range" }
        rule.check("a") shouldBe false
    }

    "comparable inRange() - fails for value above range" {
        val rule: Rule<String> = comparableInRange("b", "d") { "Out of range" }
        rule.check("e") shouldBe false
    }

    "comparable inRange() - works with Int comparable" {
        val rule: Rule<Int> = comparableInRange(1, 10) { "Not in range" }
        rule.check(5) shouldBe true
        rule.check(0) shouldBe false
        rule.check(11) shouldBe false
    }

    "comparable inRange() - message" {
        val rule: Rule<Int> = comparableInRange(1, 10) { "Value $it is out of range" }
        rule.getMessage(0) shouldBe "Value 0 is out of range"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Comparable rules: greaterThan
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "comparable greaterThan() - passes for value above threshold" {
        val rule: Rule<Int> = comparableGreaterThan(5) { "Too small" }
        rule.check(6) shouldBe true
    }

    "comparable greaterThan() - fails for value equal to threshold" {
        val rule: Rule<Int> = comparableGreaterThan(5) { "Too small" }
        rule.check(5) shouldBe false
    }

    "comparable greaterThan() - fails for value below threshold" {
        val rule: Rule<Int> = comparableGreaterThan(5) { "Too small" }
        rule.check(4) shouldBe false
    }

    "comparable greaterThan() - works with string comparison" {
        val rule: Rule<String> = comparableGreaterThan("b") { "Must be after b" }
        rule.check("c") shouldBe true
        rule.check("b") shouldBe false
        rule.check("a") shouldBe false
    }

    "comparable greaterThan() - message" {
        val rule: Rule<Int> = comparableGreaterThan(5) { "Value $it is too small" }
        rule.getMessage(3) shouldBe "Value 3 is too small"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Comparable rules: greaterThanOrEqual
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "comparable greaterThanOrEqual() - passes for value above threshold" {
        val rule: Rule<Int> = comparableGreaterThanOrEqual(5) { "Too small" }
        rule.check(6) shouldBe true
    }

    "comparable greaterThanOrEqual() - passes for value equal to threshold" {
        val rule: Rule<Int> = comparableGreaterThanOrEqual(5) { "Too small" }
        rule.check(5) shouldBe true
    }

    "comparable greaterThanOrEqual() - fails for value below threshold" {
        val rule: Rule<Int> = comparableGreaterThanOrEqual(5) { "Too small" }
        rule.check(4) shouldBe false
    }

    "comparable greaterThanOrEqual() - message" {
        val rule: Rule<Int> = comparableGreaterThanOrEqual(5) { "Must be >= 5, got $it" }
        rule.getMessage(3) shouldBe "Must be >= 5, got 3"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Comparable rules: lessThan
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "comparable lessThan() - passes for value below threshold" {
        val rule: Rule<Int> = comparableLessThan(5) { "Too big" }
        rule.check(4) shouldBe true
    }

    "comparable lessThan() - fails for value equal to threshold" {
        val rule: Rule<Int> = comparableLessThan(5) { "Too big" }
        rule.check(5) shouldBe false
    }

    "comparable lessThan() - fails for value above threshold" {
        val rule: Rule<Int> = comparableLessThan(5) { "Too big" }
        rule.check(6) shouldBe false
    }

    "comparable lessThan() - works with string comparison" {
        val rule: Rule<String> = comparableLessThan("d") { "Must be before d" }
        rule.check("c") shouldBe true
        rule.check("d") shouldBe false
        rule.check("e") shouldBe false
    }

    "comparable lessThan() - message" {
        val rule: Rule<Int> = comparableLessThan(5) { "Value $it is too big" }
        rule.getMessage(7) shouldBe "Value 7 is too big"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Comparable rules: lessThanOrEqual
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "comparable lessThanOrEqual() - passes for value below threshold" {
        val rule: Rule<Int> = comparableLessThanOrEqual(5) { "Too big" }
        rule.check(4) shouldBe true
    }

    "comparable lessThanOrEqual() - passes for value equal to threshold" {
        val rule: Rule<Int> = comparableLessThanOrEqual(5) { "Too big" }
        rule.check(5) shouldBe true
    }

    "comparable lessThanOrEqual() - fails for value above threshold" {
        val rule: Rule<Int> = comparableLessThanOrEqual(5) { "Too big" }
        rule.check(6) shouldBe false
    }

    "comparable lessThanOrEqual() - message" {
        val rule: Rule<Int> = comparableLessThanOrEqual(5) { "Must be <= 5, got $it" }
        rule.getMessage(7) shouldBe "Must be <= 5, got 7"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Collection rules: notEmpty
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "collection notEmpty() - passes for non-empty list" {
        val rule: Rule<List<Int>> = collectionNotEmpty()
        rule.check(listOf(1, 2, 3)) shouldBe true
    }

    "collection notEmpty() - fails for empty list" {
        val rule: Rule<List<Int>> = collectionNotEmpty()
        rule.check(emptyList()) shouldBe false
    }

    "collection notEmpty() - passes for single-element list" {
        val rule: Rule<List<String>> = collectionNotEmpty()
        rule.check(listOf("a")) shouldBe true
    }

    "collection notEmpty() - works with set" {
        val rule: Rule<Set<Int>> = collectionNotEmpty()
        rule.check(setOf(1)) shouldBe true
        rule.check(emptySet()) shouldBe false
    }

    "collection notEmpty() - default message" {
        val rule: Rule<List<Int>> = collectionNotEmpty()
        rule.getMessage(emptyList()) shouldBe "Must not be empty"
    }

    "collection notEmpty() - custom message" {
        val rule: Rule<List<Int>> = collectionNotEmpty("Add at least one item")
        rule.getMessage(emptyList()) shouldBe "Add at least one item"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Collection rules: minCount
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "collection minCount() - passes when size equals minimum" {
        val rule: Rule<List<Int>> = minCount(2)
        rule.check(listOf(1, 2)) shouldBe true
    }

    "collection minCount() - passes when size exceeds minimum" {
        val rule: Rule<List<Int>> = minCount(2)
        rule.check(listOf(1, 2, 3)) shouldBe true
    }

    "collection minCount() - fails when size is below minimum" {
        val rule: Rule<List<Int>> = minCount(2)
        rule.check(listOf(1)) shouldBe false
    }

    "collection minCount() - fails for empty list with minCount > 0" {
        val rule: Rule<List<Int>> = minCount(1)
        rule.check(emptyList()) shouldBe false
    }

    "collection minCount() - default message" {
        val rule: Rule<List<Int>> = minCount(3)
        rule.getMessage(listOf(1)) shouldBe "Must have at least 3 items"
    }

    "collection minCount() - custom message" {
        val rule: Rule<List<Int>> = minCount(3, "Need more items")
        rule.getMessage(listOf(1)) shouldBe "Need more items"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Collection rules: maxCount
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "collection maxCount() - default message" {
        val rule: Rule<List<Int>> = maxCount(3)
        rule.getMessage(listOf(1, 2, 3, 4)) shouldBe "Must have at most 3 items"
    }

    "collection maxCount() - custom message" {
        val rule: Rule<List<Int>> = maxCount(3, "Too many items")
        rule.getMessage(listOf(1, 2, 3, 4)) shouldBe "Too many items"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Collection rules: exactCount
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "collection exactCount() - passes when size matches exactly" {
        val rule: Rule<List<Int>> = exactCount(3)
        rule.check(listOf(1, 2, 3)) shouldBe true
    }

    "collection exactCount() - default message" {
        val rule: Rule<List<Int>> = exactCount(3)
        rule.getMessage(listOf(1, 2)) shouldBe "Must have 3 items"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: nonNull
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic nonNull() - passes for non-null value" {
        val rule: Rule<String?> = nonNull()
        rule.check("hello") shouldBe true
    }

    "generic nonNull() - fails for null value" {
        val rule: Rule<String?> = nonNull()
        rule.check(null) shouldBe false
    }

    "generic nonNull() - passes for zero" {
        val rule: Rule<Int?> = nonNull()
        rule.check(0) shouldBe true
    }

    "generic nonNull() - passes for empty string" {
        val rule: Rule<String?> = nonNull()
        rule.check("") shouldBe true
    }

    "generic nonNull() - default message" {
        val rule: Rule<String?> = nonNull()
        rule.getMessage(null) shouldBe "Must not be empty"
    }

    "generic nonNull() - custom message" {
        val rule: Rule<String?> = nonNull("Required field")
        rule.getMessage(null) shouldBe "Required field"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: nullOrElse
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic nullOrElse() - passes for null" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nullOrElse(inner)
        rule.check(null) shouldBe true
    }

    "generic nullOrElse() - passes when inner rule passes" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nullOrElse(inner)
        rule.check("hello") shouldBe true
    }

    "generic nullOrElse() - fails when inner rule fails on non-null value" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nullOrElse(inner)
        rule.check("") shouldBe false
    }

    "generic nullOrElse() - message for null value" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nullOrElse(inner)
        rule.getMessage(null) shouldBe "Invalid input"
    }

    "generic nullOrElse() - message delegates to inner for non-null" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nullOrElse(inner)
        rule.getMessage("") shouldBe "Must not be blank"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: nonNullAnd
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic nonNullAnd() - passes for non-null value that passes inner rule" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nonNullAnd(inner)
        rule.check("hello") shouldBe true
    }

    "generic nonNullAnd() - fails for null" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nonNullAnd(inner)
        rule.check(null) shouldBe false
    }

    "generic nonNullAnd() - fails when inner rule fails" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nonNullAnd(inner)
        rule.check("") shouldBe false
    }

    "generic nonNullAnd() - message for null" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nonNullAnd(inner)
        rule.getMessage(null) shouldBe "Must not be empty"
    }

    "generic nonNullAnd() - message delegates to inner for non-null" {
        val inner: Rule<String> = notBlank()
        val rule: Rule<String?> = nonNullAnd(inner)
        rule.getMessage("") shouldBe "Must not be blank"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: equalTo
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic equalTo() - passes when value equals comparison" {
        val rule: Rule<String> = equalTo("hello")
        rule.check("hello") shouldBe true
    }

    "generic equalTo() - fails when value differs" {
        val rule: Rule<String> = equalTo("hello")
        rule.check("world") shouldBe false
    }

    "generic equalTo() - works with integers" {
        val rule: Rule<Int> = equalTo(42)
        rule.check(42) shouldBe true
        rule.check(0) shouldBe false
    }

    "generic equalTo() - with lambda comparison" {
        var expected = "first"
        val rule: Rule<String> = equalTo({ expected })
        rule.check("first") shouldBe true
        expected = "second"
        rule.check("first") shouldBe false
        rule.check("second") shouldBe true
    }

    "generic equalTo() - custom message" {
        val rule: Rule<String> = equalTo("hello", "Must match")
        rule.getMessage("world") shouldBe "Must match"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: notEqualTo
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic notEqualTo() - passes when value differs" {
        val rule: Rule<String> = notEqualTo("hello")
        rule.check("world") shouldBe true
    }

    "generic notEqualTo() - fails when value equals comparison" {
        val rule: Rule<String> = notEqualTo("hello")
        rule.check("hello") shouldBe false
    }

    "generic notEqualTo() - works with integers" {
        val rule: Rule<Int> = notEqualTo(0)
        rule.check(1) shouldBe true
        rule.check(0) shouldBe false
    }

    "generic notEqualTo() - custom message" {
        val rule: Rule<String> = notEqualTo("hello", "Cannot use this value")
        rule.getMessage("hello") shouldBe "Cannot use this value"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: anyOf
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic anyOf() - passes when value is in the collection" {
        val rule: Rule<String> = anyOf(listOf("a", "b", "c"))
        rule.check("b") shouldBe true
    }

    "generic anyOf() - fails when value is not in the collection" {
        val rule: Rule<String> = anyOf(listOf("a", "b", "c"))
        rule.check("d") shouldBe false
    }

    "generic anyOf() - with empty collection always fails" {
        val rule: Rule<String> = anyOf(emptyList())
        rule.check("anything") shouldBe false
    }

    "generic anyOf() - with lambda collection" {
        var allowed = listOf("x", "y")
        val rule: Rule<String> = anyOf({ allowed }) { "Not allowed" }
        rule.check("x") shouldBe true
        rule.check("z") shouldBe false
        allowed = listOf("z")
        rule.check("z") shouldBe true
        rule.check("x") shouldBe false
    }

    "generic anyOf() - default message" {
        val rule: Rule<String> = anyOf(listOf("a", "b"))
        rule.getMessage("c") shouldBe "Must be a valid input"
    }

    "generic anyOf() - custom message" {
        val rule: Rule<String> = anyOf(listOf("a", "b"), "Pick a or b")
        rule.getMessage("c") shouldBe "Pick a or b"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: noneOf
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic noneOf() - passes when value is not in the collection" {
        val rule: Rule<String> = noneOf(listOf("a", "b", "c"))
        rule.check("d") shouldBe true
    }

    "generic noneOf() - fails when value is in the collection" {
        val rule: Rule<String> = noneOf(listOf("a", "b", "c"))
        rule.check("b") shouldBe false
    }

    "generic noneOf() - with empty collection always passes" {
        val rule: Rule<String> = noneOf(emptyList())
        rule.check("anything") shouldBe true
    }

    "generic noneOf() - with lambda collection" {
        var forbidden = listOf("x", "y")
        val rule: Rule<String> = noneOf({ forbidden }) { "Forbidden" }
        rule.check("z") shouldBe true
        rule.check("x") shouldBe false
        forbidden = listOf("z")
        rule.check("x") shouldBe true
        rule.check("z") shouldBe false
    }

    "generic noneOf() - default message" {
        val rule: Rule<String> = noneOf(listOf("a", "b"))
        rule.getMessage("a") shouldBe "Must be a valid input"
    }

    "generic noneOf() - custom message" {
        val rule: Rule<String> = noneOf(listOf("a", "b"), "Cannot use a or b")
        rule.getMessage("a") shouldBe "Cannot use a or b"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: given
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic given() - passes when predicate returns true" {
        val rule: Rule<Int> = given({ it > 0 })
        rule.check(5) shouldBe true
    }

    "generic given() - fails when predicate returns false" {
        val rule: Rule<Int> = given({ it > 0 })
        rule.check(-1) shouldBe false
    }

    "generic given() - with string predicate" {
        val rule: Rule<String> = given({ it.startsWith("A") })
        rule.check("Apple") shouldBe true
        rule.check("Banana") shouldBe false
    }

    "generic given() - default message" {
        val rule: Rule<Int> = given({ it > 0 })
        rule.getMessage(-1) shouldBe "Must be a valid input"
    }

    "generic given() - custom message lambda" {
        val rule: Rule<Int> = given(
            check = { it > 0 },
            message = { "Value $it must be positive" },
        )
        rule.getMessage(-3) shouldBe "Value -3 must be positive"
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: anyRuleOf
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic anyRuleOf() - passes when first rule passes" {
        val rule: Rule<String> = anyRuleOf(
            notBlank(),
            minLength(5),
        )
        rule.check("hi") shouldBe true
    }

    "generic anyRuleOf() - passes when second rule passes" {
        val rule: Rule<Int> = anyRuleOf(
            numberGreaterThan(10),
            numberLessThan(0),
        )
        rule.check(-5) shouldBe true
    }

    "generic anyRuleOf() - passes when all rules pass" {
        val rule: Rule<String> = anyRuleOf(
            notBlank(),
            minLength(1),
        )
        rule.check("hello") shouldBe true
    }

    "generic anyRuleOf() - fails when no rules pass" {
        val rule: Rule<String> = anyRuleOf(
            notBlank(),
            minLength(5),
        )
        rule.check("") shouldBe false
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Generic rules: allRulesOf
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "generic allRulesOf() - passes when all rules pass" {
        val rule: Rule<String> = allRulesOf(
            notBlank(),
            minLength(3),
        )
        rule.check("hello") shouldBe true
    }

    "generic allRulesOf() - fails when first rule fails" {
        val rule: Rule<String> = allRulesOf(
            notBlank(),
            minLength(3),
        )
        rule.check("") shouldBe false
    }

    "generic allRulesOf() - fails when second rule fails" {
        val rule: Rule<String> = allRulesOf(
            notBlank(),
            minLength(5),
        )
        rule.check("hi") shouldBe false
    }

    "generic allRulesOf() - fails when all rules fail" {
        val rule: Rule<String> = allRulesOf(
            notBlank(),
            minLength(5),
        )
        rule.check("") shouldBe false
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////
    // Rule composition: or
    // ////////////////////////////////////////////////////////////////////////////////////////////

    "or composition - passes when first rule passes" {
        val rule: Rule<String> = empty<String>() or notBlank()
        rule.check("") shouldBe true
    }

    "or composition - passes when second rule passes" {
        val rule: Rule<String> = empty<String>() or notBlank()
        rule.check("hello") shouldBe true
    }

    "or composition - fails when neither rule passes" {
        val rule: Rule<String> = empty<String>() or minLength(5)
        rule.check("hi") shouldBe false
    }

    "or composition - message combines both rule messages" {
        val rule: Rule<String> = empty<String>() or minLength(5)
        val msg = rule.getMessage("hi")
        msg shouldBe "Must be empty or Must be at least 5 characters"
    }

    "or composition - passes when both rules pass" {
        val rule: Rule<String> = notBlank<String>() or notEmpty()
        rule.check("hello") shouldBe true
    }
})
