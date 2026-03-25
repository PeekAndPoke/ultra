package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_LIST
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_LIST_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_LIST(true)",
            IS_LIST(true.aql),
            false
        ),
        tuple(
            "IS_LIST(false)",
            IS_LIST(true.aql),
            false
        ),
        tuple(
            "IS_LIST(null)",
            IS_LIST(null.aql),
            false
        ),
        tuple(
            "IS_LIST(0)",
            IS_LIST(0.aql),
            false
        ),
        tuple(
            "IS_LIST(1)",
            IS_LIST(1.aql),
            false
        ),
        tuple(
            "IS_LIST(\"a\")",
            IS_LIST("a".aql),
            false
        ),
        tuple(
            "IS_LIST(\"\")",
            IS_LIST("".aql),
            false
        ),
        tuple(
            "IS_LIST([0]) - ARRAY",
            IS_LIST(ARRAY(0.aql)),
            true
        ),
        tuple(
            "IS_LIST([0]) - listOf",
            IS_LIST(listOf(0).aql),
            true
        ),
        tuple(
            "IS_LIST(object)",
            IS_LIST(E2ePerson("name", 10).aql),
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
