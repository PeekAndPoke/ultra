package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REMOVE_NTH
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-REMOVE_NTH-Spec` : StringSpec({

    val cases = listOf(
        row(
            "REMOVE_NTH ([], 0)",
            REMOVE_NTH(ARRAY(), 0.aql),
            listOf()
        ),
        row(
            "REMOVE_NTH ([1], 0)",
            REMOVE_NTH(ARRAY(1.aql), 0.aql),
            listOf()
        ),
        row(
            "REMOVE_NTH ([1], -1)",
            REMOVE_NTH(ARRAY(1.aql), (-1).aql),
            listOf()
        ),
        row(
            "REMOVE_NTH ([1], 1)",
            REMOVE_NTH(ARRAY(1.aql), 1.aql),
            listOf(1)
        ),
        row(
            "REMOVE_NTH ([1, 2], 0)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 0.aql),
            listOf(2)
        ),
        row(
            "REMOVE_NTH ([1, 2], 1)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 1.aql),
            listOf(1)
        ),
        row(
            "REMOVE_NTH ([1, 2], 2)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), 2.aql),
            listOf(1, 2)
        ),
        row(
            "REMOVE_NTH ([1, 2], -1)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-1).aql),
            listOf(1)
        ),
        row(
            "REMOVE_NTH ([1, 2], -2)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-2).aql),
            listOf(2)
        ),
        row(
            "REMOVE_NTH ([1, 2], -3)",
            REMOVE_NTH(ARRAY(1.aql, 2.aql), (-3).aql),
            listOf(1, 2)
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
