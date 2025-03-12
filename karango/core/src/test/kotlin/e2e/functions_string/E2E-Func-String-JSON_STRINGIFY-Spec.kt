package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.JSON_STRINGIFY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
open class `E2E-Func-String-JSON_STRINGIFY-Spec` : StringSpec() {

    val cases = listOf(
        row(
            "JSON_STRINGIFY of null",
            JSON_STRINGIFY(null.aql),
            "null"
        ),
        row(
            "JSON_STRINGIFY of integer",
            JSON_STRINGIFY(1.aql),
            "1"
        ),
        row(
            "JSON_STRINGIFY of string",
            JSON_STRINGIFY("string".aql),
            "\"string\""
        ),
        row(
            "JSON_STRINGIFY of array",
            JSON_STRINGIFY(listOf("a", 1).aql),
            """["a",1]"""
        ),
        row(
            "JSON_STRINGIFY of object",
            JSON_STRINGIFY(E2ePerson("a", 1).aql),
            """{"name":"a","age":1}"""
        )
    )

    init {
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
    }
}
