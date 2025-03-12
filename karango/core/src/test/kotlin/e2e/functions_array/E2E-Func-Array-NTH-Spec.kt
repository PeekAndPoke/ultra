package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.NTH
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
class `E2E-Func-Array-NTH-Spec` : StringSpec({

    val cases = listOf(
        row(
            "NTH ([], 0)",
            NTH(ARRAY(), 0.aql),
            null
        ),
        row(
            "NTH ([1], 0)",
            NTH(ARRAY(1.aql), 0.aql),
            1
        ),
        row(
            "NTH ([1], -1)",
            NTH(ARRAY(1.aql), (-1).aql),
            null
        ),
        row(
            "NTH ([1], 1)",
            NTH(ARRAY(1.aql), 1.aql),
            null
        ),
        row(
            "NTH ([1, 2], 0)",
            NTH(ARRAY(1.aql, 2.aql), 0.aql),
            1
        ),
        row(
            "NTH ([1, 2], 1)",
            NTH(ARRAY(1.aql, 2.aql), 1.aql),
            2
        ),
        row(
            "NTH ([1, 2], 2)",
            NTH(ARRAY(1.aql, 2.aql), 2.aql),
            null
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
