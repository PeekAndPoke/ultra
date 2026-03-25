package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.REVERSE
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_REVERSE_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "REVERSE( [] )",
            REVERSE(ARRAY()),
            listOf()
        ),
        tuple(
            "REVERSE( ['a'] )",
            REVERSE(ARRAY("a".aql)),
            listOf("a")
        ),
        tuple(
            "REVERSE( ['a', 'b'] ) - ARRAY",
            REVERSE(ARRAY("a".aql, "b".aql)),
            listOf("b", "a")
        ),
        tuple(
            "REVERSE( ['a', 'b'] ) - listOf",
            REVERSE(listOf("a", "b").aql),
            listOf("b", "a")
        ),
        tuple(
            "REVERSE( ['a', 'b', 'c'] ) - ARRAY",
            REVERSE(ARRAY("a".aql, "b".aql, "c".aql)),
            listOf("c", "b", "a")
        ),
        tuple(
            "REVERSE( ['a', 'b', 'c'] ) - listOf",
            REVERSE(listOf("a", "b", "c").aql),
            listOf("c", "b", "a")
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
