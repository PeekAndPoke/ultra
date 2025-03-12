package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.CONCAT
import de.peekandpoke.karango.aql.CONTAINS
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-CONTAINS-Spec` : StringSpec({

    val cases = listOf(
        row(
            "prefix CONTAINS matching an input value",
            CONTAINS("abc".aql(), "b".aql()),
            true
        ),
        row(
            "prefix CONTAINS not matching on input value expression",
            CONTAINS("abc".aql(), "X".aql()),
            false
        ),
        row(
            "prefix CONTAINS matching two expressions",
            CONTAINS(CONCAT("abc".aql(), "def".aql()), CONCAT("c".aql(), "d".aql())),
            true
        ),
        row(
            "prefix CONTAINS not matching two expression",
            CONTAINS(CONCAT("abc".aql(), "def".aql()), CONCAT("X".aql(), "Y".aql())),
            false
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
