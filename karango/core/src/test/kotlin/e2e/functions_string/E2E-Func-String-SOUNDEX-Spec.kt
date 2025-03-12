package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SOUNDEX
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-SOUNDEX-Spec` : StringSpec({

    val cases = listOf(
        row(
            "SOUNDEX( \"example\" )",
            SOUNDEX("example".aql),
            "E251"
        ),
        row(
            "SOUNDEX( \"ekzampul\")",
            SOUNDEX("ekzampul".aql),
            "E251"
        ),
        row(
            "SOUNDEX( \"soundex\" )",
            SOUNDEX("soundex".aql),
            "S532"
        ),
        row(
            "SOUNDEX( \"sounteks\" )",
            SOUNDEX("sounteks".aql),
            "S532"
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
