package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.FIRST
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-FIRST-Spec` : StringSpec({

    val cases = listOf(
        row(
            "FIRST ([])",
            FIRST(ARRAY<Any>()),
            null
        ),
        row(
            "IS_NULL( FIRST ([]) )",
            IS_NULL(FIRST(ARRAY<Any>())),
            true
        ),
        row(
            "FIRST  (['a'])",
            FIRST(ARRAY("a".aql)),
            "a"
        ),
        row(
            "FIRST  (['a', 'b'])",
            FIRST(ARRAY("a".aql, "b".aql)),
            "a"
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
