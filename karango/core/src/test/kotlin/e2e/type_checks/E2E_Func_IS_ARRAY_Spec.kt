package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_ARRAY_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_ARRAY(true)",
            IS_ARRAY(true.aql),
            false
        ),
        tuple(
            "IS_ARRAY(false)",
            IS_ARRAY(true.aql),
            false
        ),
        tuple(
            "IS_ARRAY(null)",
            IS_ARRAY(null.aql),
            false
        ),
        tuple(
            "IS_ARRAY(0)",
            IS_ARRAY(0.aql),
            false
        ),
        tuple(
            "IS_ARRAY(1)",
            IS_ARRAY(1.aql),
            false
        ),
        tuple(
            "IS_ARRAY(\"a\")",
            IS_ARRAY("a".aql),
            false
        ),
        tuple(
            "IS_ARRAY(\"\")",
            IS_ARRAY("".aql),
            false
        ),
        tuple(
            "IS_ARRAY([0]) - ARRAY",
            IS_ARRAY(ARRAY(0.aql)),
            true
        ),
        tuple(
            "IS_ARRAY([0]) - listOf",
            IS_ARRAY(listOf(0).aql),
            true
        ),
        tuple(
            "IS_ARRAY(object)",
            IS_ARRAY(E2ePerson("name", 10).aql),
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
