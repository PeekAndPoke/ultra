package io.peekandpoke.ultra.common.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class Inner(val region: String, val account: String)

@Serializable
private data class Conf(
    val issuer: String,
    val signingKey: Redacted<String>,
    val aws: Redacted<Inner>,
)

/**
 * The kotlinx half of [Redacted]. The Slumber half lives in `ultra/slumber`, and the two must agree on
 * the placeholder — that is asserted there, against this constant.
 */
class RedactedSpec : StringSpec({

    val secret = "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG"

    val conf = Conf(
        issuer = "app",
        signingKey = Redacted(secret),
        aws = Redacted(Inner(region = "eu-central-1", account = "SECRET-ACCOUNT")),
    )

    "serializing writes the placeholder and nothing of the value" {
        val json = Json.encodeToString(Conf.serializer(), conf)

        json shouldNotContain secret
        json shouldBe """{"issuer":"app","signingKey":"${Redacted.PLACEHOLDER}",""" +
                """"aws":"${Redacted.PLACEHOLDER}"}"""
    }

    "a SUBTREE is replaced wholesale, not walked into" {
        // The reason the type is generic at all: `Redacted<Inner>` must not emit `{region, account}`
        // with only the sensitive-looking leaves swapped.
        val json = Json.encodeToString(Conf.serializer(), conf)

        json shouldNotContain "SECRET-ACCOUNT"
        json shouldNotContain "eu-central-1"
        json shouldNotContain "region"
    }

    "deserializing yields the REAL value — the asymmetry is the contract" {
        val back = Json.decodeFromString(
            Conf.serializer(),
            """{"issuer":"app","signingKey":"$secret","aws":{"region":"eu","account":"acct"}}""",
        )

        back.signingKey.value shouldBe secret
        back.aws.value shouldBe Inner(region = "eu", account = "acct")
    }

    "a scalar round trip DESTROYS the value — on purpose" {
        // Stated as a test so nobody 'fixes' it.
        @Serializable
        data class Scalar(val signingKey: Redacted<String>)

        val once = Json.encodeToString(Scalar.serializer(), Scalar(Redacted(secret)))
        val back = Json.decodeFromString(Scalar.serializer(), once)

        back.signingKey.value shouldBe Redacted.PLACEHOLDER
        back.signingKey.value shouldNotContain secret
    }

    "a SUBTREE round trip throws rather than yielding a broken object" {
        // `Redacted<Inner>` writes a string, so reading it back through Inner's serializer cannot work.
        // Throwing is the better outcome and is therefore pinned: the alternative would be silently
        // handing back an `Inner` whose fields are invented. A redacted subtree is GONE, and code that
        // re-reads its own output should find that out loudly.
        val once = Json.encodeToString(Conf.serializer(), conf)

        shouldThrow<SerializationException> {
            Json.decodeFromString(Conf.serializer(), once)
        }
    }

    "toString redacts — including inside an enclosing data class" {
        Redacted(secret).toString() shouldBe Redacted.PLACEHOLDER

        // The failure this prevents: `JwtConfig` carried a hand-written redacting toString() and looked
        // protected, while the serializer wrote the key in full. Here toString is safe by construction,
        // so an enclosing data class's generated toString cannot leak either.
        conf.toString() shouldNotContain secret
    }

    "equality is by value, so tests can assert on what was loaded" {
        Redacted("a") shouldBe Redacted("a")
        (Redacted("a") == Redacted("b")) shouldBe false
    }

    "hashCode is CONSTANT — it must not be a digest of the secret" {
        // String.hashCode() is cheap, well known and non-cryptographic, so returning it would hand out
        // a 32-bit oracle over the secret, offline-invertible for anything short or low-entropy. The
        // reachable sink found in review was BackgroundJobQueued.calcHash, whose fallback branch hashes
        // the raw object and persists the result as an admin-readable dedupeKey.
        Redacted("a").hashCode() shouldBe 0
        Redacted("a").hashCode() shouldBe Redacted("b").hashCode()

        // the equals/hashCode contract still holds in the direction that matters
        (Redacted("a") == Redacted("a")) shouldBe true
        Redacted("a").hashCode() shouldBe Redacted("a").hashCode()
    }
})
