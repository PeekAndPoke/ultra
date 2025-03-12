package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.CONCAT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_STRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-CONCAT-Spec` : StringSpec({

    val cases = listOf(
        row(
            "CONCAT with one empty string parameter",
            CONCAT("".aql),
            ""
        ),
        row(
            "CONCAT with two empty string parameters",
            CONCAT("".aql, "".aql),
            ""
        ),
        row(
            "CONCAT with multiple parameters",
            CONCAT("a".aql, "".aql, "b".aql),
            "ab"
        ),
        row(
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
