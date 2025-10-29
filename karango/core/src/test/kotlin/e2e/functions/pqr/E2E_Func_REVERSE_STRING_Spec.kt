package de.peekandpoke.karango.e2e.functions.pqr

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.REVERSE_STRING
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_REVERSE_STRING_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "REVERSE( '' )",
            REVERSE_STRING("".aql),
            ""
        ),
        tuple(
            "REVERSE( 'a' )",
            REVERSE_STRING("a".aql),
            "a"
        ),
        tuple(
            "REVERSE( 'ab' ) - ARRAY",
            REVERSE_STRING("ab".aql),
            "ba"
        ),
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
