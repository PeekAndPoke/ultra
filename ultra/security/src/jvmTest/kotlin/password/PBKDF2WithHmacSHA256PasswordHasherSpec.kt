package io.peekandpoke.ultra.security.password

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PBKDF2WithHmacSHA256PasswordHasherSpec : StringSpec({

    "Password should be hashed without collisions" {

        val hasher = PBKDF2WithHmacSHA256PasswordHasher.i65536k256

        listOf(
            "",
            "abc",
            "super_long_password"
        ).forEach { password ->

            val rounds = 5
            val hashed = (1..rounds).map { hasher.hash(password) }.toSet()

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
    "check returns false when the stored hash or salt is not valid base64" {

        val subject = PBKDF2WithHmacSHA256PasswordHasher.i65536k256
        val valid = subject.hash("secret")

        // a corrupt or legacy row must fail closed instead of throwing out of the auth check
        subject.check("secret", valid.copy(hash = "not base64!")) shouldBe false
        subject.check("secret", valid.copy(salt = "not base64!")) shouldBe false
    }
})
