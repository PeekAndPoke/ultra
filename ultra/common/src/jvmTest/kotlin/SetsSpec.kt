package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SetsSpec : FreeSpec() {

    init {
        "toggle" - {
            listOf(
                tuple(setOf<String>(), "a", setOf("a")),
                tuple(setOf("a"), "a", setOf()),
                tuple(setOf("a"), "b", setOf("a", "b")),
                tuple(setOf(1, 2), 2, setOf(1)),
            ).forEach { (subject, input, expected) ->

                "$subject.toggle($input) should be $expected" {
                    subject.toggle(input) shouldBe expected
                }
            }
        }
    }
}
