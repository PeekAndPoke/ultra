package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.UNION
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ARRAY
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-UNION-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "UNION ([], [])",
            UNION(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "UNION ([1], [])",
            UNION(ARRAY(1.aql), ARRAY()),
            listOf(1)
        ),
        tuple(
            "UNION ([1, 2], [2, 3])",
            UNION(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql)),
            listOf(1, 2, 2, 3)
        ),
        tuple(
            "UNION ([1, 2], ['a', 'b'])",
            UNION<Any>(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql)),
            listOf(1L, 2L, "a", "b")
        ),
        tuple(
            "UNION ([1, 2], ['a', 'b'], ['c'])",
            UNION<Any>(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql), ARRAY("c".aql)),
            listOf(1L, 2L, "a", "b", "c")
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
