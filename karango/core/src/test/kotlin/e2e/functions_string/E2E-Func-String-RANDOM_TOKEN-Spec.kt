package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RANDOM_TOKEN
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

@Suppress("ClassName")
class `E2E-Func-String-RANDOM_TOKEN-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "RANDOM_TOKEN( 5 )",
            RANDOM_TOKEN(5.aql),
            5
        ),
        tuple(
            "RANDOM_TOKEN( 100 )",
            RANDOM_TOKEN(100.aql),
            100
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            repeat(10) {
                val result = karangoDriver.query {
                    RETURN(expression)
                }

                val first = result.first()

                withDetailedClue(expression, expected) {
                    first.length shouldBe expected
                    first shouldMatch "[a-zA-Z0-9]{$expected}"
                }
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                RETURN(l)
            }

            withDetailedClue(expression, expected) {
                result.first().length shouldBe expected
            }
        }
    }
})
