package io.peekandpoke.karango.e2e.functions.jkl

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.LEVENSHTEIN_DISTANCE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_LEVENSHTEIN_DISTANCE_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "LEVENSHTEIN_DISTANCE of 'foobar' and 'foo'",
            LEVENSHTEIN_DISTANCE("foobar".aql, "foo".aql),
            3L
        ),
        tuple(
            "LEVENSHTEIN_DISTANCE of ' ' and ''",
            LEVENSHTEIN_DISTANCE(" ".aql, "".aql),
            1L
        ),
        tuple(
            "LEVENSHTEIN_DISTANCE of 'The quick brown fox jumps over the lazy dog' and 'The quick black dog jumps over the brown fox'",
            LEVENSHTEIN_DISTANCE(
                "The quick brown fox jumps over the lazy dog".aql,
                "The quick black dog jumps over the brown fox".aql
            ),
            13L
        ),
        tuple(
            "LEVENSHTEIN_DISTANCE of 'der mötör trötet' and 'der trötet'",
            LEVENSHTEIN_DISTANCE("der mötör trötet".aql, "der trötet".aql),
            6L
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
