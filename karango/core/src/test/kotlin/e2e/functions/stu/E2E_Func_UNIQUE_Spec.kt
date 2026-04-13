package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.peekandpoke.karango.aql.ARRAY
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.UNIQUE
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple
import io.peekandpoke.ultra.vault.first

@Suppress("ClassName")
class E2E_Func_UNIQUE_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "UNIQUE ( [] )",
            UNIQUE(ARRAY()),
            listOf()
        ),
        tuple(
            "UNIQUE ( [ 8, 8, 4, 4, 2, 2, 10, 6 ] )",
            UNIQUE(listOf(8, 8, 4, 4, 2, 2, 10, 6).aql),
            listOf(2, 4, 6, 8, 10)
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            val result = karangoDriver.query {
                RETURN(expression)
            }

            withDetailedClue(expression, expected) {
                result.first() shouldContainExactlyInAnyOrder expected
            }
        }

        "$description - return from LET" {

            val result = karangoDriver.query {
                val l = LET("l", expression)

                RETURN(l)
            }

            withDetailedClue(expression, expected) {
                result.first() shouldContainExactlyInAnyOrder expected
            }
        }
    }
})
