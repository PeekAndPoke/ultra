package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.AVERAGE
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
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
class `E2E-Func-Numeric-AVERAGE-Spec` : StringSpec({

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
        row(
            "AVERAGE( [5, 2, 9, 2] )",
            AVERAGE(listOf(5, 2, 9, 2).aql),
            4.5
        ),
        row(
            "AVERAGE( [ -3, -5, 2 ] )",
            AVERAGE(listOf(-3, -5, 2).aql),
            -2.0
        ),
        row(
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
