package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.PUSH
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_PUSH_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "PUSH ([], 0)",
            PUSH(ARRAY(), 1.aql),
            listOf(1)
        ),
        tuple(
            "PUSH ([1], 'a')",
            PUSH<Any>(ARRAY(1.aql), "a".aql),
            listOf(1L, "a")
        ),
        tuple(
            "PUSH ([1], 1, true)",
            PUSH(ARRAY(1.aql), 1.aql, true.aql),
            listOf(1)
        ),
        tuple(
            "PUSH ([1, 1], 2, true)",
            PUSH(ARRAY(1.aql, 1.aql), 2.aql, true.aql),
            listOf(1, 1, 2)
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
