package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SUM
import de.peekandpoke.karango.aql.TO_NUMBER
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.age
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-SUM-Spec` : StringSpec({

    "SUM from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                SUM(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 60.0
    }

    "SUM from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30)
                )
            }

            RETURN(
                SUM(
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
            "SUM( [1, 2, 3, 4] ) - listOf",
            SUM(
                listOf(1, 2, 3, 4).aql
            ),
            10.0
        ),
        tuple(
            "SUM( [1.5, 2, 3, 4] ) - listOf",
            SUM(
                listOf<Number>(1.5, 2, 3, 4).aql
            ),
            10.5
        ),
        tuple(
            "SUM( [1, 2, 3, 4] ) - ARRAY",
            SUM(
                ARRAY(1.aql, 2.aql, 3.aql, 4.aql)
            ),
            10.0
        ),
        tuple(
            "SUM( [1.5, 2, 3, 4] ) - ARRAY",
            SUM(
                ARRAY<Number>(1.5.aql, 2.aql, 3.aql, 4.aql)
            ),
            10.5
        ),
        tuple(
            "SUM( [null, -5, 6] )",
            SUM(
                ARRAY(null.aql.TO_NUMBER, (-5).aql, 6.aql)
            ),
            1.0
        ),
        tuple(
            "SUM( [ ] )",
            SUM(
                ARRAY()
            ),
            0.0
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
