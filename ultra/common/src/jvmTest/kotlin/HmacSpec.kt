package io.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HmacSpec : StringSpec({

    // RFC 4231 test case 2 — quoted from the RFC, not recalled. Guards against wiring the wrong
    // algorithm name, which is the only way this could realistically be wrong given it delegates
    // to javax.crypto.Mac.
    val key = "Jefe"
    val data = "what do ya want for nothing?"

    "hmacSha256 matches RFC 4231 test case 2" {
        data.hmacSha256(key).toHex() shouldBe
                "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"
    }

    "hmacSha384 matches RFC 4231 test case 2" {
        data.hmacSha384(key).toHex() shouldBe
                "af45d2e376484031617f78d2b58a6b1b9c7ef464f5a01b47e42ec3736322445e8e2240ca5e69e2c78b3239ecfab21649"
    }

    "digest sizes are as advertised" {
        data.hmacSha256(key).size shouldBe 32
        data.hmacSha384(key).size shouldBe 48
    }

    "a MAC is not the same as hashing the concatenation" {
        // the whole point: H(data || secret) is a different, weaker construction
        data.hmacSha256(key).toHex() shouldNotBe "$data$key".sha256().toHex()
        data.hmacSha256(key).toHex() shouldNotBe "$key$data".sha256().toHex()
    }

    "a different key gives a different MAC" {
        data.hmacSha256("Jefe").toHex() shouldNotBe data.hmacSha256("jefe").toHex()
    }

    "the same input gives the same MAC" {
        data.hmacSha256(key).toHex() shouldBe data.hmacSha256(key).toHex()
    }

    "a key longer than the block size is accepted" {
        val longKey = "k".repeat(200)

        data.hmacSha256(longKey).size shouldBe 32
    }

    "an empty key is rejected" {
        shouldThrow<IllegalArgumentException> { data.hmacSha256("") }
        shouldThrow<IllegalArgumentException> { data.hmacSha384("") }
    }
})
