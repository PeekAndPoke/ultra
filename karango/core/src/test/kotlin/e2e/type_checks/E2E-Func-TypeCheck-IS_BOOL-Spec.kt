package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.IS_BOOL
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
class `E2E-Func-TypeCheck-IS_BOOL-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "IS_BOOL(true)",
            IS_BOOL(true.aql),
            true
        ),
        tuple(
            "IS_BOOL(false)",
            IS_BOOL(true.aql),
            true
        ),
        tuple(
            "IS_BOOL(null)",
            IS_BOOL(null.aql),
            false
        ),
        tuple(
            "IS_BOOL(0)",
            IS_BOOL(0.aql),
            false
        ),
        tuple(
            "IS_BOOL(1)",
            IS_BOOL(1.aql),
            false
        ),
        tuple(
            "IS_BOOL(\"a\")",
            IS_BOOL("a".aql),
            false
        ),
        tuple(
            "IS_BOOL(\"\")",
            IS_BOOL("".aql),
            false
        ),
        tuple(
            "IS_BOOL([0])",
            IS_BOOL(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_BOOL(object)",
            IS_BOOL(E2ePerson("name", 10).aql),
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
