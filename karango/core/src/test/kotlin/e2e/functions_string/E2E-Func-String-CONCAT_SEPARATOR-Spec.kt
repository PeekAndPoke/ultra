package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.CONCAT_SEPARATOR
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_STRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-CONCAT_SEPARATOR-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "CONCAT_SEPARATOR with empty separator one empty string parameter",
            CONCAT_SEPARATOR("".aql, "".aql),
            ""
        ),
        tuple(
            "CONCAT_SEPARATOR with non empty separator one empty string parameter",
            CONCAT_SEPARATOR(", ".aql, "".aql),
            ""
        ),
        tuple(
            "CONCAT_SEPARATOR with none empty separator and two empty string parameters",
            CONCAT_SEPARATOR(", ".aql, "a".aql, "b".aql),
            "a, b"
        ),
        tuple(
            "CONCAT_SEPARATOR with empty separator more parameters",
            CONCAT_SEPARATOR("".aql, "a".aql, "_".aql, 123.aql.TO_STRING),
            "a_123"
        ),
        tuple(
            "CONCAT_SEPARATOR with none empty separator more parameters",
            CONCAT_SEPARATOR(", ".aql, "a".aql, "_".aql, 123.aql.TO_STRING),
            "a, _, 123"
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
