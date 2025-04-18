package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.IS_ARRAY
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
class `E2E-Func-TypeCheck-IS_ARRAY-Spec` : StringSpec({

    val cases = listOf(
        row(
            "IS_ARRAY(true)",
            IS_ARRAY(true.aql),
            false
        ),
        row(
            "IS_ARRAY(false)",
            IS_ARRAY(true.aql),
            false
        ),
        row(
            "IS_ARRAY(null)",
            IS_ARRAY(null.aql),
            false
        ),
        row(
            "IS_ARRAY(0)",
            IS_ARRAY(0.aql),
            false
        ),
        row(
            "IS_ARRAY(1)",
            IS_ARRAY(1.aql),
            false
        ),
        row(
            "IS_ARRAY(\"a\")",
            IS_ARRAY("a".aql),
            false
        ),
        row(
            "IS_ARRAY(\"\")",
            IS_ARRAY("".aql),
            false
        ),
        row(
            "IS_ARRAY([0]) - ARRAY",
            IS_ARRAY(ARRAY(0.aql)),
            true
        ),
        row(
            "IS_ARRAY([0]) - listOf",
            IS_ARRAY(listOf(0).aql),
            true
        ),
        row(
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
