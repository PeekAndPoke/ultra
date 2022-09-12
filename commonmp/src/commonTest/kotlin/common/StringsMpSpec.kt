package de.peekandpoke.ultra.common

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StringsMpSpec : StringSpec({

    val encodingPairs = listOf(
        "" to "",
        "abc" to "abc",
        "a c" to "a%20c",
        "a_&_c" to "a_%26_c",
        "a/c" to "a%2Fc",

        )

    "encodeUriComponent" {
        encodingPairs.forEach { (left, right) ->
            withClue("$left encoded should be $right") {
                left.encodeUriComponent() shouldBe right
            }
        }
    }

    "decodeUriComponent" {
        encodingPairs.forEach { (left, right) ->
            withClue("$right decoded should be $left") {
                right.decodeUriComponent() shouldBe left
            }
        }
    }
})
