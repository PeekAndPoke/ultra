package de.peekandpoke.ultra.security.password

import com.password4j.types.Argon2
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class Argon2PasswordHasherSpec : StringSpec({

    "Password should be hashed without collisions and be checkable" {

        // We use the minimum parameters for testing to ensure the tests run fast.
        val hasher = Argon2PasswordHasher(
            memory = 8,          // Minimum memory in KB
            iterations = 1,      // Minimum iterations
            parallelism = 1,     // Minimum parallelism
            outputLength = 16,   // A smaller output length for tests is fine
            type = Argon2.ID,
        )

        listOf(
            "a",
            "abc",
            "a-rather-long-and-secure_password-for-argon2"
        ).forEach { password ->

            val rounds = 5
            val hashed = (1..rounds).map { hasher.hash(password) }.toSet()

            withClue("there should be $rounds different hashes for password '$password'") {
                hashed.size shouldBe rounds
            }

            withClue("validating the hashes against the correct password '$password' must work") {
                hashed.all { hasher.check(password, it) } shouldBe true
            }

            withClue("validating the hashes against a wrong password must fail for '$password'") {
                hashed.any { hasher.check("$password-wrong", it) } shouldBe false
            }
        }
    }

    "Checking a password against a null hash must fail" {
        val hasher = Argon2PasswordHasher.id_m65536i2p4o32

        hasher.check("a-password", null as String?) shouldBe false
    }

    "Checking a null password against a hash must fail" {
        val hasher = Argon2PasswordHasher.id_m65536i2p4o32
        val hash = hasher.hash("a-password")

        hasher.check(null, hash) shouldBe false
    }
})
