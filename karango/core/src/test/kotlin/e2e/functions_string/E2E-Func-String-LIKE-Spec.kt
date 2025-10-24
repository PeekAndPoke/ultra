package de.peekandpoke.karango.e2e.functions_string

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LIKE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Func-String-LIKE-Spec` : StringSpec({

    val cases = listOf(
        tuple(
            "LIKE(\"cart\", \"ca_t\")",
            LIKE("cart".aql, "ca_t".aql),
            true
        ),
        tuple(
            "LIKE(\"carrot\", \"ca_t\")",
            LIKE("carrot".aql, "ca_t".aql),
            false
        ),
        tuple(
            "LIKE(\"carrot\", \"ca%t\")",
            LIKE("carrot".aql, "ca%t".aql),
            true
        ),

        tuple(
            "LIKE(\"foo bar baz\", \"bar\")",
            LIKE("foo bar baz".aql, "bar".aql),
            false
        ),
        tuple(
            "LIKE(\"foo bar baz\", \"%bar%\")",
            LIKE("foo bar baz".aql, "%bar%".aql),
            true
        ),
        tuple(
            "LIKE(\"bar\", \"%bar%\")",
            LIKE("bar".aql, "%bar%".aql),
            true
        ),

        tuple(
            "LIKE(\"FoO bAr BaZ\", \"fOo%bAz\")  in caseInsensitive mode",
            LIKE("FoO bAr BaZ".aql, "fOo%bAz".aql),
            false
        ),
        tuple(
            "LIKE(\"FoO bAr BaZ\", \"fOo%bAz\", false)  in caseInsensitive mode",
            LIKE("FoO bAr BaZ".aql, "fOo%bAz".aql, false.aql),
            false
        ),
        tuple(
            "LIKE(\"FoO bAr BaZ\", \"fOo%bAz\", true) in caseInsensitive mode",
            LIKE("FoO bAr BaZ".aql, "fOo%bAz".aql, true.aql),
            true
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
