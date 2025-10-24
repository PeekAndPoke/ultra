package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.ATAN
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-ATAN-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "ATAN (-1)",
            ATAN((-1).aql),
            -0.7853981633974483
        ),
        tuple(
            "ATAN (0)",
            ATAN(0.aql),
            0.0
        ),
        tuple(
            "ATAN (10)",
            ATAN(10.aql),
            1.4711276743037347
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
