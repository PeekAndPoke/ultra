package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LEVENSHTEIN_DISTANCE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-LEVENSHTEIN_DISTANCE-Spec` : StringSpec({

    val cases = listOf(
        row(
            "LEVENSHTEIN_DISTANCE of 'foobar' and 'foo'",
            LEVENSHTEIN_DISTANCE("foobar".aql, "foo".aql),
            3L
        ),
        row(
            "LEVENSHTEIN_DISTANCE of ' ' and ''",
            LEVENSHTEIN_DISTANCE(" ".aql, "".aql),
            1L
        ),
        row(
            "LEVENSHTEIN_DISTANCE of 'The quick brown fox jumps over the lazy dog' and 'The quick black dog jumps over the brown fox'",
            LEVENSHTEIN_DISTANCE(
                "The quick brown fox jumps over the lazy dog".aql,
                "The quick black dog jumps over the brown fox".aql
            ),
            13L
        ),
        row(
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
