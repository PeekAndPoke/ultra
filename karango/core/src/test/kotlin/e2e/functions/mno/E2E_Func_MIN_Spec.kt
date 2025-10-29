package de.peekandpoke.karango.e2e.functions.mno

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.MIN
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_MIN_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "MIN( [] )",
            MIN(listOf<Number>().aql),
            null
        ),
        tuple(
            "MIN( [5, 9, -2, 1] )",
            MIN(listOf(5, 9, -2, 1).aql),
            -2L
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
