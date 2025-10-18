package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.ASIN
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-ASIN-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "ASIN (1)",
            ASIN(1.aql),
            1.5707963267948966
        ),
        tuple(
            "ASIN (0)",
            ASIN(0.aql),
            0.0
        ),
        tuple(
            "ASIN (-1)",
            ASIN((-1).aql),
            -1.5707963267948966
        ),
        tuple(
            "ASIN (2)",
            ASIN(2.aql),
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
