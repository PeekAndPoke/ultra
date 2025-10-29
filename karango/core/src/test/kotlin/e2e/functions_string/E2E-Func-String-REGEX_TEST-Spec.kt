package e2e.functions_string

import de.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REGEX_TEST
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-REGEX_TEST-Spec` : StringSpec({

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
