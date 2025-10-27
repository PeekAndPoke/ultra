package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SPLIT
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-SPLIT-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "SPLIT( \"foo-bar-baz\", \"-\" )",
            SPLIT("foo-bar-baz".aql, "-".aql),
            listOf("foo", "bar", "baz")
        ),
        tuple(
            "SPLIT( \"foo-bar-baz\", \"-\", 1 )",
            SPLIT("foo-bar-baz".aql, "-".aql, 2.aql()),
            listOf("foo", "bar")
        ),
        tuple(
            "SPLIT( \"foo, bar & baz\", [ \", \", \" & \" ] ) - listOf",
            SPLIT("foo, bar & baz".aql, listOf(", ", " & ").aql),
            listOf("foo", "bar", "baz")
        ),
        tuple(
            "SPLIT( \"foo, bar & baz\", [ \", \", \" & \" ] ) - ARRAY",
            SPLIT("foo, bar & baz".aql, ARRAY(", ".aql, " & ".aql)),
            listOf("foo", "bar", "baz")
        ),
        tuple(
            "SPLIT( \"foo, bar & baz & buzz\", [ \", \", \" & \" ], 1 ) - listOf",
            SPLIT("foo, bar & baz & buzz".aql, listOf(", ", " & ").aql, 3.aql),
            listOf("foo", "bar", "baz")
        ),
        tuple(
            "SPLIT( \"foo, bar & baz & buzz\", [ \", \", \" & \" ], 1 ) - ARRAY",
            SPLIT("foo, bar & baz & buzz".aql, ARRAY(", ".aql, " & ".aql), 3.aql),
            listOf("foo", "bar", "baz")
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
