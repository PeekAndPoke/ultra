package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RANGE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-RANGE-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "RANGE( 1, 4 )",
            RANGE(1.aql, 4.aql),
            listOf(1L, 2L, 3L, 4L)
        ),
        tuple(
            "RANGE( 1, 4, 2 )",
            RANGE(1.aql, 4.aql, 2.aql),
            listOf(1.0, 3.0)
        ),
        tuple(
            "RANGE( 1, 4, -2 )",
            RANGE(1.aql, 4.aql, (-2).aql),
            null
        ),
        tuple(
            "RANGE( 1.5, 4.4 )",
            RANGE(1.5.aql, 4.4.aql),
            listOf(1L, 2L, 3L, 4L)
        ),
        tuple(
            "RANGE( 1.5, 4.4, 1.0 )",
            RANGE(1.5.aql, 4.4.aql, 1.0.aql),
            listOf(1.5, 2.5, 3.5)
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            val result = karangoDriver.query {
                @Suppress("UNCHECKED_CAST")
                RETURN(expression) as TerminalExpr<List<Number>> // Don't do this at home... just a workaround for the different return types
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                @Suppress("UNCHECKED_CAST")
                RETURN(l) as TerminalExpr<List<Number>> // Don't do this at home... just a workaround for the different return types
            }

            withDetailedClue(expression, expected) {
                result.toList() shouldBe listOf(expected)
            }
        }
    }
})
