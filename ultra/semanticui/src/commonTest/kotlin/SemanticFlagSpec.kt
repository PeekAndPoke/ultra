package io.peekandpoke.ultra.semanticui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SemanticFlagSpec : StringSpec({

    "all contains known countries" {
        val all = SemanticFlag.all

        (all.contains("germany")) shouldBe true
        (all.contains("united states")) shouldBe true
        (all.contains("japan")) shouldBe true
        (all.contains("brazil")) shouldBe true
    }

    "all list is not empty and has expected size" {
        SemanticFlag.all.size shouldBe 244
    }

    "cssClassOf returns space-separated CSS classes" {
        val result = SemanticFlag.cssClassOf { germany }

        result shouldBe "germany"
    }

    "cssClassOfAsList returns list of CSS classes" {
        val result = SemanticFlag.cssClassOfAsList { germany }

        result shouldBe listOf("germany")
    }
})
