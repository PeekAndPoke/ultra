package io.peekandpoke.ultra.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.util.Base64

/** Random secrets (ULTRA-00067): lengths and alphabets honoured, no repeats, no bias. */
class SecretGeneratorSpec : StringSpec({

    val subject = SecretGenerator.default

    "a token is URL-safe base64 of exactly the bytes asked for, 32 by default" {
        listOf(16, 32, 64).forEach { bytes ->
            val token = subject.token(bytes)

            token shouldMatch Regex("[A-Za-z0-9_-]+")
            Base64.getUrlDecoder().decode(token).size shouldBe bytes
        }

        Base64.getUrlDecoder().decode(subject.token()).size shouldBe 32
    }

    "a token below 128 bits is refused" {
        shouldThrow<IllegalArgumentException> { subject.token(15) }
    }

    "ten thousand tokens and temp passwords never repeat" {
        (1..10_000).map { subject.token() }.toSet().size shouldBe 10_000
        (1..10_000).map { subject.tempPassword() }.toSet().size shouldBe 10_000
    }

    "a temp password has the length asked for and only characters from the alphabet, no look-alikes" {
        val password = subject.tempPassword(length = 24)

        password.length shouldBe 24
        password.forEach { it shouldBeIn SecretGenerator.TEMP_PASSWORD_ALPHABET.toList() }
        SecretGenerator.TEMP_PASSWORD_ALPHABET.none { it in "0O1Il" } shouldBe true
    }

    "every character of the alphabet is equally likely: no modulo bias" {
        // 65 characters, 650,000 draws: about 10,000 each. A modulo over a byte (256 = 3 * 65 + 61) would make the
        // first 61 characters 4/3 times likelier than the last 4, which would land near 7,600 — far outside the band.
        val alphabet = SecretGenerator.TEMP_PASSWORD_ALPHABET
        val counts = subject.string(length = 650_000, alphabet = alphabet).groupingBy { it }.eachCount()

        counts.keys shouldBe alphabet.toSet()
        counts.forEach { (char, count) ->
            withClue(char) {
                count shouldBeGreaterThan 9_400
                count shouldBeLessThan 10_600
            }
        }
    }

    "an alphabet that repeats a character, has fewer than two, or leaves the BMP is refused" {
        shouldThrow<IllegalArgumentException> { subject.string(8, "aab") }
        shouldThrow<IllegalArgumentException> { subject.string(8, "a") }
        shouldThrow<IllegalArgumentException> { subject.string(8, "ab" + String(Character.toChars(0x1D538))) }
    }

    "a temp password is drawn again until the policy accepts it; one no draw can meet throws" {
        repeat(200) {
            subject.tempPassword(length = 32, alphabet = "ab12") { it.first().isDigit() }.first().isDigit() shouldBe true
        }

        shouldThrow<IllegalStateException> { subject.tempPassword(length = 64, alphabet = "ab") { it.contains('z') } }
    }

    "a temp password under 64 bits is refused: the one vetted place hands out nothing guessable" {
        shouldThrow<IllegalArgumentException> { subject.tempPassword(length = 6, alphabet = SecretGenerator.UNAMBIGUOUS_DIGITS) }
        shouldThrow<IllegalArgumentException> { subject.tempPassword(length = 10) } // 60 bits

        subject.tempPassword(length = 11).length shouldBe 11 // 66 bits
    }
})
