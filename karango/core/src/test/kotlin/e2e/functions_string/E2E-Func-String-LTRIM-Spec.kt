package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LTRIM
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-LTRIM-Spec` : StringSpec({

    val cases = listOf(
        row(
            "LTRIM of \"foo bar\"",
            LTRIM("foo bar".aql),
            "foo bar"
        ),
        row(
            "LTRIM of \"  foo bar  \"",
            LTRIM("  foo bar  ".aql),
            "foo bar  "
        ),
        row(
            "LTRIM of \"--==[foo-bar]==--\" with chars \"-=[]\"",
            LTRIM("--==[foo-bar]==--".aql, "-=[]".aql),
            "foo-bar]==--"
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
