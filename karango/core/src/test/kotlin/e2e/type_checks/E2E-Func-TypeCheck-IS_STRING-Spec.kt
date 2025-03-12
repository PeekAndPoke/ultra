package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.IS_STRING
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
class `E2E-Func-TypeCheck-IS_STRING-Spec` : StringSpec({

    val cases = listOf(
        row(
            "IS_STRING(true)",
            IS_STRING(true.aql),
            false
        ),
        row(
            "IS_STRING(false)",
            IS_STRING(true.aql),
            false
        ),
        row(
            "IS_STRING(null)",
            IS_STRING(null.aql),
            false
        ),
        row(
            "IS_STRING(0)",
            IS_STRING(0.aql),
            false
        ),
        row(
            "IS_STRING(1)",
            IS_STRING(1.aql),
            false
        ),
        row(
            "IS_STRING(\"a\")",
            IS_STRING("a".aql),
            true
        ),
        row(
            "IS_STRING(\"\")",
            IS_STRING("".aql),
            true
        ),
        row(
            "IS_STRING([0])",
            IS_STRING(ARRAY(0.aql)),
            false
        ),
        row(
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
