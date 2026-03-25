package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.POSITION
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_POSITION_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "POSITION ([], 0)",
            POSITION(ARRAY(), 0.aql),
            -1L
        ),
        tuple(
            "POSITION ([1], 1)",
            POSITION(ARRAY(1.aql), 1.aql),
            0L
        ),
        tuple(
            "POSITION ([1, 2, 3], 3)",
            POSITION(ARRAY(1.aql, 2.aql, 3.aql), 3.aql),
            2L
        ),
        tuple(
            "POSITION ([1, 2, 3], 4)",
            POSITION(ARRAY(1.aql, 2.aql, 3.aql), 4.aql),
            -1L
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
