package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.ACOS
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-ACOS-Spec` : StringSpec({

    val cases = listOf(
        row(
            "ACOS (-1)",
            ACOS((-1).aql),
            3.141592653589793
        ),
        row(
            "ACOS (0)",
            ACOS(0.aql),
            1.5707963267948966
        ),
        row(
            "ACOS (1)",
            ACOS(1.aql),
            0.0
        ),
        row(
            "ACOS (2)",
            ACOS(2.aql),
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
