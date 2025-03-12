package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.COS
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RADIANS
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-COS-Spec` : StringSpec({

    val cases = listOf(
        row(
            "COS(1)",
            COS(1.aql),
            0.5403023058681398
        ),
        row(
            "COS(0)",
            COS(0.aql),
            1.0
        ),
        row(
            "COS(-3.141592653589783)",
            COS((-3.141592653589783).aql),
            -1.0
        ),
        row(
            "COS(RADIANS(45))",
            COS(RADIANS(45.aql)),
            0.7071067811865476
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
