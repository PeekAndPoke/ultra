package de.peekandpoke.karango.e2e.functions.stu

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SUBSTRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_SUBSTRING_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "SUBSTRING(\"abc\", 0)",
            SUBSTRING("abc".aql, 0.aql),
            "abc"
        ),
        tuple(
            "SUBSTRING(\"abc\", 1)",
            SUBSTRING("abc".aql, 1.aql),
            "bc"
        ),
        tuple(
            "SUBSTRING(\"abc\", -1)",
            SUBSTRING("abc".aql, (-1).aql),
            "c"
        ),
        tuple(
            "SUBSTRING(\"abc\", -2, 1)",
            SUBSTRING("abc".aql, (-2).aql, 1.aql),
            "b"
        ),
        tuple(
            "SUBSTRING(\"abc\", 1, 0)",
            SUBSTRING("abc".aql, 1.aql, 0.aql),
            ""
        ),
        tuple(
            "SUBSTRING(\"abc\", 1, 1)",
            SUBSTRING("abc".aql, 1.aql, 1.aql),
            "b"
        ),
        tuple(
            "SUBSTRING(\"abc\", 1, 10)",
            SUBSTRING("abc".aql, 1.aql, 10.aql),
            "bc"
        ),
        tuple(
            "SUBSTRING(\"abc\", 1, -1)",
            SUBSTRING("abc".aql, 1.aql, (-1).aql),
            ""
        ),
        tuple(
            "SUBSTRING(\"abc\", -5, 2)",
            SUBSTRING("abc".aql, (-5).aql, 2.aql),
            "ab"
        ),
        tuple(
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
