package io.peekandpoke.ultra.semanticui

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SemanticNumberSpec : StringSpec({

    "entries count is 16" {
        SemanticNumber.entries.size shouldBe 16
    }

    "All entries have unique names" {
        val names = SemanticNumber.entries.map { it.name }
        names.distinct().size shouldBe names.size
    }

    "of() maps integers 1..16 to the correct enum entries" {
        val expected = listOf(
            1 to SemanticNumber.one,
            2 to SemanticNumber.two,
            3 to SemanticNumber.three,
            4 to SemanticNumber.four,
            5 to SemanticNumber.five,
            6 to SemanticNumber.six,
            7 to SemanticNumber.seven,
            8 to SemanticNumber.eight,
            9 to SemanticNumber.nine,
            10 to SemanticNumber.ten,
            11 to SemanticNumber.eleven,
            12 to SemanticNumber.twelve,
            13 to SemanticNumber.thirteen,
            14 to SemanticNumber.fourteen,
            15 to SemanticNumber.fifteen,
            16 to SemanticNumber.sixteen,
        )

        expected.forEach { (num, entry) ->
            withClue("of($num) should be ${entry.name}") {
                SemanticNumber.of(num) shouldBe entry
            }
        }
    }

    "of() clamps values <= 0 to one" {
        SemanticNumber.of(0) shouldBe SemanticNumber.one
        SemanticNumber.of(-1) shouldBe SemanticNumber.one
        SemanticNumber.of(-100) shouldBe SemanticNumber.one
    }

    "of() clamps values >= 17 to sixteen" {
        SemanticNumber.of(17) shouldBe SemanticNumber.sixteen
        SemanticNumber.of(100) shouldBe SemanticNumber.sixteen
        SemanticNumber.of(999) shouldBe SemanticNumber.sixteen
    }

    "entries are ordered from one to sixteen" {
        val expectedOrder = listOf(
            "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        )

        SemanticNumber.entries.map { it.name } shouldBe expectedOrder
    }
})
