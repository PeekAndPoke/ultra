package de.peekandpoke.karango.e2e.functions.pqr

import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.REGEX_MATCHES
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.karangoDriver
import de.peekandpoke.karango.e2e.withDetailedClue
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class E2E_Func_REGEX_MATCHES_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "REGEX_MATCHES(\"My-us3r_n4m3\", \"^[a-z0-9_-]{3,16}\$\", true)",
            REGEX_MATCHES("My-us3r_n4m3".aql, "^[a-z0-9_-]{3,16}$".aql, true.aql),
            listOf("My-us3r_n4m3")
        ),
        tuple(
            "REGEX_MATCHES(\"#4d82h4\", \"^#?([a-f0-9]{6}|[a-f0-9]{3})\$\", true)",
            REGEX_MATCHES("#4d82h4".aql, "^#?([a-f0-9]{6}|[a-f0-9]{3})$".aql, true.aql),
            null
        ),
        tuple(
            "REGEX_MATCHES(\"john@doe.com\", \"^([a-z0-9_\\.-]+)@([\\da-z-]+)\\.([a-z\\.]{2,6})\$\")",
            REGEX_MATCHES("john@doe.com".aql, "^([a-z0-9_\\.-]+)@([\\da-z-]+)\\.([a-z\\.]{2,6})$".aql),
            listOf("john@doe.com", "john", "doe", "com")
        ),
        tuple(
            "REGEX_MATCHES(\"john@doe.com\", \"^([a-z0-9_\\.-]+)@([\\da-z-]+)\\.([a-z\\.]{2,6})\$\", false)",
            REGEX_MATCHES("john@doe.com".aql, "^([a-z0-9_\\.-]+)@([\\da-z-]+)\\.([a-z\\.]{2,6})$".aql, false.aql),
            listOf("john@doe.com", "john", "doe", "com")
        )
    )

    for ((description, expression, expected) in cases) {

        "$description - direct return" {

            repeat(10) {
                val result = karangoDriver.query {
                    RETURN(expression)
                }

                withDetailedClue(expression, expected) {
                    result.toList() shouldBe listOf(expected)
                }
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
