package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-TypeCheck-IS_NULL-Spec` : StringSpec({

    val cases = listOf(
        row(
            "IS_NULL(true)",
            IS_NULL(true.aql),
            false
        ),
        row(
            "IS_NULL(false)",
            IS_NULL(true.aql),
            false
        ),
        row(
            "IS_NULL(null)",
            IS_NULL(null.aql),
            true
        ),
        row(
            "IS_NULL(0)",
            IS_NULL(0.aql),
            false
        ),
        row(
            "IS_NULL(1)",
            IS_NULL(1.aql),
            false
        ),
        row(
            "IS_NULL(\"a\")",
            IS_NULL("a".aql),
            false
        ),
        row(
            "IS_NULL(\"\")",
            IS_NULL("".aql),
            false
        ),
        row(
            "IS_NULL([0])",
            IS_NULL(ARRAY(0.aql)),
            false
        ),
        row(
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
