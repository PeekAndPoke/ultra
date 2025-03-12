package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.REVERSE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Array-REVERSE-Spec` : StringSpec({

    val cases = listOf(
        row(
            "REVERSE( [] )",
            REVERSE(ARRAY()),
            listOf()
        ),
        row(
            "REVERSE( ['a'] )",
            REVERSE(ARRAY("a".aql)),
            listOf("a")
        ),
        row(
            "REVERSE( ['a', 'b'] ) - ARRAY",
            REVERSE(ARRAY("a".aql, "b".aql)),
            listOf("b", "a")
        ),
        row(
            "REVERSE( ['a', 'b'] ) - listOf",
            REVERSE(listOf("a", "b").aql),
            listOf("b", "a")
        ),
        row(
            "REVERSE( ['a', 'b', 'c'] ) - ARRAY",
            REVERSE(ARRAY("a".aql, "b".aql, "c".aql)),
            listOf("c", "b", "a")
        ),
        row(
            "REVERSE( ['a', 'b', 'c'] ) - listOf",
            REVERSE(listOf("a", "b", "c").aql),
            listOf("c", "b", "a")
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            repeat(10) {
                val result = karangoDriver.query {
                    RETURN(expression)
                }

                withDetailedClue(expression, expected) {
                    result.toList() shouldBe listOf(expected)
                }
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
