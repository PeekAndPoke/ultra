package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.MEDIAN
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
class `E2E-Func-Numeric-MEDIAN-Spec` : StringSpec({

    "MEDIAN from multiple LETs" {

        val result = karangoDriver.query {
            val a = LET("a", 10.aql)
            val b = LET("b", 20.aql)

            RETURN(
                MEDIAN(
                    ARRAY(a, b, 30.aql)
                )
            )
        }

        result.first() shouldBe 20L
    }

    "MEDIAN from multiple objects" {

        val result = karangoDriver.query {
            val persons = LET("persons") {
                listOf(
                    E2ePerson("a", 10),
                    E2ePerson("b", 20),
                    E2ePerson("c", 30),
                    E2ePerson("d", 100)
                )
            }

            RETURN(
                MEDIAN(
                    FOR(persons) { p ->
                        FILTER(p.age GT 10)
                        RETURN(p.age)
                    }
                )
            )
        }

        result.first() shouldBe 30.0
    }


    val cases = listOf(
        row(
            "MEDIAN( [1, 2, 3] ) - listOf",
            MEDIAN(listOf(1, 2, 3).aql),
            2.0
        ),
        row(
            "MEDIAN( [1, 2, 3] ) - ARRAY",
            MEDIAN(ARRAY(1.aql, 2.aql, 3.aql)),
            2.0
        ),
        row(
            "MEDIAN( [ 1, 2, 3, 4 ] )",
            MEDIAN(listOf(1, 2, 3, 4).aql),
            2.5
        ),
        row(
            "MEDIAN( [ 4, 2, 3, 1 ] )",
            MEDIAN(listOf(4, 2, 3, 1).aql),
            2.5
        ),
        row(
            "MEDIAN( [ 999, 80, 4, 4, 4, 3, 3, 3 ] )",
            MEDIAN(listOf(999, 80, 4, 4, 4, 3, 3, 3).aql),
            4.0
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
