package io.peekandpoke.karango.e2e.functions.def

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.DEGREES
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_DEGREES_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "DEGREES(0.7853981633974483)",
            DEGREES(0.7853981633974483.aql),
            45.0
        ),
        tuple(
            "DEGREES(0)",
            DEGREES(0.aql),
            0.0
        ),
        tuple(
            "DEGREES(3.141592653589793)",
            DEGREES(3.141592653589793.aql),
            180.0
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
