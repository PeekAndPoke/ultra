package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.JSON_PARSE
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
open class `E2E-Func-String-JSON_PARSE-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "JSON_PARSE of single quoted string -> parse error",
            JSON_PARSE("'string'".aql),
            null
        ),
        tuple(
            "JSON_PARSE of broken json -> parse error",
            JSON_PARSE("broken".aql),
            null
        ),
        tuple(
            "JSON_PARSE of 'null'",
            JSON_PARSE("null".aql),
            null
        ),
        tuple(
            "JSON_PARSE of '1'",
            JSON_PARSE("1".aql),
            1L
        ),
        tuple(
            "JSON_PARSE of '-1.1'",
            JSON_PARSE("-1.1".aql),
            -1.1
        ),
        tuple(
            "JSON_PARSE of 'string'",
            JSON_PARSE("\"string\"".aql),
            "string"
        ),
        tuple(
            "JSON_PARSE of [1, \"a\"]",
            JSON_PARSE("""[1, "a"]""".aql),
            listOf(1L, "a")
        ),
        tuple(
            "JSON_PARSE of {a:1, b:[1, 2]}",
            JSON_PARSE("""{"a":1, "b":[1, 2]}""".aql),
            mapOf("a" to 1L, "b" to listOf(1L, 2L))
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            val result = karangoDriver.query {
                RETURN(expression)
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                RETURN(l)
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }
    }
})
