package de.peekandpoke.karango.e2e.functions_array

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.UNIQUE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

@Suppress("ClassName")
class `E2E-Func-Array-UNIQUE-Spec` : StringSpec({

    val cases = listOf(
        row(
            "UNIQUE ( [] )",
            UNIQUE(ARRAY()),
            listOf()
        ),
        row(
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
