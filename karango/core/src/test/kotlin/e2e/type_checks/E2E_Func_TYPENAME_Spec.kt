package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.TYPENAME
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_TYPENAME_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "TYPENAME(true)",
            TYPENAME(true.aql),
            "bool"
        ),
        tuple(
            "TYPENAME(false)",
            TYPENAME(true.aql),
            "bool"
        ),
        tuple(
            "TYPENAME(null)",
            TYPENAME(null.aql),
            "null"
        ),
        tuple(
            "TYPENAME(0)",
            TYPENAME(0.aql),
            "number"
        ),
        tuple(
            "TYPENAME(1.1)",
            TYPENAME(1.1.aql),
            "number"
        ),
        tuple(
            "TYPENAME(\"a\")",
            TYPENAME("a".aql),
            "string"
        ),
        tuple(
            "TYPENAME(\"\")",
            TYPENAME("".aql),
            "string"
        ),
        tuple(
            "TYPENAME(\"1\")",
            TYPENAME("1".aql),
            "string"
        ),
        tuple(
            "TYPENAME([])",
            TYPENAME(ARRAY<Any>()),
            "array"
        ),
        tuple(
            "TYPENAME([0])",
            TYPENAME(ARRAY(0.aql)),
            "array"
        ),
        tuple(
            "TYPENAME(object)",
            TYPENAME(E2ePerson("name", 10).aql),
            "object"
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
