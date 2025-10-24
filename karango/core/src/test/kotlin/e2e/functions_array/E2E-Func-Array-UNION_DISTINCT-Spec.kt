package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SORTED
import de.peekandpoke.karango.aql.UNION_DISTINCT
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ARRAY
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-UNION_DISTINCT-Spec` : StringSpec({

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
                @Suppress("UNCHECKED_CAST")
                RETURN(expression) as TerminalExpr<Any>
            }

            withDetailedClue(expression, expected) {
                result.toList().first() shouldBe expected
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                @Suppress("UNCHECKED_CAST")
                RETURN(l) as TerminalExpr<Any>
            }

            withDetailedClue(expression, expected) {
                result.toList().first() shouldBe expected
            }
        }
    }
})
