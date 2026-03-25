package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RADIANS
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.SIN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_SIN_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "SIN(3.141592653589783 / 2)",
            SIN((3.141592653589783 / 2).aql),
            1.0
        ),
        tuple(
            "SIN(0)",
            SIN(0.aql),
            0.0
        ),
        tuple(
            "SIN(-3.141592653589783 / 2)",
            SIN((-3.141592653589783 / 2).aql),
            -1.0
        ),
        tuple(
            "SIN(RADIANS(270))",
            SIN(RADIANS(270.aql)),
            -1.0
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
