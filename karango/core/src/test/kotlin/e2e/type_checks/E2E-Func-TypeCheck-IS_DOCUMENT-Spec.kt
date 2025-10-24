package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.IS_DOCUMENT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.E2ePerson
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-TypeCheck-IS_DOCUMENT-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "IS_DOCUMENT(object) - empty objects seem to be documents",
            IS_DOCUMENT(mapOf<Any, Any>().aql),
            true
        ),
        tuple(
            "IS_DOCUMENT(object) - objects seem to be documents",
            IS_DOCUMENT(E2ePerson("name", 10).aql),
            true
        ),
        tuple(
            "IS_DOCUMENT(true)",
            IS_DOCUMENT(true.aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(false)",
            IS_DOCUMENT(true.aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(null)",
            IS_DOCUMENT(null.aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(0)",
            IS_DOCUMENT(0.aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(1)",
            IS_DOCUMENT(1.aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(\"a\")",
            IS_DOCUMENT("a".aql),
            false
        ),
        tuple(
            "IS_DOCUMENT(\"\")",
            IS_DOCUMENT("".aql),
            false
        ),
        tuple(
            "IS_DOCUMENT([0]) - ARRAY",
            IS_DOCUMENT(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_DOCUMENT([0]) - listOf",
            IS_DOCUMENT(listOf(0).aql),
            false
        ),
        tuple(
            "IS_DOCUMENT([object])",
            IS_DOCUMENT(ARRAY(E2ePerson("name", 10).aql)),
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
