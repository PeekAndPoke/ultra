package io.peekandpoke.karango.e2e.functions.pqr

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.REGEX_TEST
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_REGEX_TEST_Spec : StringSpec({

    val cases = listOf(
        tuple(
            REGEX_TEST("the quick brown fox".aql, "the.*fox".aql),
            true
        ),
        tuple(
            REGEX_TEST("the quick brown fox".aql, "the.*FOX".aql, false.aql),
            false
        ),
        tuple(
            REGEX_TEST("the quick brown fox".aql, "the.*FOX".aql, true.aql),
            true
        ),
    )

    for ((expression, expected) in cases) {

        val description = expression.printRawQuery()

        "$description - direct return" {

            repeat(10) {
                val result = karangoDriver.query {
                    RETURN(expression)
                }

                withDetailedClue(expression, expected) {
                    result.toList() shouldBe listOf(expected)
                }
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
