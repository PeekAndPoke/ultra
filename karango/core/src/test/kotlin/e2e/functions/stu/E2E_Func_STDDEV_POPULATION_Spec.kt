package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.STDDEV
import io.peekandpoke.karango.aql.STDDEV_POPULATION
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.age
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_STDDEV_POPULATION_Spec : StringSpec({

    "STDDEV_POPULATION from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                STDDEV_POPULATION(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 8.16496580927726
    }

    "STDDEV_POPULATION from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30)
                )
            }

            RETURN(
                STDDEV_POPULATION(
                    FOR(persons) { p ->
                        FILTER(p.age GT 10)
                        RETURN(p.age)
                    }
                )
            )
        }

        result.first() shouldBe 5.0
    }

    val cases = listOf(
        tuple(
            "STDDEV_POPULATION( [] ) - ARRAY",
            STDDEV_POPULATION(ARRAY()),
            null
        ),
        tuple(
            "STDDEV_POPULATION( [1] ) - ARRAY",
            STDDEV_POPULATION(ARRAY(1.aql)),
            0.0
        ),
        tuple(
            "STDDEV_POPULATION( [1, 1] ) - ARRAY",
            STDDEV_POPULATION(ARRAY(1.aql, 1.aql)),
            0.0
        ),
        tuple(
            "STDDEV_POPULATION( [ 1, 3, 6, 5, 2 ] ) - ARRAY",
            STDDEV_POPULATION(listOf(1, 3, 6, 5, 2).aql),
            1.854723699099141
        ),
        tuple(
            "STDDEV_POPULATION( [ 1, 3, 6, 5, 2 ] ) - listOf",
            STDDEV_POPULATION(listOf(1, 3, 6, 5, 2).aql),
            1.854723699099141
        ),
        tuple(
            "STDDEV( [ 1, 3, 6, 5, 2 ] ) - ARRAY",
            STDDEV(ARRAY(1.aql, 3.aql, 6.aql, 5.aql, 2.aql)),
            1.854723699099141
        ),
        tuple(
            "STDDEV( [ 1, 3, 6, 5, 2 ] ) - listOf",
            STDDEV(listOf(1, 3, 6, 5, 2).aql),
            1.854723699099141
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
