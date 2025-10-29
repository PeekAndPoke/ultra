package de.peekandpoke.karango.e2e.functions.abc

import de.peekandpoke.karango.aql.CEIL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_CEIL_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "CEIL(2)",
            CEIL(2.aql),
            2.0
        ),
        tuple(
            "CEIL(2.49)",
            CEIL(2.49.aql),
            3.0
        ),
        tuple(
            "CEIL(2.50)",
            CEIL(2.50.aql),
            3.0
        ),
        tuple(
            "CEIL(-2.50)",
            CEIL((-2.50).aql),
            -2.0
        ),
        tuple(
            "CEIL(-2.51)",
            CEIL((-2.51).aql),
            -2.0
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
