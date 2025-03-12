package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.STDDEV_SAMPLE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.age
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-STDDEV_SAMPLE-Spec` : StringSpec({

    "STDDEV_SAMPLE from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                STDDEV_SAMPLE(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 10L
    }

    "STDDEV_SAMPLE from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30)
                )
            }

            RETURN(
                STDDEV_SAMPLE(
                    FOR(persons) { p ->
                        FILTER(p.age GT 10)
                        RETURN(p.age)
                    }
                )
            )
        }

        result.first() shouldBe 7.0710678118654755
    }

    val cases = listOf(
        row(
            "STDDEV_SAMPLE( [] ) - ARRAY",
            STDDEV_SAMPLE(ARRAY()),
            null
        ),
        row(
            "STDDEV_SAMPLE( [1] ) - ARRAY",
            STDDEV_SAMPLE(ARRAY(1.aql)),
            null
        ),
        row(
            "STDDEV_SAMPLE( [1, 1] ) - ARRAY",
            STDDEV_SAMPLE(ARRAY(1.aql, 1.aql)),
            0.0
        ),
        row(
            "STDDEV_SAMPLE( [ 1, 3, 6, 5, 2 ] ) - ARRAY",
            STDDEV_SAMPLE(ARRAY(1.aql, 3.aql, 6.aql, 5.aql, 2.aql)),
            2.0736441353327724
        ),
        row(
            "STDDEV_SAMPLE( [ 1, 3, 6, 5, 2 ] ) - listOf",
            STDDEV_SAMPLE(listOf(1, 3, 6, 5, 2).aql),
            2.0736441353327724
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
