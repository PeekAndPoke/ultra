package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.MINUS
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ARRAY
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-MINUS-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "MINUS ([1,2,3,4], [3,4,5,6], [5,6,7,8])",
            MINUS(listOf(1, 2, 3, 4).aql, listOf(3, 4, 5, 6).aql, listOf(5, 6, 7, 8).aql),
            listOf(2, 1)
        ),
        tuple(
            "MINUS ([], [])",
            MINUS(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "MINUS ([1], [])",
            MINUS(ARRAY(1.aql), ARRAY()),
            listOf(1)
        ),
        tuple(
            "MINUS ([1, 2], [2, 3])",
            MINUS(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql)),
            listOf(1)
        ),
        tuple(
            "MINUS ([1, 2], ['a', 'b'])",
            MINUS<Any>(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql)),
            listOf(2L, 1L)
        ),
        tuple(
            "MINUS ([1, 2], [1, 'a', 'b'])",
            MINUS<Any>(ARRAY(1.aql, 2.aql), ARRAY<Any>(1.aql, "a".aql, "b".aql)),
            listOf(2L)
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            val result = karangoDriver.query {
                @Suppress("UNCHECKED_CAST")
                RETURN(expression) as TerminalExpr<Any>
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                @Suppress("UNCHECKED_CAST")
                RETURN(l) as TerminalExpr<Any>
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }
    }
})
