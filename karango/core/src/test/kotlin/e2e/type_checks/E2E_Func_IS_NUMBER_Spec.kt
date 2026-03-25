package io.peekandpoke.karango.e2e.type_checks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.IS_NUMBER
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.E2ePerson
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_IS_NUMBER_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_NUMBER(true)",
            IS_NUMBER(true.aql),
            false
        ),
        tuple(
            "IS_NUMBER(false)",
            IS_NUMBER(true.aql),
            false
        ),
        tuple(
            "IS_NUMBER(null)",
            IS_NUMBER(null.aql),
            false
        ),
        tuple(
            "IS_NUMBER(0)",
            IS_NUMBER(0.aql),
            true
        ),
        tuple(
            "IS_NUMBER(1)",
            IS_NUMBER(1.aql),
            true
        ),
        tuple(
            "IS_NUMBER(-1.5)",
            IS_NUMBER((-1.5).aql),
            true
        ),
        tuple(
            "IS_NUMBER(\"1\")",
            IS_NUMBER("1".aql),
            false
        ),
        tuple(
            "IS_NUMBER(\"a\")",
            IS_NUMBER("a".aql),
            false
        ),
        tuple(
            "IS_NUMBER(\"\")",
            IS_NUMBER("".aql),
            false
        ),
        tuple(
            "IS_NUMBER([0])",
            IS_NUMBER(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_NUMBER(object)",
            IS_NUMBER(E2ePerson("name", 10).aql),
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
