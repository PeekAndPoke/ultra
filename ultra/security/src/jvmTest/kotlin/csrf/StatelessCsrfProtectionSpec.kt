package io.peekandpoke.ultra.security.csrf

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.peekandpoke.ultra.common.sha384
import io.peekandpoke.ultra.common.fromBase64
import io.peekandpoke.ultra.common.toBase64
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.security.user.UserRecord
import kotlinx.coroutines.delay

class StatelessCsrfProtectionSpec : StringSpec({

    "Token patterns" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val token = subject.createToken("SALT")

        String(token.fromBase64()) shouldMatch "[0-9]+#.+"
    }

    "Validating a valid token must work" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val salt = "SALT"
        val token = subject.createToken(salt)

        subject.validateToken(salt, token) shouldBe true
    }

    "Validating a valid token must work for anonymous users" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.anonymous
        )

        val salt = "SALT"
        val token = subject.createToken(salt)

        subject.validateToken(salt, token) shouldBe true
    }

    "Validating a valid token must not work with a wrong salt" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val token = subject.createToken("SALT")

        subject.validateToken("WRONG", token) shouldBe false
    }

    "Token must depend on the user id" {

        val creator = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val validator = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("X"), clientIp = "IP"))
        )

        val salt = "SALT"
        val token = creator.createToken(salt)

        validator.validateToken(salt, token) shouldBe false
    }

    "Token must depend on the user ip" {

        val creator = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val validator = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("X"), clientIp = "IP"))
        )

        val salt = "SALT"
        val token = creator.createToken(salt)

        validator.validateToken(salt, token) shouldBe false
    }

    "The ttl must be part of the tokens hash" {

        val creator = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val salt = "SALT"
        val token = creator.createToken(salt)

        val parts = String(token.fromBase64()).split(creator.glue)

        val crafted = "${parts[0].toLong() + 1}${creator.glue}${parts[1]}".toBase64()

        token shouldNotBe crafted

        creator.validateToken(salt, crafted) shouldBe false
    }

    "Token must become invalid after the its ttl" {

        val creator = StatelessCsrfProtection(
            "secret", 10, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val salt = "SALT"
        val token = creator.createToken(salt)

        creator.validateToken(salt, token) shouldBe true

        delay(200)

        creator.validateToken(salt, token) shouldBe false
    }
    "Validating a token that is not valid base64 must return false, not throw" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        // every other malformed shape is answered with false, so this one must be too
        listOf("x!", "!!!!", "QQ=", "QQ ==", "ab-_", "not base64 at all").forEach { token ->
            withClue(token) {
                subject.validateToken("SALT", token) shouldBe false
            }
        }
    }
    "The signature is an HMAC keyed by the secret, not a digest of the concatenation" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val token = subject.createToken("SALT")
        val signature = String(token.fromBase64()).split(subject.glue)[1]

        // what the old hand-rolled construction would have produced for the same fields
        val ttl = String(token.fromBase64()).split(subject.glue)[0]
        val handRolled = "SALT\u0000USER\u0000IP\u0000$ttl\u0000secret".sha384().toBase64()

        signature shouldNotBe handRolled
    }

    "A token does not validate under a different secret" {

        val fields = UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))

        val token = StatelessCsrfProtection("secret-a", 1000, fields).createToken("SALT")

        StatelessCsrfProtection("secret-b", 1000, fields).validateToken("SALT", token) shouldBe false
    }

    "Tampering with the signature is rejected" {

        val subject = StatelessCsrfProtection(
            "secret", 1000, UserProvider.static(UserRecord.LoggedIn(userId = UserId("USER"), clientIp = "IP"))
        )

        val decoded = String(subject.createToken("SALT").fromBase64())
        val (ttl, signature) = decoded.split(subject.glue)

        val flipped = signature.take(signature.length - 1) + if (signature.last() == 'A') 'B' else 'A'

        subject.validateToken("SALT", "$ttl${subject.glue}$flipped".toBase64()) shouldBe false
    }
})
