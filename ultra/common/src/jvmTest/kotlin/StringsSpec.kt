package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StringsSpec : StringSpec({

    //  String.surround  ///////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", "", ""),
        tuple("a", "", "a"),
        tuple("", "%", "%%"),
        tuple("x", "::", "::x::")
    ).forEach { (source, surround, expected) ->

        "String.surround: '$source' surrounded with '$surround'" {
            source.surround(surround) shouldBe expected
        }
    }

    //  String.ucFirst  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", ""),
        tuple("a", "A"),
        tuple("ab", "Ab"),
        tuple("abc", "Abc"),
        tuple("A", "A"),
        tuple("AB", "AB"),
        tuple("ABC", "ABC")
    ).forEach { (source, expected) ->

        "String.ucFirst: '$source'" {
            source.ucFirst() shouldBe expected
        }
    }

    //  String.lcFirst  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", ""),
        tuple("a", "a"),
        tuple("ab", "ab"),
        tuple("abc", "abc"),
        tuple("A", "a"),
        tuple("AB", "aB"),
        tuple("ABC", "aBC")
    ).forEach { (source, expected) ->

        "String.lcFirst: '$source'" {
            source.lcFirst() shouldBe expected
        }
    }

    //  String.startsWithAny / String.startsWithNone  //////////////////////////////////////////////////////////////////

    listOf(
        tuple("", arrayOf(), false),
        tuple("", arrayOf("a"), false),
        tuple("a", arrayOf("b"), false),
        tuple("abc", arrayOf("abc"), true),
        tuple("abc", arrayOf("a", "b", "c"), true),
        tuple("abc", arrayOf("a", "b", "abc"), true)
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
        tuple("", 0),
        tuple("a", 1),
        tuple(" ", 1),
        tuple(" a ", 3),
        tuple(
            """
                line1
            """.trimIndent(),
            5
        ),
        tuple(
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
        tuple("", 0, ""),
        tuple("a", 0, "..."),
        tuple("Some Text", 4, "Some...")
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
        tuple("", listOf()),
        tuple("a", listOf("a")),
        tuple("A", listOf("A")),
        tuple("a1B1", listOf("a1", "B1")),
        tuple("oneTwo", listOf("one", "Two")),
        tuple("OneTwo", listOf("One", "Two")),
        tuple("One Two", listOf("One", "Two"))
    ).forEach { (text, expected) ->

        "String.camelCaseSplit: '$text' should be $expected" {
            text.camelCaseSplit() shouldBe expected
        }
    }

    //  String.camelCaseDivide  ////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        tuple("", ""),
        tuple("a", "a"),
        tuple("A", "A"),
        tuple("a1B1", "a1 B1"),
        tuple("oneTwo", "one Two"),
        tuple("OneTwo", "One Two"),
        tuple("One Two", "One Two")
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
        tuple("", arrayOf(), ""),
        tuple("uri", arrayOf(), "uri"),
        tuple("uri", arrayOf("a" to "a"), "uri?a=a"),
        tuple("uri", arrayOf("a" to "a", "b" to "b"), "uri?a=a&b=b"),
        tuple("uri?x", arrayOf(), "uri?x"),
        tuple("uri?x", arrayOf("a" to "a", "b" to "b"), "uri?x&a=a&b=b")
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
