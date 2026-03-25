package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.SHIFT
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_SHIFT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "SHIFT ([])",
            SHIFT(ARRAY()),
            listOf()
        ),
        tuple(
            "SHIFT ([1])",
            SHIFT(ARRAY(1.aql)),
            listOf()
        ),
        tuple(
            "SHIFT ([1, 2])",
            SHIFT(ARRAY(1.aql, 2.aql)),
            listOf(2)
        ),
        tuple(
            "SHIFT ([1, 2, 3])",
            SHIFT(ARRAY(1.aql, 2.aql, 3.aql)),
            listOf(2, 3)
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
