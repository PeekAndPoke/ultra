package io.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class EncodingSpec : StringSpec({

    "base64 round-trips" {
        "hello world".toBase64().fromBase64().decodeToString() shouldBe "hello world"
    }

    "fromBase64 tolerates missing trailing padding" {
        "QQ".fromBase64().decodeToString() shouldBe "A"
    }

    // The strict / OrNull pair ////////////////////////////////////////////////////////////////////

    "fromBase64 throws on malformed input" {
        listOf(
            "!!!!",          // illegal characters
            "QQ=",           // broken ending unit
            "QQ ==",         // whitespace
            "ab-_",          // url-safe alphabet, not the standard one
        ).forEach { input ->
            shouldThrow<IllegalArgumentException> { input.fromBase64() }
        }
    }

    "fromBase64OrNull returns null for the same inputs" {
        listOf("!!!!", "QQ=", "QQ ==", "ab-_").forEach { input ->
            input.fromBase64OrNull().shouldBeNull()
        }
    }

    "fromBase64OrNull decodes valid input" {
        "hello".toBase64().fromBase64OrNull()?.decodeToString() shouldBe "hello"
    }

    "fromBase64OrNull returns an empty array for an empty string" {
        "".fromBase64OrNull()?.size shouldBe 0
    }

    // toHex ///////////////////////////////////////////////////////////////////////////////////////

    "toHex renders two lowercase characters per byte" {
        byteArrayOf(0x00, 0x0f, 0x7f, -1).toHex() shouldBe "000f7fff"
    }

    "toHex of an empty array is empty" {
        byteArrayOf().toHex() shouldBe ""
    }
})
