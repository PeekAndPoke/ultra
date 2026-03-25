package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StringsExtSpec : StringSpec({

    "isUrlWithProtocol must match valid URLs" {
        "https://example.com".isUrlWithProtocol() shouldBe true
        "http://example.com".isUrlWithProtocol() shouldBe true
        "https://www.example.com".isUrlWithProtocol() shouldBe true
        "https://example.com/path?q=1&r=2".isUrlWithProtocol() shouldBe true
        "https://sub.domain.example.com".isUrlWithProtocol() shouldBe true
        "http://example.com:8080/path".isUrlWithProtocol() shouldBe true
    }

    "isUrlWithProtocol is case-insensitive for protocol" {
        "HTTPS://example.com".isUrlWithProtocol() shouldBe true
        "HTTP://example.com".isUrlWithProtocol() shouldBe true
        "Https://example.com".isUrlWithProtocol() shouldBe true
    }

    "isUrlWithProtocol must reject invalid URLs" {
        "".isUrlWithProtocol() shouldBe false
        "example.com".isUrlWithProtocol() shouldBe false
        "ftp://example.com".isUrlWithProtocol() shouldBe false
        "just some text".isUrlWithProtocol() shouldBe false
        "http://".isUrlWithProtocol() shouldBe false
    }

    "isEmail must match valid emails" {
        "user@example.com".isEmail() shouldBe true
        "user.name@example.com".isEmail() shouldBe true
        "user+tag@example.com".isEmail() shouldBe true
        "user@sub.domain.com".isEmail() shouldBe true
    }

    "isEmail must reject invalid emails" {
        "".isEmail() shouldBe false
        "not-an-email".isEmail() shouldBe false
        "@example.com".isEmail() shouldBe false
        "user@".isEmail() shouldBe false
        "user @example.com".isEmail() shouldBe false
    }

    "splitAndTrimToSet with default delimiter" {
        "a, b, c".splitAndTrimToSet() shouldBe setOf("a", "b", "c")
        " a , b , c ".splitAndTrimToSet() shouldBe setOf("a", "b", "c")
        "a,,b".splitAndTrimToSet() shouldBe setOf("a", "b")
        "".splitAndTrimToSet() shouldBe emptySet()
        " , , ".splitAndTrimToSet() shouldBe emptySet()
    }

    "splitAndTrimToSet with custom delimiter" {
        "a;b;c".splitAndTrimToSet(";") shouldBe setOf("a", "b", "c")
        " a ; b ; c ".splitAndTrimToSet(";") shouldBe setOf("a", "b", "c")
    }

    "splitAndTrimToSet deduplicates" {
        "a, b, a, c, b".splitAndTrimToSet() shouldBe setOf("a", "b", "c")
    }

    "surround with same prefix and suffix" {
        "hello".surround("*") shouldBe "*hello*"
        "".surround("--") shouldBe "----"
    }

    "surround with different prefix and suffix" {
        "hello".surround("[", "]") shouldBe "[hello]"
    }

    "ucFirst capitalizes first letter" {
        "hello".ucFirst() shouldBe "Hello"
        "Hello".ucFirst() shouldBe "Hello"
        "".ucFirst() shouldBe ""
        "a".ucFirst() shouldBe "A"
    }

    "lcFirst lowercases first letter" {
        "Hello".lcFirst() shouldBe "hello"
        "hello".lcFirst() shouldBe "hello"
        "".lcFirst() shouldBe ""
        "A".lcFirst() shouldBe "a"
    }

    "startsWithAny matches any prefix" {
        "hello world".startsWithAny("hi", "hello") shouldBe true
        "hello world".startsWithAny("hi", "bye") shouldBe false
    }

    "startsWithNone rejects all prefixes" {
        "hello world".startsWithNone("hi", "bye") shouldBe true
        "hello world".startsWithNone("hello", "bye") shouldBe false
    }

    "maxLineLength returns max across lines" {
        "abc\nab\nabcde".maxLineLength() shouldBe 5
        "single".maxLineLength() shouldBe 6
        "".maxLineLength() shouldBe 0
    }

    "ellipsis truncates long strings" {
        "hello world".ellipsis(5) shouldBe "hello..."
        "hi".ellipsis(5) shouldBe "hi"
        "exact".ellipsis(5) shouldBe "exact"
        "hello".ellipsis(5, "~") shouldBe "hello"
        "hello!".ellipsis(5, "~") shouldBe "hello~"
    }

    "camelCaseSplit splits camelCase words" {
        "camelCaseWord".camelCaseSplit() shouldBe listOf("camel", "Case", "Word")
        "XMLParser".camelCaseSplit() shouldBe listOf("X", "M", "L", "Parser")
        "simple".camelCaseSplit() shouldBe listOf("simple")
        "".camelCaseSplit() shouldBe emptyList()
    }

    "camelCaseDivide joins with divider" {
        "camelCaseWord".camelCaseDivide() shouldBe "camel Case Word"
        "camelCaseWord".camelCaseDivide("-") shouldBe "camel-Case-Word"
    }
})
