package de.peekandpoke.ultra.security.password

import com.password4j.types.Bcrypt
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BcryptPasswordHasherSpec : FreeSpec() {

    init {
        "Password should be hashed without collisions and be checkable" {

            // We use the minimum rounds for testing to ensure the test runs fast
            val hasher = BcryptPasswordHasher(logRounds = 4, version = Bcrypt.Y)

            listOf(
                "a",
                "abc",
                "a-rather-long-and-secure_password"
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
            val hasher = BcryptPasswordHasher.l12y

            hasher.check("a-password", null as String?) shouldBe false
        }

        "Checking a null password against a hash must fail" {
            val hasher = BcryptPasswordHasher.l13y
            val hash = hasher.hash("a-password")

            hasher.check(null, hash) shouldBe false
        }
    }
}
