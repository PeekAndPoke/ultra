package de.peekandpoke.karango.e2e.functions.def

import de.peekandpoke.karango.aql.EXP
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_EXP_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "EXP(1)",
            EXP(1.aql),
            2.718281828459045
        ),
        tuple(
            "EXP(10)",
            EXP(10.aql),
            22026.465794806718
        ),
        tuple(
            "EXP(0)",
            EXP(0.aql),
            1.0
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
