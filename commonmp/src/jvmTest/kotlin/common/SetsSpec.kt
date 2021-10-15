package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class SetsSpec : FreeSpec() {

    init {
        "toggle" - {

            listOf(
                row(setOf<String>(), "a", setOf("a")),
                row(setOf("a"), "a", setOf()),
                row(setOf("a"), "b", setOf("a", "b")),
                row(setOf(1, 2), 2, setOf(1)),
            ).forEach { (subject, input, expected) ->

                "${subject}.toggle($input) should be $expected" {
                    subject.toggle(input) shouldBe expected
                }
            }
        }
    }
}
