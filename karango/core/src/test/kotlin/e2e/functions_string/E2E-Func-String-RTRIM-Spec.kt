package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.RTRIM
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-RTRIM-Spec` : StringSpec({

    val cases = listOf(
        row(
            "RTRIM of \"foo bar\"",
            RTRIM("foo bar".aql),
            "foo bar"
        ),
        row(
            "RTRIM of \"  foo bar  \"",
            RTRIM("  foo bar  ".aql),
            "  foo bar"
        ),
        row(
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
