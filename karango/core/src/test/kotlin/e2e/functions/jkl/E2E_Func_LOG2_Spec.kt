package de.peekandpoke.karango.e2e.functions.jkl

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LOG2
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_LOG2_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "LOG2(1024)",
            LOG2(1024.aql),
            10.0
        ),
        tuple(
            "LOG2(8)",
            LOG2(8.aql),
            3.0
        ),
        tuple(
            "LOG2(0)",
            LOG2(0.aql),
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
