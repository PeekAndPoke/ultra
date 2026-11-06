package io.peekandpoke.ultra.security

import java.security.SecureRandom
import java.util.Base64
import kotlin.math.ln

/**
 * Random secrets from a CSPRNG (ULTRA-00067): tokens for links and invites, and temporary passwords a person can type.
 * One vetted place, so no call site rolls its own with a weak random or a biased alphabet.
 *
 * Every choice is uniform: bytes come straight from [SecureRandom], and characters are picked with
 * `SecureRandom.nextInt(bound)`, which rejects instead of taking a modulo, so no character is likelier than another.
 */
class SecretGenerator(private val random: SecureRandom = SecureRandom()) {

    companion object {
        /** A shared instance on a plain [SecureRandom]: a CSPRNG that does not block, unlike `getInstanceStrong()`. */
        val default: SecretGenerator = SecretGenerator()

        /** Upper-case letters without `I`, `O`: the worst look-alikes go (I/l/1, O/0); others such as S/5 stay. */
        const val UNAMBIGUOUS_LETTERS_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"

        /** Lower-case letters without `l`. */
        const val UNAMBIGUOUS_LETTERS_LOWER = "abcdefghijkmnopqrstuvwxyz"

        /** Digits without `0`, `1`. */
        const val UNAMBIGUOUS_DIGITS = "23456789"

        /** Symbols, for policies that demand one. Each stops a double-click selection: show such a password monospaced, in a select-all box. */
        const val UNAMBIGUOUS_SYMBOLS = "!#%+-=?@"

        /** The default temp-password alphabet: 65 characters, about 6.02 bits each. */
        const val TEMP_PASSWORD_ALPHABET =
            UNAMBIGUOUS_LETTERS_UPPER + UNAMBIGUOUS_LETTERS_LOWER + UNAMBIGUOUS_DIGITS + UNAMBIGUOUS_SYMBOLS

        private const val MAX_DRAWS = 1000

        /** The least a temp password may hold: below it, `tempPassword` refuses (a token's floor is 128 bits). */
        const val MIN_TEMP_PASSWORD_BITS = 64.0
    }

    /** [byteLength] random bytes. */
    fun bytes(byteLength: Int): ByteArray {
        require(byteLength > 0) { "byteLength must be positive" }

        return ByteArray(byteLength).also { random.nextBytes(it) }
    }

    /**
     * A URL-safe token: [byteLength] random bytes as base64url without padding, `8 * byteLength` bits of entropy. The
     * default 32 bytes (256 bits) is what a link or an invite needs; [byteLength] below 16 is refused.
     */
    fun token(byteLength: Int = 32): String {
        require(byteLength >= 16) { "a token needs at least 16 random bytes (128 bits)" }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(byteLength))
    }

    /** [length] characters, each picked uniformly from [alphabet]: `length * log2(alphabet.length)` bits. */
    fun string(length: Int, alphabet: String): String {
        require(length > 0) { "length must be positive" }
        require(alphabet.length >= 2) { "the alphabet needs two characters at least" }
        // Indexing picks UTF-16 units: a character outside the BMP would come out as a lone surrogate.
        require(alphabet.none(Char::isSurrogate)) { "the alphabet must hold BMP characters only" }
        require(alphabet.toSet().size == alphabet.length) { "the alphabet must not repeat a character: that biases it" }

        return buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    /**
     * A temporary password a person can type: [length] characters from [alphabet] (default: [TEMP_PASSWORD_ALPHABET],
     * 16 characters, about 96 bits), drawn again until [accept] takes it, e.g. a realm's `PasswordPolicy::matches`. A
     * redraw keeps every accepted password equally likely. Refuses fewer than [MIN_TEMP_PASSWORD_BITS] bits
     * (`length * log2(alphabet size)`). Throws when [accept] refuses [MAX_DRAWS] draws in a row: a policy this length
     * and alphabet cannot, or almost never, meet (e.g. a minimum length above [length]: pass a longer one).
     */
    fun tempPassword(
        length: Int = 16,
        alphabet: String = TEMP_PASSWORD_ALPHABET,
        accept: (String) -> Boolean = { true },
    ): String {
        val bits = length * ln(alphabet.toSet().size.toDouble()) / ln(2.0)
        require(bits >= MIN_TEMP_PASSWORD_BITS) {
            "a temp password of $length from ${alphabet.length} characters holds ${bits.toInt()} bits, " +
                    "under $MIN_TEMP_PASSWORD_BITS: make it longer"
        }

        repeat(MAX_DRAWS) {
            val candidate = string(length, alphabet)
            if (accept(candidate)) return candidate
        }

        error("No temp password of length $length from this alphabet met the policy in $MAX_DRAWS draws")
    }
}
