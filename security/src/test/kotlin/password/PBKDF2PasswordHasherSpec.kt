package de.peekandpoke.ultra.security.password

import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PBKDF2PasswordHasherSpec : StringSpec({

    "Password should be hashed without collisions" {

        val hasher = PBKDF2PasswordHasher()

        listOf(
            "",
            "abc",
            "super_long_password"
        ).forEach { password ->

            val rounds = 5
            val hashed = (1..rounds).map { hasher.hash(password) }.toSet()

            assertSoftly {
                withClue("there should be $rounds different hashes") {
                    hashed.size shouldBe rounds
                }

                withClue("validating the hashes against the correct password must work") {
                    hashed.all { hasher.check(password, it) } shouldBe true
                }

                withClue("validating the hashes against a wrong password must work") {
                    hashed.any { hasher.check(password + "a", it) } shouldBe false
                }
            }
        }
    }
})
