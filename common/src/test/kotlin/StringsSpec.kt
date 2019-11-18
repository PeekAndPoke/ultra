package de.peekandpoke.ultra.common

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class StringsSpec : StringSpec({

    ////  String.surround  /////////////////////////////////////////////////////////////////////////////////////////////

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

    ////  String.ucFirst  //////////////////////////////////////////////////////////////////////////////////////////////

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

    ////  String.lcFirst  //////////////////////////////////////////////////////////////////////////////////////////////

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

    ////  String.startsWithAny / String.startsWithNone  ////////////////////////////////////////////////////////////////

    listOf(
        row("", listOf(), false),
        row("", listOf("a"), false),
        row("a", listOf("b"), false),
        row("abc", listOf("abc"), true),
        row("abc", listOf("a", "b", "c"), true),
        row("abc", listOf("a", "b", "abc"), true)
    ).forEach { (source, search, expected) ->

        "Strings.startsWithAny: '$source' search '$search'" {

            assertSoftly {

                source.startsWithAny(*search.toTypedArray()) shouldBe expected
                source.startsWithAny(search.toTypedArray()) shouldBe expected
                source.startsWithAny(search) shouldBe expected

                source.startsWithNone(*search.toTypedArray()) shouldBe !expected
                source.startsWithNone(search.toTypedArray()) shouldBe !expected
                source.startsWithNone(search) shouldBe !expected
            }
        }
    }

    ////  String.toUri  ////////////////////////////////////////////////////////////////////////////////////////////////

    listOf(
        row("", listOf(), ""),
        row("uri", listOf(), "uri"),
        row("uri", listOf("a" to "a"), "uri?a=a"),
        row("uri", listOf("a" to "a", "b" to "b"), "uri?a=a&b=b"),
        row("uri?x", listOf(), "uri?x"),
        row("uri?x", listOf("a" to "a", "b" to "b"), "uri?x&a=a&b=b")
    ).forEach { (source, params, expected) ->

        "String.toUri: '$source' params '$params'" {

            assertSoftly {
                source.toUri(*params.toTypedArray()) shouldBe expected
                source.toUri(params.toMap()) shouldBe expected
                source.toUri(params) shouldBe expected
            }
        }
    }
})
