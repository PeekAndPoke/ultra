package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.REMOVE_NTH
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_REMOVE_NTH_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "REMOVE_NTH ([], 0)",
            REMOVE_NTH(ARRAY(), 0.aql),
            listOf()
        ),
        tuple(
            "REMOVE_NTH ([1], 0)",
            REMOVE_NTH(ARRAY(1.aql), 0.aql),
            listOf()
        ),
        tuple(
            "REMOVE_NTH ([1], -1)",
            REMOVE_NTH(ARRAY(1.aql), (-1).aql),
            listOf()
        ),
        tuple(
            "REMOVE_NTH ([1], 1)",
            REMOVE_NTH(ARRAY(1.aql), 1.aql),
            listOf(1)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], 0)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 0.aql),
            listOf(2)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], 1)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 1.aql),
            listOf(1)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], 2)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 2.aql),
            listOf(1, 2)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], -1)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-1).aql),
            listOf(1)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], -2)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-2).aql),
            listOf(2)
        ),
        tuple(
            "REMOVE_NTH ([1, 2], -3)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-3).aql),
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
