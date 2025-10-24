package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HashingSpec : StringSpec({

    //  ByteArray.toBase64  ////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(byteArrayOf(), ""),
        tuple(byteArrayOf(0.toByte()), "AA=="),
        tuple(byteArrayOf(65.toByte()), "QQ==")
    ).forEach { (input, expected) ->

        "ByteArray.toBase64: '${String(input)}' expects '$expected'" {
            input.toBase64() shouldBe expected
        }
    }

    //  String.toBase64  ///////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", Charsets.UTF_8, ""),
        tuple("a", Charsets.UTF_8, "YQ=="),
        tuple("äöü", Charsets.UTF_8, "w6TDtsO8"),
        tuple("", Charsets.UTF_16, ""),
        tuple("a", Charsets.UTF_16, "/v8AYQ=="),
        tuple("äöü", Charsets.UTF_16, "/v8A5AD2APw=")
    ).forEach { (input, charset, expected) ->

        "String.toBase64: '$input' ($charset) expects '$expected'" {
            input.toBase64(charset) shouldBe expected
        }
    }

    //  String.fromBase64  /////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", byteArrayOf()),
        tuple("AA==", byteArrayOf(0.toByte())),
        tuple("QQ==", byteArrayOf(65.toByte()))

    ).forEach { (input, expected) ->

        "String.fromBase64: '$input' expects '${String(expected)}'" {
            input.fromBase64() shouldBe expected
        }
    }

    //  ByteArray.md5  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(byteArrayOf(), "d41d8cd98f00b204e9800998ecf8427e"),
        tuple(byteArrayOf(0.toByte()), "93b885adfe0da089cdf634904fd59f71"),
        tuple(byteArrayOf(65.toByte()), "7fc56270e7a70fa81a5935b72eacbe29")
    ).forEach { (input, expected) ->

        "ByteArray.md5: '${String(input)}' expects '$expected'" {
            input.md5() shouldBe expected
        }
    }

    //  String.md5  ////////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", "d41d8cd98f00b204e9800998ecf8427e"),
        tuple("A", "7fc56270e7a70fa81a5935b72eacbe29"),
        tuple("äöü", "0a09d7ee1e23c509e5e6846c86823081")
    ).forEach { (input, expected) ->

        "String.md5: '$input' expects '$expected'" {
            input.md5() shouldBe expected
        }
    }

    //  String.sha256  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
        tuple("A", "559aead08264d5795d3909718cdd05abd49572e84fe55590eef31a88a08fdffd"),
        tuple("äöü", "bc329dbfa029f66476cb633afcd3136c65c840d0003ef6479583c09e64e2c33e")
    ).forEach { (input, expected) ->

        "String.sha256: '$input' expects '$expected'" {
            input.sha256().toHex() shouldBe expected
        }
    }

    //  String.sha384  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple(
            "",
            "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b"
        ),
        tuple(
            "A",
            "ad14aaf25020bef2fd4e3eb5ec0c50272cdfd66074b0ed037c9a11254321aac0729985374beeaa5b80a504d048be1864"
        ),
        tuple(
            "äöü",
            "8a224a81de903b7c3e225f930f855205d55e4c3da677a8d9808cd67b9941290637e678489b0843074e4448b048824ee3"
        ),
    ).forEach { (input, expected) ->

        "String.sha384: '$input' expects '$expected'" {
            input.sha384().toHex() shouldBe expected
        }
    }
})
