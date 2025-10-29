package de.peekandpoke.karango.e2e.functions.vwxyz

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.VARIANCE_SAMPLE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.age
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_VARIANCE_SAMPLE_Spec : StringSpec({

    "VARIANCE_SAMPLE from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                VARIANCE_SAMPLE(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 100.0
    }

    "VARIANCE_SAMPLE from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30)
                )
            }

            RETURN(
                VARIANCE_SAMPLE(
                    FOR(persons) { p ->
                        FILTER(p.age GT 10)
                        RETURN(p.age)
                    }
                )
            )
        }

        result.first() shouldBe 50.0
    }

    val cases = listOf(
        tuple(
            "VARIANCE_SAMPLE( [] ) - ARRAY",
            VARIANCE_SAMPLE(ARRAY()),
            null
        ),
        tuple(
            "VARIANCE_SAMPLE( [1] ) - ARRAY",
            VARIANCE_SAMPLE(ARRAY(1.aql)),
            null
        ),
        tuple(
            "VARIANCE_SAMPLE( [1, 1] ) - ARRAY",
            VARIANCE_SAMPLE(ARRAY(1.aql, 1.aql)),
            0.0
        ),
        tuple(
            "VARIANCE_SAMPLE( [ 1, 3, 6, 5, 2.0 ] ) - ARRAY",
            VARIANCE_SAMPLE(ARRAY<Number>(1.aql, 3.aql, 6.aql, 5.aql, 2.0.aql)),
            4.300000000000001
        ),
        tuple(
            "VARIANCE_SAMPLE( [ 1, 3, 6, 5, 2 ] ) - listOf",
            VARIANCE_SAMPLE(listOf<Number>(1, 3, 6, 5, 2).aql),
            4.300000000000001
        ),
        tuple(
            "VARIANCE_SAMPLE( [ 1.0, 3, 6, 5, 2 ] ) - listOf",
            VARIANCE_SAMPLE(listOf<Number>(1.0, 3, 6, 5, 2).aql),
            4.300000000000001
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
