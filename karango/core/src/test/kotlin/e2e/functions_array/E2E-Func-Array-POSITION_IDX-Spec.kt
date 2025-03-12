package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.POSITION_IDX
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-POSITION_IDX-Spec` : StringSpec({

    val cases = listOf(
        row(
            "POSITION_IDX ([], 0)",
            POSITION_IDX(ARRAY(), 0.aql),
            -1L
        ),
        row(
            "POSITION_IDX ([1], 1)",
            POSITION_IDX(ARRAY(1.aql), 1.aql),
            0L
        ),
        row(
            "POSITION_IDX ([1, 2, 3], 3)",
            POSITION_IDX(ARRAY(1.aql, 2.aql, 3.aql), 3.aql),
            2L
        ),
        row(
            "POSITION_IDX ([1, 2, 3], 4)",
            POSITION_IDX(ARRAY(1.aql, 2.aql, 3.aql), 4.aql),
            -1L
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
