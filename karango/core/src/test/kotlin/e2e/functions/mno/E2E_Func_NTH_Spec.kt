package io.peekandpoke.karango.e2e.functions.mno

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.NTH
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_NTH_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "NTH ([], 0)",
            NTH(ARRAY(), 0.aql),
            null
        ),
        tuple(
            "NTH ([1], 0)",
            NTH(ARRAY(1.aql), 0.aql),
            1
        ),
        tuple(
            "NTH ([1], -1)",
            NTH(ARRAY(1.aql), (-1).aql),
            null
        ),
        tuple(
            "NTH ([1], 1)",
            NTH(ARRAY(1.aql), 1.aql),
            null
        ),
        tuple(
            "NTH ([1, 2], 0)",
            NTH(ARRAY(1.aql, 2.aql), 0.aql),
            1
        ),
        tuple(
            "NTH ([1, 2], 1)",
            NTH(ARRAY(1.aql, 2.aql), 1.aql),
            2
        ),
        tuple(
            "NTH ([1, 2], 2)",
            NTH(ARRAY(1.aql, 2.aql), 2.aql),
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
