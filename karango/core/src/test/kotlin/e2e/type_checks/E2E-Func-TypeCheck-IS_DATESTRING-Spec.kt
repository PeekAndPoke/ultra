package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.IS_DATESTRING
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
class `E2E-Func-TypeCheck-IS_DATESTRING-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "IS_DATESTRING('2019')",
            IS_DATESTRING("2019".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03')",
            IS_DATESTRING("2019-03".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01')",
            IS_DATESTRING("2019-03-01".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01 00:00')",
            IS_DATESTRING("2019-03-01 00:00".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01 23:59:59')",
            IS_DATESTRING("2019-03-01 23:59:59".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01T23:59:59')",
            IS_DATESTRING("2019-03-01T23:59:59".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01T23:59:59.000')",
            IS_DATESTRING("2019-03-01T23:59:59.000".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01T23:59:59.000Z')",
            IS_DATESTRING("2019-03-01T23:59:59.000Z".aql),
            true
        ),
        tuple(
            "IS_DATESTRING('2019-03-01T23:59:59+01:00')",
            IS_DATESTRING("2019-03-01T23:59:59+01:00".aql),
            true
        ),
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        tuple(
            "IS_DATESTRING('now')",
            IS_DATESTRING("NOW".aql),
            false
        ),
        tuple(
            "IS_DATESTRING('tomorrow')",
            IS_DATESTRING("tomorrow".aql),
            false
        ),
        tuple(
            "IS_DATESTRING(true)",
            IS_DATESTRING(true.aql),
            false
        ),
        tuple(
            "IS_DATESTRING(false)",
            IS_DATESTRING(true.aql),
            false
        ),
        tuple(
            "IS_DATESTRING(null)",
            IS_DATESTRING(null.aql),
            false
        ),
        tuple(
            "IS_DATESTRING(0)",
            IS_DATESTRING(0.aql),
            false
        ),
        tuple(
            "IS_DATESTRING(1)",
            IS_DATESTRING(1.aql),
            false
        ),
        tuple(
            "IS_DATESTRING(\"a\")",
            IS_DATESTRING("a".aql),
            false
        ),
        tuple(
            "IS_DATESTRING(\"\")",
            IS_DATESTRING("".aql),
            false
        ),
        tuple(
            "IS_DATESTRING([0]) - ARRAY",
            IS_DATESTRING(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_DATESTRING([0]) - listOf",
            IS_DATESTRING(listOf(0).aql),
            false
        ),
        tuple(
            "IS_DATESTRING([object])",
            IS_DATESTRING(ARRAY(E2ePerson("name", 10).aql)),
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
