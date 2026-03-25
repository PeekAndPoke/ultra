package io.peekandpoke.karango.e2e.functions.abc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.CONCAT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.TO_STRING
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_CONCAT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "CONCAT with one empty string parameter",
            CONCAT("".aql),
            ""
        ),
        tuple(
            "CONCAT with two empty string parameters",
            CONCAT("".aql, "".aql),
            ""
        ),
        tuple(
            "CONCAT with multiple parameters",
            CONCAT("a".aql, "".aql, "b".aql),
            "ab"
        ),
        tuple(
            "CONCAT with more parameters",
            CONCAT("".aql, "a".aql, "_".aql, 123.aql.TO_STRING),
            "a_123"
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
