package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.CONTAINS_ARRAY_IDX
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-CONTAINS_ARRAY_IDX-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "CONTAINS_ARRAY_IDX ([], 0)",
            CONTAINS_ARRAY_IDX(ARRAY(), 0.aql),
            -1L
        ),
        tuple(
            "CONTAINS_ARRAY_IDX ([1], 1)",
            CONTAINS_ARRAY_IDX(ARRAY(1.aql), 1.aql),
            0L
        ),
        tuple(
            "CONTAINS_ARRAY_IDX ([1, 2, 3], 3)",
            CONTAINS_ARRAY_IDX(ARRAY(1.aql, 2.aql, 3.aql), 3.aql),
            2L
        ),
        tuple(
            "CONTAINS_ARRAY_IDX ([1, 2, 3], 4)",
            CONTAINS_ARRAY_IDX(ARRAY(1.aql, 2.aql, 3.aql), 4.aql),
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
