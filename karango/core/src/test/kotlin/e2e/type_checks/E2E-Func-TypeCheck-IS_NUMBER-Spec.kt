package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.IS_NUMBER
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
class `E2E-Func-TypeCheck-IS_NUMBER-Spec` : StringSpec({

    val cases = listOf(
        row(
            "IS_NUMBER(true)",
            IS_NUMBER(true.aql),
            false
        ),
        row(
            "IS_NUMBER(false)",
            IS_NUMBER(true.aql),
            false
        ),
        row(
            "IS_NUMBER(null)",
            IS_NUMBER(null.aql),
            false
        ),
        row(
            "IS_NUMBER(0)",
            IS_NUMBER(0.aql),
            true
        ),
        row(
            "IS_NUMBER(1)",
            IS_NUMBER(1.aql),
            true
        ),
        row(
            "IS_NUMBER(-1.5)",
            IS_NUMBER((-1.5).aql),
            true
        ),
        row(
            "IS_NUMBER(\"1\")",
            IS_NUMBER("1".aql),
            false
        ),
        row(
            "IS_NUMBER(\"a\")",
            IS_NUMBER("a".aql),
            false
        ),
        row(
            "IS_NUMBER(\"\")",
            IS_NUMBER("".aql),
            false
        ),
        row(
            "IS_NUMBER([0])",
            IS_NUMBER(ARRAY(0.aql)),
            false
        ),
        row(
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
