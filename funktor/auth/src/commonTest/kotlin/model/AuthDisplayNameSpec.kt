package io.peekandpoke.funktor.auth.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.auth.model.AuthDisplayName.Problem

/** The name rule runs on the page (JS) and on the server (JVM); this spec runs on both, so they cannot drift. */
class AuthDisplayNameSpec : StringSpec({

    /** [codePoints] as UTF-16, so the spec holds no raw invisible characters. */
    fun text(vararg codePoints: Int): String = buildString {
        for (cp in codePoints) {
            if (cp < 0x10000) {
                append(Char(cp))
            } else {
                val v = cp - 0x10000
                append(Char(0xD800 + (v shr 10)))
                append(Char(0xDC00 + (v and 0x3FF)))
            }
        }
    }

    fun named(name: String) = name.map { it.code.toString(16) }

    "a name is trimmed, and 1 to MAX_LENGTH characters are accepted" {
        AuthDisplayName.normalizeOrNull("  Ada Lovelace ") shouldBe "Ada Lovelace"
        AuthDisplayName.normalizeOrNull("A") shouldBe "A"
        AuthDisplayName.normalizeOrNull("x".repeat(100)) shouldBe "x".repeat(100)

        AuthDisplayName.normalizeOrNull("").shouldBeNull()
        AuthDisplayName.normalizeOrNull("   ").shouldBeNull()
        AuthDisplayName.normalizeOrNull("x".repeat(101)).shouldBeNull()
    }

    "real names in other scripts, and emoji built with joiners and variation selectors, are accepted" {
        val accepted = listOf(
            "José Zoë",
            "Zoe" + text(0x0308), // a combining diaeresis on its letter
            text(0x674E, 0x5C0F, 0x9F99), // Chinese
            text(0x845B, 0xE0100), // a CJK ideograph with its variation selector
            text(0x0645, 0x06CC, 0x200C, 0x062E, 0x0648, 0x0627, 0x0645), // Persian, with a zero-width non-joiner
            text(0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467), // a family emoji, zero-width joiners
            text(0x2764, 0xFE0F, 0x200D, 0x1F525), // a heart on fire: a joiner after a variation selector
            text(0x1F44B, 0x1F3FD), // a wave with a skin tone
            "Ada " + text(0x2764, 0xFE0F), // a heart with a variation selector
            "No " + text(0x31, 0xFE0F, 0x20E3), // a keycap: digit, variation selector, enclosing mark
            "Ada" + text(0x00A0) + "L", // a no-break space inside
            text(0x09B6, 0x09B0, 0x09A4, 0x09CD, 0x200D), // older Bengali: a word-final joiner after the virama
            "Zoe" + text(0x0301).repeat(30), // 30 marks on one letter: the stream-safe limit
        )

        for (name in accepted) {
            withClue("accepted: ${named(name)}") {
                AuthDisplayName.refusalOf(name).shouldBeNull()
                AuthDisplayName.normalizeOrNull(name) shouldBe name
            }
        }
    }

    // Each sits INSIDE the name unless the case is about an end: there, `trim()` removes what counts as whitespace.
    "control, invisible and undisplayable characters are refused, each with its reason" {
        val refused = mapOf(
            "a line break" to ("Call" + text(0x0A) + "support" to Problem.LineBreak),
            "a tab" to ("Ada" + text(0x09) + "L" to Problem.LineBreak),
            "a C1 control" to ("Ada" + text(0x85) + "L" to Problem.LineBreak),
            "a line separator" to ("Ada" + text(0x2028) + "L" to Problem.LineBreak),
            "a paragraph separator" to ("Ada" + text(0x2029) + "L" to Problem.LineBreak),
            "a right-to-left override" to ("Ada" + text(0x202E) + "evol" to Problem.Invisible),
            "a bidi isolate" to ("Ada" + text(0x2066) + "L" to Problem.Invisible),
            "a right-to-left mark" to ("Ada" + text(0x200F) + "L" to Problem.Invisible),
            "a trailing left-to-right mark" to ("Ada" + text(0x200E) to Problem.Invisible),
            "the Arabic letter mark" to ("Ada" + text(0x061C) + "L" to Problem.Invisible),
            "a zero-width space" to ("Ad" + text(0x200B) + "min" to Problem.Invisible),
            "a word joiner" to ("Ada" + text(0x2060) + "L" to Problem.Invisible),
            "a byte-order mark" to ("Ada" + text(0xFEFF) + "L" to Problem.Invisible),
            "a soft hyphen" to ("Ad" + text(0x00AD) + "min" to Problem.Invisible),
            "the Mongolian vowel separator" to ("Ada" + text(0x180E) + "L" to Problem.Invisible),
            "the combining grapheme joiner" to ("Ad" + text(0x034F) + "min" to Problem.Invisible),
            "a Khmer inherent vowel" to ("Ad" + text(0x17B4) + "min" to Problem.Invisible),
            "tag characters (hidden text)" to ("Bob" + text(0xE0069, 0xE0067, 0xE006E) to Problem.Invisible),
            "a musical format control (beyond the BMP)" to ("Ad" + text(0x1D173) + "min" to Problem.Invisible),
            "a shorthand format control (beyond the BMP)" to ("Ad" + text(0x1BCA0) + "min" to Problem.Invisible),
            "an Egyptian format control (beyond the BMP)" to ("Ad" + text(0x13430) + "min" to Problem.Invisible),
            "a run of variation selectors (hidden bytes)" to ("Bob" + text(0xE0100, 0xE0101, 0xE0102) to Problem.Invisible),
            "a variation selector after a space" to ("Ada " + text(0xFE0F) + "L" to Problem.Invisible),
            "a name of joiners only" to (text(0x200D, 0x200D) to Problem.Invisible),
            "a leading joiner" to (text(0x200D) + "Ada" to Problem.Invisible),
            "a trailing joiner" to ("Ada" + text(0x200C) to Problem.Invisible),
            "a joiner next to a space" to ("Ada" + text(0x200D) + " L" to Problem.Invisible),
            "a name of variation selectors only" to (text(0xFE0F, 0xFE0F) to Problem.Invisible),
            "a Hangul filler" to ("A" + text(0x3164) + "B" to Problem.Invisible),
            "the halfwidth Hangul filler" to ("A" + text(0xFFA0) + "B" to Problem.Invisible),
            "a Hangul choseong filler" to ("A" + text(0x115F) + "B" to Problem.Invisible),
            "a Hangul jungseong filler" to ("A" + text(0x1160) + "B" to Problem.Invisible),
            "the blank braille cell" to ("A" + text(0x2800) + "B" to Problem.Invisible),
            "a name of marks only" to (text(0x0301, 0x0301) to Problem.StartsWithMark),
            "a leading combining mark" to (text(0x0301) + "Ada" to Problem.StartsWithMark),
            "a private-use character" to ("Ada" + text(0xE000) to Problem.NotDisplayable),
            "a private-use plane character" to ("Ada" + text(0xF0000) to Problem.NotDisplayable),
            "a noncharacter" to ("Ada" + text(0xFFFE) to Problem.NotDisplayable),
            "an Arabic-block noncharacter" to ("Ada" + text(0xFDD0) to Problem.NotDisplayable),
            "a noncharacter beyond the BMP" to ("Ada" + text(0x1FFFF) to Problem.NotDisplayable),
            "a reserved default-ignorable (U+2065)" to ("Ad" + text(0x2065) + "min" to Problem.Invisible),
            "an ideographic variation selector on a Latin letter" to ("A" + text(0xE0100) + "da" to Problem.Invisible),
            "31 marks on one letter" to ("Zoe" + text(0x0301).repeat(31) to Problem.TooManyMarks),
            "an unpaired high surrogate" to ("Ada" + Char(0xD800) + "L" to Problem.NotDisplayable),
            "an unpaired low surrogate" to ("Ada" + Char(0xDC00) + "L" to Problem.NotDisplayable),
        )

        for ((what, case) in refused) {
            val (name, problem) = case
            withClue("refused: $what") {
                AuthDisplayName.normalizeOrNull(name).shouldBeNull()
                AuthDisplayName.refusalOf(name)?.problem shouldBe problem
            }
        }
    }

    "the message names the reason, the character and where it sits" {
        AuthDisplayName.refusalOf("").shouldNotBeNull().message shouldBe "Please enter your name."
        AuthDisplayName.refusalOf("x".repeat(101)).shouldNotBeNull().message shouldBe
            "Please use at most 100 characters (an emoji counts as two or more)."
        AuthDisplayName.refusalOf("Ada" + Char(0xD800)).shouldNotBeNull().message shouldContain
            "cannot be displayed (U+D800) at the end"

        AuthDisplayName.refusalOf("Ada" + text(0x200E)).shouldNotBeNull().message shouldBe
            "Your name contains an invisible character (U+200E) at the end. " +
            "It often comes along with copy and paste - please delete it, or type your name in again."
        AuthDisplayName.refusalOf(text(0x200D) + "Ada").shouldNotBeNull().message shouldContain "(U+200D) at the start"
        AuthDisplayName.refusalOf("Ad" + text(0x1D173) + "min").shouldNotBeNull().message shouldContain
            "invisible character (U+1D173)."
        AuthDisplayName.refusalOf("Ada" + text(0xE000)).shouldNotBeNull().message shouldContain
            "cannot be displayed (U+E000) at the end"
        AuthDisplayName.refusalOf(text(0x0301) + "Ada").shouldNotBeNull().message shouldContain
            "starts with an accent or mark (U+0301)"
        AuthDisplayName.refusalOf("Ada" + text(0x0A) + "L").shouldNotBeNull().message shouldContain "on one line"
    }
})
