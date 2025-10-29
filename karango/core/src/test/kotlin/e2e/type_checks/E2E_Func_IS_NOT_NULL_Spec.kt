package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.IS_NOT_NULL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_IS_NOT_NULL_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_NOT_NULL(true)",
            IS_NOT_NULL(true.aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(false)",
            IS_NOT_NULL(true.aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(null)",
            IS_NOT_NULL(null.aql),
            true.not()
        ),
        tuple(
            "IS_NOT_NULL(0)",
            IS_NOT_NULL(0.aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(1)",
            IS_NOT_NULL(1.aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(\"a\")",
            IS_NOT_NULL("a".aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(\"\")",
            IS_NOT_NULL("".aql),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL([0])",
            IS_NOT_NULL(ARRAY(0.aql)),
            false.not()
        ),
        tuple(
            "IS_NOT_NULL(object)",
            IS_NOT_NULL(E2ePerson("name", 10).aql),
            false.not()
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
