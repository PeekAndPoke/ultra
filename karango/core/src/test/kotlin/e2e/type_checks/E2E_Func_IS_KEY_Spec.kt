package de.peekandpoke.karango.e2e.type_checks

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.IS_KEY
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
class E2E_Func_IS_KEY_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "IS_KEY(\"a\")",
            IS_KEY("a".aql),
            true
        ),
        // //////////////////////////////////////////////////////////////////////////////////////////////
        tuple(
            "IS_KEY(true)",
            IS_KEY(true.aql),
            false
        ),
        tuple(
            "IS_KEY(false)",
            IS_KEY(true.aql),
            false
        ),
        tuple(
            "IS_KEY(null)",
            IS_KEY(null.aql),
            false
        ),
        tuple(
            "IS_KEY(0)",
            IS_KEY(0.aql),
            false
        ),
        tuple(
            "IS_KEY(1)",
            IS_KEY(1.aql),
            false
        ),
        tuple(
            "IS_KEY(\"\")",
            IS_KEY("".aql),
            false
        ),
        tuple(
            "IS_KEY([0])",
            IS_KEY(ARRAY(0.aql)),
            false
        ),
        tuple(
            "IS_KEY(object)",
            IS_KEY(E2ePerson("name", 10).aql),
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
