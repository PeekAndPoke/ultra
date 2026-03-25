package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.UNSHIFT
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_UNSHIFT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "UNSHIFT ([], 0)",
            UNSHIFT(ARRAY(), 1.aql),
            listOf(1)
        ),
        tuple(
            "UNSHIFT ([1], 'a')",
            UNSHIFT<Any>(ARRAY(1.aql), "a".aql),
            listOf("a", 1L)
        ),
        tuple(
            "UNSHIFT ([1], 1, true)",
            UNSHIFT(ARRAY(1.aql), 1.aql, true.aql),
            listOf(1)
        ),
        tuple(
            "UNSHIFT ([1], 2, true)",
            UNSHIFT(ARRAY(1.aql), 2.aql, true.aql),
            listOf(2, 1)
        ),
        tuple(
            "UNSHIFT ([1, 1], 2, true)",
            UNSHIFT(ARRAY(1.aql, 1.aql), 2.aql, true.aql),
            listOf(2, 1, 1)
        ),
        tuple(
            "UNSHIFT ([1, 1, 2], 2, true)",
            UNSHIFT(ARRAY(1.aql, 1.aql, 2.aql), 2.aql, true.aql),
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
