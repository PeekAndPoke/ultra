package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.SORTED
import io.peekandpoke.karango.aql.UNION_DISTINCT
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_UNION_DISTINCT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "UNION_DISTINCT ([], [])",
            UNION_DISTINCT(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "UNION_DISTINCT ([1], [])",
            SORTED(UNION_DISTINCT(ARRAY(1.aql), ARRAY())),
            listOf(1),
        ),
        tuple(
            "UNION_DISTINCT ([1, 2], [2, 3])",
            SORTED(UNION_DISTINCT(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql))),
            listOf(1, 2, 3),
        ),
        tuple(
            "UNION_DISTINCT ([1, 2], [2, 3], [3, 4])",
            SORTED(UNION_DISTINCT(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql), ARRAY(3.aql, 4.aql))),
            listOf(1, 2, 3, 4),
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            val result = karangoDriver.query {
                RETURN(expression)
            }

            withDetailedClue(expression, expected) {
                result.toList().first() shouldBe expected
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                RETURN(l)
            }

            withDetailedClue(expression, expected) {
                result.toList().first() shouldBe expected
            }
        }
    }
})
