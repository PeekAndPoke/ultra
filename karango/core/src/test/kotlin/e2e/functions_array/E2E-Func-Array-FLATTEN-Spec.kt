package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.FLATTEN
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-FLATTEN-Spec` : StringSpec({

    val cases = listOf(
        row(
            "FLATTEN ([])",
            FLATTEN(ARRAY<Any>()),
            listOf()
        ),
        row(
            "FLATTEN ([1])",
            FLATTEN(ARRAY(1.aql)),
            listOf(1L)
        ),
        row(
            "FLATTEN ([1], 0)",
            FLATTEN(ARRAY(1.aql), 0.aql),
            listOf(1L)
        ),
        row(
            "FLATTEN ([1], 1)",
            FLATTEN(ARRAY(1.aql), 1.aql),
            listOf(1L)
        ),
        row(
            "FLATTEN ([1], 2)",
            FLATTEN(ARRAY(1.aql), 2.aql),
            listOf(1L)
        ),
        row(
            "FLATTEN ([[1, 2]], -1)",
            FLATTEN(ARRAY(ARRAY(1.aql, 2.aql)), (-1).aql),
            listOf(1L, 2L)
        ),
        row(
            "FLATTEN ([[1, 2]], 0)",
            FLATTEN(ARRAY(ARRAY(1.aql, 2.aql)), 0.aql),
            listOf(1L, 2L)
        ),
        row(
            "FLATTEN ([[1]], 1)",
            FLATTEN(ARRAY(ARRAY(1.aql, 2.aql)), 1.aql),
            listOf(1L, 2L)
        ),
        row(
            "FLATTEN ([[1]], 2)",
            FLATTEN(ARRAY(ARRAY(1.aql, 2.aql)), 2.aql),
            listOf(1L, 2L)
        ),
        row(
            "FLATTEN ( [ [1, 2], 3 ])",
            FLATTEN(
                ARRAY(ARRAY(1.aql, 2.aql), 3.aql)
            ),
            listOf(1L, 2L, 3L)
        ),
        row(
            "FLATTEN ( [ [1, 2], 3, [ [4, 5], 6] ] )",
            FLATTEN(
                ARRAY(
                    ARRAY(1.aql, 2.aql),
                    3.aql,
                    ARRAY(
                        ARRAY(4.aql, 5.aql),
                        6.aql
                    )
                )
            ),
            listOf(1L, 2L, 3L, listOf(4L, 5L), 6L)
        ),
        row(
            "FLATTEN ( [ [1, 2], 3, [ [4, 5], 6] ], 2 )",
            FLATTEN(
                ARRAY(
                    ARRAY(1.aql, 2.aql),
                    3.aql,
                    ARRAY(
                        ARRAY(4.aql, 5.aql),
                        6.aql
                    )
                ),
                2.aql
            ),
            listOf(1L, 2L, 3L, 4L, 5L, 6L)
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
