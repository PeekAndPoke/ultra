package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_OBJECT
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_OBJECT_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_OBJECT(object) - empty object",
            IS_OBJECT(mapOf<Any, Any>().aql),
            true
        ),
        tuple(
            "IS_OBJECT(object) - object with properties",
            IS_OBJECT(E2ePerson("name", 10).aql),
            true
        ),
        tuple(
            "IS_OBJECT(true)",
            IS_OBJECT(true.aql),
            false
        ),
        tuple(
            "IS_OBJECT(false)",
            IS_OBJECT(true.aql),
            false
        ),
        tuple(
            "IS_OBJECT(null)",
            IS_OBJECT(null.aql),
            false
        ),
        tuple(
            "IS_OBJECT(0)",
            IS_OBJECT(0.aql),
            false
        ),
        tuple(
            "IS_OBJECT(1)",
            IS_OBJECT(1.aql),
            false
        ),
        tuple(
            "IS_OBJECT(\"a\")",
            IS_OBJECT("a".aql),
            false
        ),
        tuple(
            "IS_OBJECT(\"\")",
            IS_OBJECT("".aql),
            false
        ),
        tuple(
            "IS_OBJECT([0]) - ARRAY",
            IS_OBJECT(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_OBJECT([0]) - listOf",
            IS_OBJECT(listOf(0).aql),
            false
        ),
        tuple(
            "IS_OBJECT([object])",
            IS_OBJECT(ARRAY(E2ePerson("name", 10).aql)),
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
