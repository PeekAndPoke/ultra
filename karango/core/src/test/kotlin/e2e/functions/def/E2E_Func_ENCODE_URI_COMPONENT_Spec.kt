package io.peekandpoke.karango.e2e.functions.def

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ENCODE_URI_COMPONENT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_ENCODE_URI_COMPONENT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "ENCODE_URI_COMPONENT ( \"foo bar\" ))",
            ENCODE_URI_COMPONENT("foo bar".aql),
            "foo%20bar"
        ),
        tuple(
            "ENCODE_URI_COMPONENT ( \"föö bär\" ))",
            ENCODE_URI_COMPONENT("föö bär".aql),
            "f%C3%B6%C3%B6%20b%C3%A4r"
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
