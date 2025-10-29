package de.peekandpoke.karango.e2e.functions.stu

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.SORTED_UNIQUE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_SORTED_UNIQUE_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "SORTED_UNIQUE ( [] )",
            SORTED_UNIQUE(ARRAY()),
            listOf()
        ),
        tuple(
            "SORTED_UNIQUE ( [ 8, 4, 2, 10, 6, 2, 8, 6, 4 ] )",
            SORTED_UNIQUE(listOf(8, 4, 2, 10, 6, 2, 8, 6, 4).aql),
            listOf(2, 4, 6, 8, 10)
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
