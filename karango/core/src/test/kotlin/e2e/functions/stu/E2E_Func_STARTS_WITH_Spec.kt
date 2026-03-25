package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.STARTS_WITH
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue
import io.peekandpoke.ultra.common.tuple

@Suppress("ClassName")
class E2E_Func_STARTS_WITH_Spec : StringSpec({

    val cases = listOf(
        tuple(
            "STARTS_WITH matching prefix",
            STARTS_WITH("hello world".aql(), "hello".aql()),
            true
        ),
        tuple(
            "STARTS_WITH non-matching prefix",
            STARTS_WITH("hello world".aql(), "world".aql()),
            false
        ),
        tuple(
            "STARTS_WITH empty prefix always matches",
            STARTS_WITH("hello".aql(), "".aql()),
            true
        ),
        tuple(
            "STARTS_WITH on empty string with non-empty prefix",
            STARTS_WITH("".aql(), "hello".aql()),
            false
        ),
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

        "$description - via LET" {

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
