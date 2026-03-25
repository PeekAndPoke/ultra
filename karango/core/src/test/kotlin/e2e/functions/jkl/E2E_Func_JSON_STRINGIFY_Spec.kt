package io.peekandpoke.karango.e2e.functions.jkl

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.JSON_STRINGIFY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
open class E2E_Func_JSON_STRINGIFY_Spec : StringSpec() {

    val cases = listOf(
        tuple(
            "JSON_STRINGIFY of null",
            JSON_STRINGIFY(null.aql),
            "null"
        ),
        tuple(
            "JSON_STRINGIFY of integer",
            JSON_STRINGIFY(1.aql),
            "1"
        ),
        tuple(
            "JSON_STRINGIFY of string",
            JSON_STRINGIFY("string".aql),
            "\"string\""
        ),
        tuple(
            "JSON_STRINGIFY of array",
            JSON_STRINGIFY(listOf("a", 1).aql),
            """["a",1]"""
        ),
        tuple(
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
