package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SQRT
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-SQRT-Spec` : StringSpec({

    val cases = listOf(
        row(
            "SQRT(9)",
            SQRT(9.aql),
            3.0
        ),
        row(
            "SQRT(2)",
            SQRT(2.aql),
            1.4142135623730951
        ),
        row(
            "SQRT(0)",
            SQRT(0.aql),
            0.0
        ),
        row(
            "SQRT(-1)",
            SQRT((-1).aql),
            null
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
