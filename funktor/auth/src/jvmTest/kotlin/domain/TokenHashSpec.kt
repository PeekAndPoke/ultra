package io.peekandpoke.funktor.auth.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.RealmTokenConfig
import kotlin.time.Duration

/** What the auth records store of a one-shot secret (ULTRA-00291): pinned to a known answer, not to the code. */
class TokenHashSpec : StringSpec({

    "the hash is SHA-256, base64url without padding: the published vector for \"abc\"" {
        // SHA-256("abc") = ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad
        TokenHash.of("abc").value shouldBe "ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0"
    }

    "the token is hashed whole, as UTF-8" {
        TokenHash.of("abcd").value shouldBe "iNQmb9TmM40TuEX88olXnSCciXgjuSF9o-Fhk28DFYk"
        TokenHash.of(Char(0xE4).toString()).value shouldBe "M-bXP-6CkEyNevt43hFU0ejcKg7bCBIOY99bk4XC2cw" // "a" with umlaut
    }

    "a realm cannot make the unsalted hash guessable, nor the cancel-link cap racy" {
        shouldThrow<IllegalArgumentException> { RealmTokenConfig(randomTokenByteLength = 31) }
        shouldThrow<IllegalArgumentException> { RealmTokenConfig(maxSignUpCancelTokens = 1) }

        RealmTokenConfig(randomTokenByteLength = 32, maxSignUpCancelTokens = 2).randomTokenByteLength shouldBe 32
    }

    "with the defaults, every activation mail an account can get keeps its cancel link until the account ends" {
        val config = RealmTokenConfig()
        // The sign-up mail, then one re-send after each delay, the last delay repeating, while the account lives.
        var mails = 1
        var at = Duration.ZERO

        while (true) {
            at += config.activationResendDelays.getOrElse(mails - 1) { config.activationResendDelays.last() }
            if (at > config.pendingAccountLifetime) break
            mails++
        }

        mails shouldBe 12
        mails shouldBeLessThanOrEqual config.maxSignUpCancelTokens
    }
})
