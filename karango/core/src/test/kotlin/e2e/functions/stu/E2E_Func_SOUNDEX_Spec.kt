package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.SOUNDEX
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

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
