package de.peekandpoke.karango.e2e.functions.def

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.FIRST
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_FIRST_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "FIRST ([])",
            FIRST(ARRAY<Any>()),
            null
        ),
        tuple(
            "IS_NULL( FIRST ([]) )",
            IS_NULL(FIRST(ARRAY<Any>())),
            true
        ),
        tuple(
            "FIRST  (['a'])",
            FIRST(ARRAY("a".aql)),
            "a"
        ),
        tuple(
            "FIRST  (['a', 'b'])",
            FIRST(ARRAY("a".aql, "b".aql)),
            "a"
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
