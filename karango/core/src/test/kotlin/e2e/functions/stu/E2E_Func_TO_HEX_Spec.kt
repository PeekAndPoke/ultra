package de.peekandpoke.karango.e2e.functions.stu

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TO_HEX
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_TO_HEX_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "TO_HEX (\"\")",
            TO_HEX("".aql),
            ""
        ),
        tuple(
            "TO_HEX (\"ABC\")",
            TO_HEX("ABC".aql),
            "414243"
        ),
        tuple(
            "TO_HEX (\"abc ?\")",
            TO_HEX("abc ?".aql),
            "616263203f"
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
