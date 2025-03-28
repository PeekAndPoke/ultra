package de.peekandpoke.ultra.common.fixtures

import de.peekandpoke.ultra.common.fixture.LoremIpsum
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe

class LoremIpsumSpec : StringSpec() {
    init {
        "invoke operator should return the exact number of words requested" {
            val result = LoremIpsum(5)
            result.split(" ") shouldHaveSize 5
            result shouldBe "Lorem ipsum dolor sit amet,"
        }

        "words(n) should return the exact number of words requested" {
            val result = LoremIpsum.words(10)
            result.split(" ") shouldHaveSize 10
            result shouldBe "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam"
        }

        "words with random range should return close to requested number of words" {
            val result = LoremIpsum.words(10, 3)
            val wordCount = result.split(" ").size
            wordCount shouldBeInRange (7..13)
        }

        "words(n) with large n should not exceed available words" {
            val result = LoremIpsum.words(1000)
            result.split(" ") shouldHaveSize 1000
        }

        "words with negative count should handle gracefully" {
            val result = LoremIpsum.words(-5)
            result shouldBe ""
        }

        "words with zero count should return empty string" {
            val result = LoremIpsum.words(0)
            result shouldBe ""
        }
    }
}
