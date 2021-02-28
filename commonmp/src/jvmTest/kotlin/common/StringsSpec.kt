package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class StringsSpec : StringSpec({

    //  String.surround  ///////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", "", ""),
        row("a", "", "a"),
        row("", "%", "%%"),
        row("x", "::", "::x::")
    ).forEach { (source, surround, expected) ->

        "String.surround: '$source' surrounded with '$surround'" {
            source.surround(surround) shouldBe expected
        }
    }

    //  String.ucFirst  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", ""),
        row("a", "A"),
        row("ab", "Ab"),
        row("abc", "Abc"),
        row("A", "A"),
        row("AB", "AB"),
        row("ABC", "ABC")
    ).forEach { (source, expected) ->

        "String.ucFirst: '$source'" {
            source.ucFirst() shouldBe expected
        }
    }

    //  String.lcFirst  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", ""),
        row("a", "a"),
        row("ab", "ab"),
        row("abc", "abc"),
        row("A", "a"),
        row("AB", "aB"),
        row("ABC", "aBC")
    ).forEach { (source, expected) ->

        "String.lcFirst: '$source'" {
            source.lcFirst() shouldBe expected
        }
    }

    //  String.startsWithAny / String.startsWithNone  //////////////////////////////////////////////////////////////////

    listOf(
        row("", arrayOf(), false),
        row("", arrayOf("a"), false),
        row("a", arrayOf("b"), false),
        row("abc", arrayOf("abc"), true),
        row("abc", arrayOf("a", "b", "c"), true),
        row("abc", arrayOf("a", "b", "abc"), true)
    ).forEach { (source, search, expected) ->

        "Strings.startsWithAny: '$source' search '$search'" {

            assertSoftly {

                source.startsWithAny(*search) shouldBe expected
                source.startsWithAny(search) shouldBe expected
                source.startsWithAny(search.toList()) shouldBe expected

                source.startsWithNone(*search) shouldBe !expected
                source.startsWithNone(search) shouldBe !expected
                source.startsWithNone(search.toList()) shouldBe !expected
            }
        }
    }

    //  String.maxLineLength  //////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", 0),
        row("a", 1),
        row(" ", 1),
        row(" a ", 3),
        row(
            """
                line1
            """.trimIndent(),
            5
        ),
        row(
            """
                line1
                long line2
            """.trimIndent(),
            10
        )
    ).forEach { (text, expected) ->

        "String.maxLineLength: '$text' should be $expected" {
            text.maxLineLength() shouldBe expected
        }
    }

    //  String.ellipsis  ///////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", 0, ""),
        row("a", 0, "..."),
        row("Some Text", 4, "Some...")
    ).forEach { (text, maxLength, expected) ->

        "String.ellipsis: '$text' maxLength $maxLength should be '$expected'" {
            text.ellipsis(maxLength) shouldBe expected
        }
    }

    "String.ellipsis with custom suffix" {
        "Some Text".ellipsis(maxLength = 4, suffix = "-") shouldBe "Some-"
    }

    //  String.camelCaseSplit  /////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", listOf()),
        row("a", listOf("a")),
        row("A", listOf("A")),
        row("a1B1", listOf("a1", "B1")),
        row("oneTwo", listOf("one", "Two")),
        row("OneTwo", listOf("One", "Two")),
        row("One Two", listOf("One", "Two"))
    ).forEach { (text, expected) ->

        "String.camelCaseSplit: '$text' should be $expected" {
            text.camelCaseSplit() shouldBe expected
        }
    }

    //  String.camelCaseDivide  ////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", ""),
        row("a", "a"),
        row("A", "A"),
        row("a1B1", "a1 B1"),
        row("oneTwo", "one Two"),
        row("OneTwo", "One Two"),
        row("One Two", "One Two")
    ).forEach { (text, expected) ->

        "String.camelCaseDivide: '$text' should be $expected" {
            text.camelCaseDivide() shouldBe expected
        }
    }

    "String.camelCaseDivide: 'oneTwo' with custom divider '-'" {
        "oneTwo".camelCaseDivide("-") shouldBe "one-Two"
    }

    //  String.toUri  //////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", arrayOf(), ""),
        row("uri", arrayOf(), "uri"),
        row("uri", arrayOf("a" to "a"), "uri?a=a"),
        row("uri", arrayOf("a" to "a", "b" to "b"), "uri?a=a&b=b"),
        row("uri?x", arrayOf(), "uri?x"),
        row("uri?x", arrayOf("a" to "a", "b" to "b"), "uri?x&a=a&b=b")
    ).forEach { (source, params, expected) ->

        "String.toUri: '$source' params '$params'" {

            assertSoftly {
                source.toUri(*params) shouldBe expected
                source.toUri(params.toMap()) shouldBe expected
                source.toUri(params.toList()) shouldBe expected
            }
        }
    }
})
