package io.peekandpoke.karango.e2e.functions.abc

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ASIN
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_ASIN_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "ASIN (1)",
            ASIN(1.aql),
            1.5707963267948966
        ),
        tuple(
            "ASIN (0)",
            ASIN(0.aql),
            0.0
        ),
        tuple(
            "ASIN (-1)",
            ASIN((-1).aql),
            -1.5707963267948966
        ),
        tuple(
            "ASIN (2)",
            ASIN(2.aql),
            null
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
