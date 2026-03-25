package io.peekandpoke.ultra.slumber.builtin.objects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake

class AnyCodecSpec : StringSpec({

    val codec = Codec.default

    "Awake Any? passes through strings" {
        codec.awake<Any?>("hello") shouldBe "hello"
    }

    "Awake Any? passes through numbers" {
        codec.awake<Any?>(42) shouldBe 42
    }

    "Awake Any? passes through maps" {
        val map = mapOf("a" to 1)
        codec.awake<Any?>(map) shouldBe map
    }

    "Awake Any? passes through null" {
        codec.awake<Any?>(null) shouldBe null
    }

    "Awake Any? passes through lists" {
        val list = listOf(1, 2, 3)
        codec.awake<Any?>(list) shouldBe list
    }
})
