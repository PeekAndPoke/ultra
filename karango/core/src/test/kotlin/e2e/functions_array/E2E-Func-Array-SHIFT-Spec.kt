package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SHIFT
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-SHIFT-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "SHIFT ([])",
            SHIFT(ARRAY()),
            listOf()
        ),
        tuple(
            "SHIFT ([1])",
            SHIFT(ARRAY(1.aql)),
            listOf()
        ),
        tuple(
            "SHIFT ([1, 2])",
            SHIFT(ARRAY(1.aql, 2.aql)),
            listOf(2)
        ),
        tuple(
            "SHIFT ([1, 2, 3])",
            SHIFT(ARRAY(1.aql, 2.aql, 3.aql)),
            listOf(2, 3)
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
