package io.peekandpoke.ultra.common

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

    "isForbiddenInId bans what could forge a key boundary or a log line" {
        // Written as Char(code) rather than '\uXXXX' escapes so this source stays pure ASCII.
        // A bare U+2028/U+2029 is a LINE TERMINATOR in Kotlin source, so a slip that lands the
        // raw character here instead of the escape either breaks the literal or silently tests
        // the wrong character. Codes cannot be mistyped invisibly.
        Char(0x00).isForbiddenInId() shouldBe true // NUL — the composite-key delimiter
        '\n'.isForbiddenInId() shouldBe true
        '\r'.isForbiddenInId() shouldBe true
        Char(0x7F).isForbiddenInId() shouldBe true // DEL
        Char(0x85).isForbiddenInId() shouldBe true // NEL, a C1 control
        Char(0x2028).isForbiddenInId() shouldBe true // LINE SEPARATOR
        Char(0x2029).isForbiddenInId() shouldBe true // PARAGRAPH SEPARATOR
    }

    "isForbiddenInId allows everything an id legitimately contains" {
        "b2b_users/abc123".none { it.isForbiddenInId() } shouldBe true
        "user@example.com".none { it.isForbiddenInId() } shouldBe true
        ' '.isForbiddenInId() shouldBe false // ugly in an id, but not a security boundary
        Char(0xE4).isForbiddenInId() shouldBe false
    }

    "isForbiddenInId bans EXACTLY the C0 controls, DEL, the C1 controls and U+2028 / U+2029" {
        // Enumerated, not spot-checked. Every id value class gates on this one predicate, so the
        // boundary has to be pinned in both directions: widening it starts rejecting ids already in
        // the database, narrowing it lets a log-forging character back through.
        val actual = (0..0x2100).map { it.toChar() }.filter { it.isForbiddenInId() }.toSet()

        val expected = ((0x00..0x1F) + listOf(0x7F) + (0x80..0x9F) + listOf(0x2028, 0x2029))
            .map { it.toChar() }
            .toSet()

        actual shouldBe expected
    }
})
