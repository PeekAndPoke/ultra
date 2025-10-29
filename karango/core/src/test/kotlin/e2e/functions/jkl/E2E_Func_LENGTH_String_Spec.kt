package de.peekandpoke.karango.e2e.functions.jkl

import de.peekandpoke.karango.aql.LENGTH
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_LENGTH_String_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "LENGTH on an empty string parameter",
            LENGTH("".aql),
            0L
        ),
        tuple(
            "LENGTH on a simple string parameter",
            LENGTH("1".aql),
            1L
        ),
        tuple(
            "LENGTH on another simple string parameter",
            LENGTH("12".aql),
            2L
        ),
        tuple(
            "LENGTH on a string with UTF-8 characters",
            LENGTH("äöüß".aql),
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
