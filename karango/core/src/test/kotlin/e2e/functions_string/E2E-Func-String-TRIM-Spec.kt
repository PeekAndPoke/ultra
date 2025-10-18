package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TRIM
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-TRIM-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "TRIM of \"foo bar\"",
            TRIM("foo bar".aql),
            "foo bar"
        ),
        tuple(
            "TRIM of \"  foo bar  \"",
            TRIM("  foo bar  ".aql),
            "foo bar"
        ),
        tuple(
            "TRIM of \"--==[foo-bar]==--\" with chars \"-=[]\"",
            TRIM("--==[foo-bar]==--".aql, "-=[]".aql),
            "foo-bar"
        ),
        tuple(
            "TRIM of \"  foobar\\t \\r\\n \"",
            TRIM("  foobar\t \r\n ".aql),
            "foobar"
        ),
        tuple(
            "TRIM of \";foo;bar;baz, \" with chars \",; \"",
            TRIM(";foo;bar;baz, ".aql, ",; ".aql),
            "foo;bar;baz"
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
