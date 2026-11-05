package io.peekandpoke.funktor.auth.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

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

    "real names in other scripts, and emoji built with joiners, are accepted" {
        val accepted = listOf(
            "José Zoë",
            text(0x674E, 0x5C0F, 0x9F99), // Chinese
            text(0x0645, 0x06CC, 0x200C, 0x062E, 0x0648, 0x0627, 0x0645), // Persian, with a zero-width non-joiner
            text(0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467), // a family emoji, zero-width joiners
            text(0x1F44B, 0x1F3FD), // a wave with a skin tone
            "Ada " + text(0x2764, 0xFE0F), // a heart with a variation selector
        )

        for (name in accepted) {
            withClue("accepted: ${named(name)}") { AuthDisplayName.normalizeOrNull(name) shouldBe name }
        }
    }

    // Each sits INSIDE the name: at an end, `trim()` removes the ones that count as whitespace, which is correct.
    "control, format, tag and blank characters are refused" {
        val refused = mapOf(
            "a line break" to "Call" + text(0x0A) + "support",
            "a tab" to "Ada" + text(0x09) + "L",
            "a C1 control" to "Ada" + text(0x85) + "L",
            "a line separator" to "Ada" + text(0x2028) + "L",
            "a paragraph separator" to "Ada" + text(0x2029) + "L",
            "a right-to-left override" to "Ada" + text(0x202E) + "evol",
            "a bidi isolate" to "Ada" + text(0x2066) + "L",
            "a right-to-left mark" to "Ada" + text(0x200F) + "L",
            "the Arabic letter mark" to "Ada" + text(0x061C) + "L",
            "a zero-width space" to "Ad" + text(0x200B) + "min",
            "a word joiner" to "Ada" + text(0x2060) + "L",
            "a byte-order mark" to "Ada" + text(0xFEFF) + "L",
            "a soft hyphen" to "Ad" + text(0x00AD) + "min",
            "tag characters (hidden text)" to "Bob" + text(0xE0069, 0xE0067, 0xE006E),
            "a Hangul filler" to "A" + text(0x3164) + "B",
            "the halfwidth Hangul filler" to "A" + text(0xFFA0) + "B",
            "a Hangul choseong filler" to "A" + text(0x115F) + "B",
            "a Hangul jungseong filler" to "A" + text(0x1160) + "B",
            "the blank braille cell" to "A" + text(0x2800) + "B",
        )

        for ((what, name) in refused) {
            withClue("refused: $what") { AuthDisplayName.normalizeOrNull(name).shouldBeNull() }
        }
    }
})
