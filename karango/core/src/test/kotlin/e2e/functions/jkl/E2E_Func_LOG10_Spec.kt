package de.peekandpoke.karango.e2e.functions.jkl

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LOG10
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_LOG10_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "LOG10(10000)",
            LOG10(10000.aql),
            4.0
        ),
        tuple(
            "LOG10(10)",
            LOG10(10.aql),
            1.0
        ),
        tuple(
            "LOG10(0)",
            LOG10(0.aql),
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
