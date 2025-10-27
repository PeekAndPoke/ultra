package de.peekandpoke.karango.e2e.functions_numeric

import de.peekandpoke.karango.aql.AqlPercentileMethod
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.PERCENTILE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-Numeric-PERCENTILE-Spec` : StringSpec({

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
