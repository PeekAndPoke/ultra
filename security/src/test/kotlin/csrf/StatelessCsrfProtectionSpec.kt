package de.peekandpoke.ultra.security.csrf

import de.peekandpoke.ultra.common.fromBase64
import de.peekandpoke.ultra.common.toBase64
import de.peekandpoke.ultra.security.user.StaticUserRecordProvider
import de.peekandpoke.ultra.security.user.UserRecord
import de.peekandpoke.ultra.security.user.UserRecordProvider
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldMatch
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.delay

class StatelessCsrfProtectionSpec : StringSpec({

    "Token patterns" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserRecordProvider.static(userId = "USER", clientIp = "IP")
        )

        val token = subject.createToken("SALT")

        assertSoftly {
            String(token.fromBase64()) shouldMatch "[0-9]+#.+"
        }
    }

    "Validating a valid token must work" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserRecordProvider.static(userId = "USER", clientIp = "IP")
        )

        val salt = "SALT"
        val token = subject.createToken(salt)

        assertSoftly {
            subject.validateToken(salt, token) shouldBe true
        }
    }

    "Validating a valid token must work for anonymous users" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserRecordProvider.anonymous
        )

        val salt = "SALT"
        val token = subject.createToken(salt)

        assertSoftly {
            subject.validateToken(salt, token) shouldBe true
        }
    }

    "Validating a valid token must not work with a wrong salt" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, StaticUserRecordProvider(UserRecord("USER", "IP"))
        )

        val token = subject.createToken("SALT")

        assertSoftly {
            subject.validateToken("WRONG", token) shouldBe false
        }
    }

    "Token must depend on the user id" {

        val creator = StatelessCsrfProtection("secret", 1000, StaticUserRecordProvider(UserRecord("USER", "IP")))

        val validator = StatelessCsrfProtection("secret", 1000, StaticUserRecordProvider(UserRecord("X", "IP")))

        val salt = "SALT"
        val token = creator.createToken(salt)

        assertSoftly {
            validator.validateToken(salt, token) shouldBe false
        }
    }

    "Token must depend on the user ip" {

        val creator = StatelessCsrfProtection("secret", 1000, StaticUserRecordProvider(UserRecord("USER", "IP")))

        val validator = StatelessCsrfProtection("secret", 1000, StaticUserRecordProvider(UserRecord("USER", "X")))

        val salt = "SALT"
        val token = creator.createToken(salt)

        assertSoftly {
            validator.validateToken(salt, token) shouldBe false
        }
    }

    "The ttl must be part of the tokens hash" {

        val creator = StatelessCsrfProtection("secret", 1000, StaticUserRecordProvider(UserRecord("USER", "IP")))

        val salt = "SALT"
        val token = creator.createToken(salt)

        val parts = String(token.fromBase64()).split(creator.glue)

        val crafted = "${parts[0].toLong() + 1}${creator.glue}${parts[1]}".toBase64()

        assertSoftly {
            token shouldNotBe crafted

            creator.validateToken(salt, crafted) shouldBe false
        }
    }

    "Token must become invalid after the its ttl" {

        val creator = StatelessCsrfProtection("secret", 100, StaticUserRecordProvider(UserRecord("USER", "IP")))

        val salt = "SALT"
        val token = creator.createToken(salt)

        assertSoftly {
            creator.validateToken(salt, token) shouldBe true

            delay(200)

            creator.validateToken(salt, token) shouldBe false
        }
    }
})
