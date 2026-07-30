package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UtilsSpec : StringSpec({

    "modifyIf applies the modifier when the condition is true" {
        "abc".modifyIf(true) { uppercase() } shouldBe "ABC"
    }

    "modifyIf returns the receiver untouched when the condition is false" {
        "abc".modifyIf(false) { uppercase() } shouldBe "abc"
    }

    "modifyIf does not evaluate the modifier when the condition is false" {
        var called = 0

        "abc".modifyIf(false) { called++; uppercase() }

        called shouldBe 0
    }

    "modifyIf chains" {
        val result = 1
            .modifyIf(true) { this + 1 }
            .modifyIf(false) { this * 100 }
            .modifyIf(true) { this * 10 }

        result shouldBe 20
    }

    "modifyIf works on a nullable receiver" {
        val value: String? = null

        value.modifyIf(true) { this ?: "fallback" } shouldBe "fallback"
    }
})
