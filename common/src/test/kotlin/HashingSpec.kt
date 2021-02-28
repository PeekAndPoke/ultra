package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class HashingSpec : StringSpec({

    //  ByteArray.toBase64  ////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(byteArrayOf(), ""),
        row(byteArrayOf(0.toByte()), "AA=="),
        row(byteArrayOf(65.toByte()), "QQ==")
    ).forEach { (input, expected) ->

        "ByteArray.toBase64: '${String(input)}' expects '$expected'" {
            input.toBase64() shouldBe expected
        }
    }

    //  String.toBase64  ///////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", Charsets.UTF_8, ""),
        row("a", Charsets.UTF_8, "YQ=="),
        row("äöü", Charsets.UTF_8, "w6TDtsO8"),
        row("", Charsets.UTF_16, ""),
        row("a", Charsets.UTF_16, "/v8AYQ=="),
        row("äöü", Charsets.UTF_16, "/v8A5AD2APw=")
    ).forEach { (input, charset, expected) ->

        "String.toBase64: '$input' ($charset) expects '$expected'" {
            input.toBase64(charset) shouldBe expected
        }
    }

    //  String.fromBase64  /////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", byteArrayOf()),
        row("AA==", byteArrayOf(0.toByte())),
        row("QQ==", byteArrayOf(65.toByte()))

    ).forEach { (input, expected) ->

        "String.fromBase64: '$input' expects '${String(expected)}'" {
            input.fromBase64() shouldBe expected
        }
    }

    //  ByteArray.md5  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row(byteArrayOf(), "d41d8cd98f00b204e9800998ecf8427e"),
        row(byteArrayOf(0.toByte()), "93b885adfe0da089cdf634904fd59f71"),
        row(byteArrayOf(65.toByte()), "7fc56270e7a70fa81a5935b72eacbe29")
    ).forEach { (input, expected) ->

        "ByteArray.md5: '${String(input)}' expects '$expected'" {
            input.md5() shouldBe expected
        }
    }

    //  String.md5  ////////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", "d41d8cd98f00b204e9800998ecf8427e"),
        row("A", "7fc56270e7a70fa81a5935b72eacbe29"),
        row("äöü", "0a09d7ee1e23c509e5e6846c86823081")
    ).forEach { (input, expected) ->

        "String.md5: '${input}' expects '$expected'" {
            input.md5() shouldBe expected
        }
    }

    //  String.sha256  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
        row("A", "559aead08264d5795d3909718cdd05abd49572e84fe55590eef31a88a08fdffd"),
        row("äöü", "bc329dbfa029f66476cb633afcd3136c65c840d0003ef6479583c09e64e2c33e")
    ).forEach { (input, expected) ->

        "String.sha256: '${input}' expects '$expected'" {
            input.sha256().toHex() shouldBe expected
        }
    }

    //  String.sha384  /////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b"),
        row("A", "ad14aaf25020bef2fd4e3eb5ec0c50272cdfd66074b0ed037c9a11254321aac0729985374beeaa5b80a504d048be1864"),
        row("äöü", "8a224a81de903b7c3e225f930f855205d55e4c3da677a8d9808cd67b9941290637e678489b0843074e4448b048824ee3")
    ).forEach { (input, expected) ->

        "String.sha384: '${input}' expects '$expected'" {
            input.sha384().toHex() shouldBe expected
        }
    }
})
