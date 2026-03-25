package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPercentileMethod
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.PERCENTILE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_PERCENTILE_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "PERCENTILE( [1, 2, 3, 4], 50 )",
            PERCENTILE(listOf(1, 2, 3, 4).aql, 50.aql),
            2.0
        ),
        tuple(
            "PERCENTILE( [1, 2, 3, 4], 50, \"rank\" )",
            PERCENTILE(listOf(1, 2, 3, 4).aql, 50.aql, AqlPercentileMethod.RANK),
            2.0
        ),
        tuple(
            "PERCENTILE( [1, 2, 3, 4], 50, \"interpolation\" )",
            PERCENTILE(listOf(1, 2, 3, 4).aql, 50.aql, AqlPercentileMethod.INTERPOLATION),
            2.5
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
