package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RemoveResultSpec : StringSpec({

    "RemoveResult.empty has count 0 and null query" {
        val result = RemoveResult.empty

        result.count shouldBe 0
        result.query shouldBe null
    }

    "RemoveResult with custom values retains them" {
        val result = RemoveResult(count = 42, query = null)

        result.count shouldBe 42
        result.query shouldBe null
    }

    "RemoveResult data class equality works correctly" {
        val a = RemoveResult(count = 5, query = null)
        val b = RemoveResult(count = 5, query = null)
        val c = RemoveResult(count = 10, query = null)

        (a == b) shouldBe true
        (a == c) shouldBe false
    }
})
