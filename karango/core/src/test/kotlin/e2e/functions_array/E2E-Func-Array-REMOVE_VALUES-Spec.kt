package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REMOVE_VALUES
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-REMOVE_VALUESS-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "REMOVE_VALUES ([], [])",
            REMOVE_VALUES(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "REMOVE_VALUES ([1], [])",
            REMOVE_VALUES(ARRAY(1.aql), ARRAY()),
            listOf(1)
        ),
        tuple(
            "REMOVE_VALUES ([1], [1])",
            REMOVE_VALUES(ARRAY(1.aql), ARRAY(1.aql)),
            listOf()
        ),
        tuple(
            "REMOVE_VALUES ([1, 1, 2, 2, 3, 3, 4, 5], [1, 3, 4])",
            REMOVE_VALUES(ARRAY(1.aql, 1.aql, 2.aql, 2.aql, 3.aql, 4.aql, 5.aql), ARRAY(1.aql, 3.aql, 5.aql)),
            listOf(2, 2, 4)
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
