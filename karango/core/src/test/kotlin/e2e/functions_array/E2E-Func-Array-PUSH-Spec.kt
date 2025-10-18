package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.PUSH
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
class `E2E-Func-Array-PUSH-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "PUSH ([], 0)",
            PUSH(ARRAY(), 1.aql),
            listOf(1)
        ),
        tuple(
            "PUSH ([1], 'a')",
            PUSH<Any>(ARRAY(1.aql), "a".aql),
            listOf(1L, "a")
        ),
        tuple(
            "PUSH ([1], 1, true)",
            PUSH(ARRAY(1.aql), 1.aql, true.aql),
            listOf(1)
        ),
        tuple(
            "PUSH ([1, 1], 2, true)",
            PUSH(ARRAY(1.aql, 1.aql), 2.aql, true.aql),
            listOf(1, 1, 2)
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
