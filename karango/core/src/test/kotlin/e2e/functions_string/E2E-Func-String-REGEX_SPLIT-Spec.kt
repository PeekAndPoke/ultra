package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.AqlPrinter.Companion.printRawQuery
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REGEX_SPLIT
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-REGEX_SPLIT-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            REGEX_SPLIT("Capture the article".aql, "(the)".aql),
            listOf("Capture ", "the", " article")
        ),
        tuple(
            REGEX_SPLIT("Don't capture the article".aql, "the".aql),
            listOf("Don't capture ", " article")
        ),
        tuple(
            REGEX_SPLIT("cA,Bc,A,BcA,BcA,Bc".aql, "a,b".aql, true.aql),
            listOf("c", "c,", "c", "c", "c")
        ),
        tuple(
            REGEX_SPLIT("cA,Bc,A,BcA,BcA,Bc".aql, "a,b".aql, true.aql, 3.aql),
            listOf("c", "c,", "c")
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
