package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_STRING
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_STRING_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_STRING(true)",
            IS_STRING(true.aql),
            false
        ),
        tuple(
            "IS_STRING(false)",
            IS_STRING(true.aql),
            false
        ),
        tuple(
            "IS_STRING(null)",
            IS_STRING(null.aql),
            false
        ),
        tuple(
            "IS_STRING(0)",
            IS_STRING(0.aql),
            false
        ),
        tuple(
            "IS_STRING(1)",
            IS_STRING(1.aql),
            false
        ),
        tuple(
            "IS_STRING(\"a\")",
            IS_STRING("a".aql),
            true
        ),
        tuple(
            "IS_STRING(\"\")",
            IS_STRING("".aql),
            true
        ),
        tuple(
            "IS_STRING([0])",
            IS_STRING(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_STRING(object)",
            IS_STRING(E2ePerson("name", 10).aql),
            false
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
