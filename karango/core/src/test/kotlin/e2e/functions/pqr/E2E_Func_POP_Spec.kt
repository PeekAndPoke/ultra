package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.POP
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_POP_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "POP ([])",
            POP(ARRAY()),
            listOf()
        ),
        tuple(
            "POP ([1])",
            POP(ARRAY(1.aql)),
            listOf()
        ),
        tuple(
            "POP ([1, 2])",
            POP(ARRAY(1.aql, 2.aql)),
            listOf(1)
        ),
        tuple(
            "POP ([1, 2, 3])",
            POP(ARRAY(1.aql, 2.aql, 3.aql)),
            listOf(1, 2)
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
