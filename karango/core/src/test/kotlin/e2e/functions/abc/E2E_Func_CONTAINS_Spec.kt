package io.peekandpoke.karango.e2e.functions.abc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.CONCAT
import io.peekandpoke.karango.aql.CONTAINS
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_CONTAINS_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "prefix CONTAINS matching an input value",
            CONTAINS("abc".aql(), "b".aql()),
            true
        ),
        tuple(
            "prefix CONTAINS not matching on input value expression",
            CONTAINS("abc".aql(), "X".aql()),
            false
        ),
        tuple(
            "prefix CONTAINS matching two expressions",
            CONTAINS(CONCAT("abc".aql(), "def".aql()), CONCAT("c".aql(), "d".aql())),
            true
        ),
        tuple(
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
