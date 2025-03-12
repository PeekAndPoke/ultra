package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.ROUND
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-ROUND-Spec` : StringSpec({

    val cases = listOf(
        row(
            "ROUND(2.49)",
            ROUND(2.49.aql),
            2.0
        ),
        row(
            "ROUND(2.50)",
            ROUND(2.50.aql),
            3.0
        ),
        row(
            "ROUND(-2.50)",
            ROUND((-2.50).aql),
            -2.0
        ),
        row(
            "ROUND(-2.51)",
            ROUND((-2.51).aql),
            -3.0
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
