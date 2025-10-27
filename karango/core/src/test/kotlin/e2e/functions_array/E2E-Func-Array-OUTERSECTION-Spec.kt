package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.OUTERSECTION
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-OUTERSECTION-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "OUTERSECTION ([ 1, 2, 3 ], [ 2, 3, 4 ], [ 3, 4, 5 ])",
            OUTERSECTION(listOf(1, 2, 3).aql, listOf(2, 3, 4).aql, listOf(3, 4, 5).aql),
            listOf(5, 1)
        ),
        tuple(
            "OUTERSECTION ([], [])",
            OUTERSECTION(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "OUTERSECTION ([1], [])",
            OUTERSECTION(ARRAY(1.aql), ARRAY()),
            listOf(1)
        ),
        tuple(
            "OUTERSECTION ([1, 2], [2, 3])",
            OUTERSECTION(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql)),
            listOf(3, 1)
        ),
        tuple(
            "OUTERSECTION ([1, 2], ['a', 'b'])",
            OUTERSECTION<Any>(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql)),
            listOf("b", "a", 2L, 1L)
        ),
        tuple(
            "OUTERSECTION ([1, 2, 'a'], [1, 'a', 'b'])",
            OUTERSECTION<Any>(ARRAY<Any>(1.aql, 2.aql, "a".aql), ARRAY<Any>(1.aql, "a".aql, "b".aql)),
            listOf("b", 2L)
        ),
        tuple(
            "OUTERSECTION ( [ [1, 2] ], [ [1, 2], ['a', 'b']] )",
            OUTERSECTION(ARRAY(ARRAY(1.aql, 2.aql)), ARRAY(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql))),
            listOf(listOf("a", "b"))
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
