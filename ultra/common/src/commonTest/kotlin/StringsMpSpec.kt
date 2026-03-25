package io.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StringsMpSpec : StringSpec({

    val encodingPairs = listOf(
        // Basic cases
        "" to "",
        "abc" to "abc",
        "a c" to "a%20c",
        "a_&_c" to "a_%26_c",
        "a/c" to "a%2Fc",

        // Unreserved characters (should NOT be encoded)
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" to "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
        "test-file_name.txt~backup" to "test-file_name.txt~backup",

        // Reserved characters (that should NOT be encoded)
        "!" to "!",
        "'" to "'",
        "(" to "(",
        ")" to ")",
        "*" to "*",

        // Reserved characters (should be encoded)
        "#" to "%23",
        "$" to "%24",
        "&" to "%26",
        "+" to "%2B",
        "," to "%2C",
        "/" to "%2F",
        ":" to "%3A",
        ";" to "%3B",
        "=" to "%3D",
        "?" to "%3F",
        "@" to "%40",
        "[" to "%5B",
        "]" to "%5D",

        // Special characters
        " " to "%20", // space
        "\"" to "%22", // quote
        "%" to "%25", // percent
        "<" to "%3C", // less than
        ">" to "%3E", // greater than
        "\\" to "%5C", // backslash
        "^" to "%5E", // caret
        "`" to "%60", // backtick
        "{" to "%7B", // left brace
        "|" to "%7C", // pipe
        "}" to "%7D", // right brace

        // Unicode characters (Latin)
        "José" to "Jos%C3%A9",
        "café" to "caf%C3%A9",
        "naïve" to "na%C3%AFve",
        "résumé" to "r%C3%A9sum%C3%A9",
        "Zürich" to "Z%C3%BCrich",

        // Unicode characters (other scripts)
        "世界" to "%E4%B8%96%E7%95%8C", // Chinese
        "こんにちは" to "%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF", // Japanese
        "안녕하세요" to "%EC%95%88%EB%85%95%ED%95%98%EC%84%B8%EC%9A%94", // Korean
        "Привет" to "%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82", // Russian

        // Emoji
        "🙂" to "%F0%9F%99%82",
        "👍" to "%F0%9F%91%8D",
        "🌟" to "%F0%9F%8C%9F",

        // Mixed content
        "Hello, 世界! 🙂" to "Hello%2C%20%E4%B8%96%E7%95%8C!%20%F0%9F%99%82",
        "user@example.com" to "user%40example.com",
        "https://example.com/path?query=value&other=data" to "https%3A%2F%2Fexample.com%2Fpath%3Fquery%3Dvalue%26other%3Ddata",

        // Edge cases
        "+++" to "%2B%2B%2B", // Plus signs should be encoded, not treated as spaces
        "a+b=c&d" to "a%2Bb%3Dc%26d",
        "100%" to "100%25",
        "file.txt?version=1.0" to "file.txt%3Fversion%3D1.0",

        // Control characters
        "\t" to "%09", // tab
        "\n" to "%0A", // newline
        "\r" to "%0D", // carriage return

        // High Unicode code points
        "𝕏" to "%F0%9D%95%8F", // Mathematical double-struck X
        "🚀" to "%F0%9F%9A%80", // Rocket emoji
    )

    "encodeUriComponent" {
        assertSoftly {
            encodingPairs.forEach { (left, right) ->
                withClue("'$left' encoded should be '$right'") {
                    left.encodeUriComponent() shouldBe right
                }
            }
        }
    }

    "decodeUriComponent" {
        assertSoftly {
            encodingPairs.forEach { (left, right) ->
                withClue("'$right' decoded should be '$left'") {
                    right.decodeUriComponent() shouldBe left
                }
            }
        }
    }

    "decodeUriComponent should not decode plus signs" {
        "hello+world".decodeUriComponent() shouldBe "hello+world"
        "a+b%20c".decodeUriComponent() shouldBe "a+b c"
        "%2B".decodeUriComponent() shouldBe "+"
    }

    "encodeUriComponent and decodeUriComponent should be reversible" {
        val testStrings = listOf(
            "Hello, World!",
            "José café naïve",
            "世界 🌍",
            "user@domain.com",
            "path/to/file.txt?query=value&other=123",
            "++encoded++",
            "Mixed: ASCII + Unicode 世界 + Emoji 🚀"
        )

        assertSoftly {
            testStrings.forEach { original ->
                withClue("Round trip encoding/decoding should preserve: '$original'") {
                    val encoded = original.encodeUriComponent()
                    val decoded = encoded.decodeUriComponent()
                    decoded shouldBe original
                }
            }
        }
    }
})
