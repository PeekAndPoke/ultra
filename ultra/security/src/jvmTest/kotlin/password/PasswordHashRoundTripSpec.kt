package io.peekandpoke.ultra.security.password

import com.password4j.types.Argon2
import com.password4j.types.Bcrypt
import io.kotest.assertions.throwables.shouldThrow
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
    val argon2 = Argon2PasswordHasher(memory = 64, iterations = 1, parallelism = 1, outputLength = 32, type = Argon2.ID)
    val bcrypt = BcryptPasswordHasher(logRounds = 4, version = Bcrypt.Y)
    val pbkdf2 = PBKDF2WithHmacSHA256PasswordHasher(iterations = 1000, keyLength = 256)

    val hashers = listOf(argon2, bcrypt, pbkdf2)

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

    // The exact original failure, end-to-end: records written BEFORE the base64-salt fix stored the
    // raw password4j salt, which could contain ':'. Argon2/Bcrypt verify against the self-describing
    // encoded hash only, so such records must still check out once the parse recovers the hash
    // positionally. (PBKDF2 is exempt: it reads the salt back, but always stored it base64.)
    for (hasher in listOf(argon2, bcrypt)) {
        "${hasher.id}: a legacy stored hash whose raw salt contains ':' still verifies" {
            val fresh = hasher.hash("S3cret123!")
            val legacyStored = fresh.copy(salt = "ra:w..sa:lt").asString()

            hasher.check("S3cret123!", legacyStored) shouldBe true
        }
    }

    // Format contract (see Hash KDoc): only the salt may contain ':'. A future hasher violating
    // this must fail loudly at write time, not silently truncate at parse time.
    "Hash.asString rejects a hasher id containing ':'" {
        shouldThrow<IllegalArgumentException> {
            PasswordHasher.Hash(id = "bad:id", salt = "salt", hash = "hash").asString()
        }
    }

    "Hash.asString rejects an encoded hash containing ':'" {
        shouldThrow<IllegalArgumentException> {
            PasswordHasher.Hash(id = "id", salt = "salt", hash = "bad:hash").asString()
        }
    }
})
