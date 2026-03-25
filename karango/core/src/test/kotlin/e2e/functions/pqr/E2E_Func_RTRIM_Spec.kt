package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.RTRIM
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_RTRIM_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "RTRIM of \"foo bar\"",
            RTRIM("foo bar".aql),
            "foo bar"
        ),
        tuple(
            "RTRIM of \"  foo bar  \"",
            RTRIM("  foo bar  ".aql),
            "  foo bar"
        ),
        tuple(
            "RTRIM of \"--==[foo-bar]==--\" with chars \"-=[]\"",
            RTRIM("--==[foo-bar]==--".aql, "-=[]".aql),
            "--==[foo-bar"
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
