package de.peekandpoke.karango.e2e.functions.stu

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SOUNDEX
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_SOUNDEX_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "SOUNDEX( \"example\" )",
            SOUNDEX("example".aql),
            "E251"
        ),
        tuple(
            "SOUNDEX( \"ekzampul\")",
            SOUNDEX("ekzampul".aql),
            "E251"
        ),
        tuple(
            "SOUNDEX( \"soundex\" )",
            SOUNDEX("soundex".aql),
            "S532"
        ),
        tuple(
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
