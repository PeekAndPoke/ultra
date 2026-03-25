package io.peekandpoke.karango.e2e.functions.abc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.AVERAGE
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.age
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_AVERAGE_Spec : StringSpec({

    "AVERAGE from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                AVERAGE(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 20L
    }

    "AVERAGE from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30)
                )
            }

            RETURN(
                AVERAGE(
                    FOR(persons) { p ->
                        FILTER(p.age GT 10)
                        RETURN(p.age)
                    }
                )
            )
        }

        result.first() shouldBe 25.0
    }

    val cases = listOf(
        tuple(
            "AVERAGE( [5, 2, 9, 2] )",
            AVERAGE(listOf(5, 2, 9, 2).aql),
            4.5
        ),
        tuple(
            "AVERAGE( [ -3, -5, 2 ] )",
            AVERAGE(listOf(-3, -5, 2).aql),
            -2.0
        ),
        tuple(
            "AVG( [ 999, 80, 4, 4, 4, 3, 3, 3 ] )",
            AVERAGE(ARRAY(999.aql, 80.aql, 4.aql, 4.aql, 4.aql, 3.aql, 3.aql, 3.aql)),
            137.5
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
