package io.peekandpoke.ultra.security.password

import com.password4j.types.Argon2
import com.password4j.types.Bcrypt
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Regression: [PasswordHasher.Hash] packs the hash as "id:salt:hash". The salt must never contain
 * the ':' delimiter, otherwise the round-trip corrupts the parsed hash — an intermittent,
 * salt-dependent "Invalid hashed value" on check.
 *
 * Two defences, both verified here:
 * - the salt is base64-encoded at the source (Argon2 / Bcrypt now match PBKDF2), so no field can
 *   contain ':' by construction;
 * - [PasswordHasher.Hash.fromString] parses positionally (id before the first ':', hash after the
 *   last ':'), tolerating any ':' in a legacy/raw salt.
 */
class PasswordHashRoundTripSpec : StringSpec({

    // Cheap params — we are testing the string round-trip, not the KDF cost.
    val hashers = listOf(
        Argon2PasswordHasher(memory = 64, iterations = 1, parallelism = 1, outputLength = 32, type = Argon2.ID),
        BcryptPasswordHasher(logRounds = 4, version = Bcrypt.Y),
        PBKDF2WithHmacSHA256PasswordHasher(iterations = 1000, keyLength = 256),
    )

    for (hasher in hashers) {
        "${hasher.id}: hashAsString/check round-trips across many random salts" {
            repeat(200) { i ->
                val stored = hasher.hashAsString("S3cret123!")
                withClue("iteration $i, stored='$stored'") {
                    hasher.check("S3cret123!", stored) shouldBe true
                }
            }
        }

        "${hasher.id}: every serialized field is free of the ':' delimiter" {
            repeat(200) {
                val h = PasswordHasher.Hash.fromString(hasher.hashAsString("S3cret123!"))
                withClue("id='${h.id}' salt='${h.salt}' hash='${h.hash}'") {
                    h.id shouldNotContain ":"
                    h.salt shouldNotContain ":"
                    h.hash shouldNotContain ":"
                }
            }
        }
    }

    "Hash.fromString still round-trips a legacy salt that contains ':'" {
        val original = PasswordHasher.Hash(id = "argon2-x", salt = "aa:bb:cc", hash = "the-encoded-hash")

        PasswordHasher.Hash.fromString(original.asString()) shouldBe original
    }
})
