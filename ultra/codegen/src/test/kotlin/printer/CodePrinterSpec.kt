package io.peekandpoke.ultra.codegen.printer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CodePrinterSpec : FreeSpec() {

    init {
        "append writes plain text without indentation" {
            CodePrinter.print {
                append("hello")
            } shouldBe "hello"
        }

        "nl always emits LF, never the platform separator" {
            CodePrinter.print {
                append("a").nl().append("b")
            } shouldBe "a\nb"
        }

        "indented adds surrounding line breaks and one level" {
            CodePrinter.print {
                append("class Foo {").indented {
                    append("bar: string")
                }.append("}")
            } shouldBe "class Foo {\n    bar: string\n}"
        }

        "nested indentation accumulates" {
            CodePrinter.print {
                append("a {").indented {
                    append("b {").indented {
                        append("c")
                    }.append("}")
                }.append("}")
            } shouldBe "a {\n    b {\n        c\n    }\n}"
        }

        "multi-line input picks up the current indentation on every line" {
            CodePrinter.print {
                append("x {").indented {
                    append("one\ntwo")
                }.append("}")
            } shouldBe "x {\n    one\n    two\n}"
        }

        "blank lines inside multi-line input stay blank — no trailing whitespace" {
            CodePrinter.print {
                append("x {").indented {
                    append("one\n\ntwo")
                }.append("}")
            } shouldBe "x {\n    one\n\n    two\n}"
        }

        "appendEach separates but does not trail" {
            CodePrinter.print {
                appendEach(listOf("a", "b", "c"), separator = { append(", ") }) { append(it) }
            } shouldBe "a, b, c"
        }

        "appendEach on an empty list writes nothing" {
            CodePrinter.print {
                appendEach(emptyList<String>(), separator = { append(", ") }) { append(it) }
            } shouldBe ""
        }

        "appendEach defaults to a newline separator" {
            CodePrinter.print {
                appendEach(listOf("a", "b")) { append(it) }
            } shouldBe "a\nb"
        }

        "appendLine appends text then breaks" {
            CodePrinter.print {
                appendLine("a").appendLine("b")
            } shouldBe "a\nb\n"
        }

        "empty append is a no-op" {
            CodePrinter.print {
                append("a").append("").append("b")
            } shouldBe "ab"
        }
    }
}
