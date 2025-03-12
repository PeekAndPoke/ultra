package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.COUNT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-COUNT-Spec` : StringSpec({

    val cases = listOf(
        row(
            "COUNT on an empty string parameter",
            COUNT("".aql),
            0L
        ),
        row(
            "COUNT on a simple string parameter",
            COUNT("1".aql),
            1L
        ),
        row(
            "COUNT on another simple string parameter",
            COUNT("12".aql),
            2L
        ),
        row(
            "COUNT on a string with UTF-8 characters",
            COUNT("äöüß".aql),
            4L
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
