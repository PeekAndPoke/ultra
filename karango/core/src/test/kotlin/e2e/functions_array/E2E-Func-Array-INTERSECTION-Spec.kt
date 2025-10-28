package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.INTERSECTION
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-INTERSECTION-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "INTERSECTION ([], [])",
            INTERSECTION(ARRAY(), ARRAY()),
            listOf()
        ),
        tuple(
            "INTERSECTION ([1], [])",
            INTERSECTION(ARRAY(1.aql), ARRAY()),
            listOf()
        ),
        tuple(
            "INTERSECTION ([1, 2], [2, 3])",
            INTERSECTION(ARRAY(1.aql, 2.aql), ARRAY(2.aql, 3.aql)),
            listOf(2)
        ),
        tuple(
            "INTERSECTION ([1, 2], ['a', 'b'])",
            INTERSECTION<Any>(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql)),
            listOf()
        ),
        tuple(
            "INTERSECTION ([1, 2], [1, 'a', 'b'])",
            INTERSECTION<Any>(ARRAY(1.aql, 2.aql), ARRAY<Any>(1.aql, "a".aql, "b".aql)),
            listOf(1L)
        ),
        tuple(
            "INTERSECTION ( [ [1, 2] ], [ [1, 2], ['a', 'b']] )",
            INTERSECTION<Any>(ARRAY(ARRAY(1.aql, 2.aql)), ARRAY(ARRAY(1.aql, 2.aql), ARRAY("a".aql, "b".aql))),
            listOf(listOf(1L, 2L))
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
