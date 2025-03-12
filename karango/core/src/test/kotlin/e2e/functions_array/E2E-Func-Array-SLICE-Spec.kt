package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SLICE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-SLICE-Spec` : StringSpec({

    val cases = listOf(
        row(
            "SLICE ( [ 1, 2, 3, 4, 5 ], 0, 1 )",
            SLICE(listOf(1, 2, 3, 4, 5).aql, 0.aql, 1.aql),
            listOf(1)
        ),
        row(
            "SLICE ( [ 1, 2, 3 ], 0, 1 )",
            SLICE(ARRAY(1.aql, 2.aql, 3.aql), 0.aql, 1.aql),
            listOf(1)
        ),
        row(
            "SLICE ( [ 1, 2, 3, 4, 5 ], 1, 2 )",
            SLICE(listOf(1, 2, 3, 4, 5).aql, 1.aql, 2.aql),
            listOf(2, 3)
        ),
        row(
            "SLICE ( [ 1, 2, 3, 4, 5 ], 3 )",
            SLICE(listOf(1, 2, 3, 4, 5).aql, 3.aql),
            listOf(4, 5)
        ),
        row(
            "SLICE ( [ 1, 2, 3, 4, 5 ], 1, -1 )",
            SLICE(listOf(1, 2, 3, 4, 5).aql, 1.aql, (-1).aql),
            listOf(2, 3, 4)
        ),
        row(
            "SLICE ( [ 1, 2, 3, 4, 5 ], 0, -2 )",
            SLICE(listOf(1, 2, 3, 4, 5).aql, 0.aql, (-2).aql),
            listOf(1, 2, 3)
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
