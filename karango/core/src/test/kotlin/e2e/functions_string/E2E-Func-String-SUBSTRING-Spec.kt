package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SUBSTRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-SUBSTRING-Spec` : StringSpec({

    val cases = listOf(
        row(
            "SUBSTRING(\"abc\", 0)",
            SUBSTRING("abc".aql, 0.aql),
            "abc"
        ),
        row(
            "SUBSTRING(\"abc\", 1)",
            SUBSTRING("abc".aql, 1.aql),
            "bc"
        ),
        row(
            "SUBSTRING(\"abc\", -1)",
            SUBSTRING("abc".aql, (-1).aql),
            "c"
        ),
        row(
            "SUBSTRING(\"abc\", -2, 1)",
            SUBSTRING("abc".aql, (-2).aql, 1.aql),
            "b"
        ),
        row(
            "SUBSTRING(\"abc\", 1, 0)",
            SUBSTRING("abc".aql, 1.aql, 0.aql),
            ""
        ),
        row(
            "SUBSTRING(\"abc\", 1, 1)",
            SUBSTRING("abc".aql, 1.aql, 1.aql),
            "b"
        ),
        row(
            "SUBSTRING(\"abc\", 1, 10)",
            SUBSTRING("abc".aql, 1.aql, 10.aql),
            "bc"
        ),
        row(
            "SUBSTRING(\"abc\", 1, -1)",
            SUBSTRING("abc".aql, 1.aql, (-1).aql),
            ""
        ),
        row(
            "SUBSTRING(\"abc\", -5, 2)",
            SUBSTRING("abc".aql, (-5).aql, 2.aql),
            "ab"
        ),
        row(
            "SUBSTRING(\"abc\", -5, 3)",
            SUBSTRING("abc".aql, (-5).aql, 3.aql),
            "abc"
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
