package de.peekandpoke.karango.e2e.functions.def

import de.peekandpoke.karango.aql.FIND_FIRST
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
open class E2E_Func_FIND_FIRST_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "FIND_FIRST empty needle in empty haystack",
            FIND_FIRST("".aql, "".aql),
            0L
        ),
        tuple(
            "FIND_FIRST empty needle in haystack",
            FIND_FIRST("abc".aql, "".aql),
            0L
        ),
        tuple(
            "FIND_FIRST not finding (no start no end)",
            FIND_FIRST("abc".aql, "x".aql),
            -1L
        ),
        tuple(
            "FIND_FIRST finding (no start no end)",
            FIND_FIRST("abc abc abc".aql, "abc".aql),
            0L
        ),
        tuple(
            "FIND_FIRST finding with start (no end)",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql),
            4L
        ),
        tuple(
            "FIND_FIRST finding with start and end",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql, 6.aql),
            4L
        ),
        tuple(
            "FIND_FIRST not finding with start and end",
            FIND_FIRST("abc abc abc".aql, "abc".aql, 1.aql, 2.aql),
            -1L
        ),
        tuple(
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
