package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * Pins the escaping performed by [tsStringLiteral] and [tsPropertyName].
 *
 * Asserts the PROPERTY — that every emitted literal survives a round trip through a JavaScript string
 * decoder — rather than one example per character. An example-based test goes green as soon as the one
 * character it happens to name is handled, which is exactly how a half-finished escaper survives.
 */
class TsLiteralsSpec : FreeSpec() {

    /**
     * Decodes a single-quoted JavaScript string literal.
     *
     * Deliberately strict: a literal that is not properly delimited, or that carries a raw quote or
     * line break, is an error rather than something that decodes to a plausible-looking value. A lenient
     * decoder would accept broken emissions and defeat the point of the round trip.
     */
    private fun decodeJsSingleQuoted(literal: String): String {
        require(literal.length >= 2 && literal.first() == '\'' && literal.last() == '\'') {
            "not a single-quoted literal: $literal"
        }

        val body = literal.substring(1, literal.length - 1)
        val out = StringBuilder()

        var i = 0

        while (i < body.length) {
            when (val ch = body[i]) {
                '\\' -> {
                    require(i + 1 < body.length) { "trailing backslash in $literal" }

                    when (val esc = body[i + 1]) {
                        '\\' -> out.append('\\').also { i += 2 }
                        '\'' -> out.append('\'').also { i += 2 }
                        'n' -> out.append('\n').also { i += 2 }
                        'r' -> out.append('\r').also { i += 2 }

                        'u' -> {
                            require(i + 6 <= body.length) { "truncated unicode escape in $literal" }
                            out.append(Char(body.substring(i + 2, i + 6).toInt(16)))
                            i += 6
                        }

                        else -> error("unsupported escape '\\$esc' in $literal")
                    }
                }

                // Either of these would have terminated the literal in a real parser.
                '\'' -> error("unescaped quote in $literal")
                '\n', '\r' -> error("raw line break in $literal")

                else -> out.append(ch).also { i++ }
            }
        }

        return out.toString()
    }

    /** Values chosen so that each one fails a DIFFERENT missing escape rule. */
    private val hostile: List<String> = listOf(
        "plain",
        "it's",
        """back\slash""",
        """both ' and \ together""",
        """trailing backslash\""",
        "trailing quote'",
        "'leading quote",
        "line\nbreak",
        "carriage\rreturn",
        "crlf\r\npair",
        Char(0x2028).toString(),
        Char(0x2029).toString(),
        // What the escaping actually prevents: closing the literal and appending statements.
        "'); alert(1); z.literal('",
    )

    init {
        "tsStringLiteral" - {
            "every value that can break out of a literal round-trips" {
                hostile.forEach { value ->
                    withClue("code points: ${value.map { it.code }}") {
                        decodeJsSingleQuoted(tsStringLiteral(value)) shouldBe value
                    }
                }
            }

            "the result is always delimited by single quotes" {
                hostile.forEach { value ->
                    withClue("value: $value") {
                        tsStringLiteral(value).first() shouldBe '\''
                        tsStringLiteral(value).last() shouldBe '\''
                    }
                }
            }

            "an empty string is still a valid literal" {
                tsStringLiteral("") shouldBe "''"
            }
        }

        "tsPropertyName" - {
            "a bare identifier is emitted unquoted" {
                listOf("name", "_private", "\$dollar", "a1", "camelCase").forEach {
                    withClue("name: $it") { tsPropertyName(it) shouldBe it }
                }
            }

            "anything else is quoted, and round-trips as a literal" {
                // `@type` is the realistic case: a Jackson-interop hierarchy overriding the
                // discriminator. `1st` and `kebab-case` are not valid bare identifiers either.
                val quoted = listOf("@type", "1st", "kebab-case", "with space", "it's", "")

                quoted.forEach { name ->
                    withClue("name: $name") {
                        tsPropertyName(name) shouldBe tsStringLiteral(name)
                        decodeJsSingleQuoted(tsPropertyName(name)) shouldBe name
                    }
                }
            }

            "a name is either a bare identifier or a decodable literal — never raw" {
                (hostile + listOf("@type", "1st", "ok")).forEach { name ->
                    withClue("name: $name") {
                        val rendered = tsPropertyName(name)

                        if (rendered != name) {
                            decodeJsSingleQuoted(rendered) shouldBe name
                        }
                    }
                }
            }
        }
    }
}
