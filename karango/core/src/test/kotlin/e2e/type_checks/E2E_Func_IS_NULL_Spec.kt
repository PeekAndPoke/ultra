package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_NULL
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_NULL_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_NULL(true)",
            IS_NULL(true.aql),
            false
        ),
        tuple(
            "IS_NULL(false)",
            IS_NULL(true.aql),
            false
        ),
        tuple(
            "IS_NULL(null)",
            IS_NULL(null.aql),
            true
        ),
        tuple(
            "IS_NULL(0)",
            IS_NULL(0.aql),
            false
        ),
        tuple(
            "IS_NULL(1)",
            IS_NULL(1.aql),
            false
        ),
        tuple(
            "IS_NULL(\"a\")",
            IS_NULL("a".aql),
            false
        ),
        tuple(
            "IS_NULL(\"\")",
            IS_NULL("".aql),
            false
        ),
        tuple(
            "IS_NULL([0])",
            IS_NULL(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_NULL(object)",
            IS_NULL(E2ePerson("name", 10).aql),
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
