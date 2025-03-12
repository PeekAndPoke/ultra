package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.FIND_FIRST
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
open class `E2E-Func-String-FIND_FIRST-Spec` : StringSpec({

    val cases = listOf(
        row(
            "FIND_FIRST empty needle in empty haystack",
            FIND_FIRST("".aql, "".aql),
            0L
        ),
        row(
            "FIND_FIRST empty needle in haystack",
            FIND_FIRST("abc".aql, "".aql),
            0L
        ),
        row(
            "FIND_FIRST not finding (no start no end)",
            FIND_FIRST("abc".aql, "x".aql),
            -1L
        ),
        row(
            "FIND_FIRST finding (no start no end)",
            FIND_FIRST("abc abc abc".aql, "abc".aql),
            0L
        ),
        row(
            "FIND_FIRST finding with start (no end)",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql),
            4L
        ),
        row(
            "FIND_FIRST finding with start and end",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql, 6.aql),
            4L
        ),
        row(
            "FIND_FIRST not finding with start and end",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql, 2.aql),
            -1L
        ),
        row(
            "FIND_FIRST not finding with start greater end",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 2.aql, 1.aql),
            -1L
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
