package io.peekandpoke.ultra.semanticui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SemanticColorSpec : StringSpec({

    "none.isSet is false" {
        SemanticColor.none.isSet shouldBe false
    }

    "default.isSet is false" {
        SemanticColor.default.isSet shouldBe false
    }

    "isSet is true for actual colors" {
        val actualColors = listOf(
            SemanticColor.white,
            SemanticColor.red,
            SemanticColor.orange,
            SemanticColor.yellow,
            SemanticColor.olive,
            SemanticColor.green,
            SemanticColor.teal,
            SemanticColor.blue,
            SemanticColor.violet,
            SemanticColor.purple,
            SemanticColor.pink,
            SemanticColor.brown,
            SemanticColor.grey,
            SemanticColor.black,
        )

        actualColors.forEach { color ->
            color.isSet shouldBe true
        }
    }

    "none or red returns red" {
        (SemanticColor.none or SemanticColor.red) shouldBe SemanticColor.red
    }

    "red or blue returns red (first non-none wins)" {
        (SemanticColor.red or SemanticColor.blue) shouldBe SemanticColor.red
    }

    "none or none returns none" {
        (SemanticColor.none or SemanticColor.none) shouldBe SemanticColor.none
    }

    "All entries have unique names" {
        val names = SemanticColor.entries.map { it.name }
        names.distinct().size shouldBe names.size
    }

    "entries count is 16" {
        SemanticColor.entries.size shouldBe 16
    }
})
