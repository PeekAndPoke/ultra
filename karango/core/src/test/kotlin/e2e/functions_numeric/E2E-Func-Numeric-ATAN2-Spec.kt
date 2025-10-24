package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.ATAN2
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-ATAN2-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "ATAN2(0, 0)",
            ATAN2(0.aql, 0.aql),
            0.0
        ),
        tuple(
            "ATAN2(1, 0)",
            ATAN2(1.aql, 0.aql),
            1.5707963267948966
        ),
        tuple(
            "ATAN2(1, 1)",
            ATAN2(1.aql, 1.aql),
            0.7853981633974483
        ),
        tuple(
            "ATAN2(-10, 20)",
            ATAN2((-10).aql, 20.aql),
            -0.4636476090008061
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
