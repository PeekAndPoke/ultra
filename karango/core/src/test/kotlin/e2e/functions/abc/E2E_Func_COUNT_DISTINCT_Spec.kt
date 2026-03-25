package io.peekandpoke.karango.e2e.functions.abc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.COUNT_DISTINCT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_COUNT_DISTINCT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "COUNT_DISTINCT ([])",
            COUNT_DISTINCT(ARRAY<Any>()),
            0L
        ),
        tuple(
            "COUNT_DISTINCT (['a'])",
            COUNT_DISTINCT(ARRAY("a".aql)),
            1L
        ),
        tuple(
            "COUNT_DISTINCT (['a', 'a'])",
            COUNT_DISTINCT(ARRAY("a".aql, "a".aql)),
            1L
        ),
        tuple(
            "COUNT_DISTINCT (['a', 'b', 'a'])",
            COUNT_DISTINCT(ARRAY("a".aql, "b".aql, "a".aql)),
            2L
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
