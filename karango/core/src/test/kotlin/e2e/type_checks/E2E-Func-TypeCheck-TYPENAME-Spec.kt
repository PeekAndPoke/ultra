package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.TYPENAME
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-TypeCheck-TYPENAME-Spec` : StringSpec({

    val cases = listOf(
        row(
            "TYPENAME(true)",
            TYPENAME(true.aql),
            "bool"
        ),
        row(
            "TYPENAME(false)",
            TYPENAME(true.aql),
            "bool"
        ),
        row(
            "TYPENAME(null)",
            TYPENAME(null.aql),
            "null"
        ),
        row(
            "TYPENAME(0)",
            TYPENAME(0.aql),
            "number"
        ),
        row(
            "TYPENAME(1.1)",
            TYPENAME(1.1.aql),
            "number"
        ),
        row(
            "TYPENAME(\"a\")",
            TYPENAME("a".aql),
            "string"
        ),
        row(
            "TYPENAME(\"\")",
            TYPENAME("".aql),
            "string"
        ),
        row(
            "TYPENAME(\"1\")",
            TYPENAME("1".aql),
            "string"
        ),
        row(
            "TYPENAME([])",
            TYPENAME(ARRAY<Any>()),
            "array"
        ),
        row(
            "TYPENAME([0])",
            TYPENAME(ARRAY(0.aql)),
            "array"
        ),
        row(
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
