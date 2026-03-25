package io.peekandpoke.karango.e2e.functions.def

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.FIRST
import io.peekandpoke.karango.aql.IS_NULL
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_FIRST_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "FIRST ([])",
            FIRST(ARRAY<Any>()),
            null
        ),
        tuple(
            "IS_NULL( FIRST ([]) )",
            IS_NULL(FIRST(ARRAY<Any>())),
            true
        ),
        tuple(
            "FIRST  (['a'])",
            FIRST(ARRAY("a".aql)),
            "a"
        ),
        tuple(
            "FIRST  (['a', 'b'])",
            FIRST(ARRAY("a".aql, "b".aql)),
            "a"
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
