package io.peekandpoke.ultra.slumber.builtin.objects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

object SingletonA

object SingletonB {
    const val VALUE = 42
}

class ObjectInstanceCodecSpec : StringSpec({

    val codec = Codec.default

    "Awake returns the singleton instance" {
        val result = codec.awake<SingletonA>(emptyMap<String, Any>())

        result shouldBeSameInstanceAs SingletonA
    }

    "Awake returns the singleton from arbitrary data" {
        val result = codec.awake<SingletonA>(mapOf("ignored" to "data"))

        result shouldBeSameInstanceAs SingletonA
    }

    "Slumber produces empty map" {
        val result = codec.slumber(SingletonA)

        result shouldBe emptyMap<String, Any>()
    }

    "Roundtrip preserves identity" {
        val slumbered = codec.slumber(SingletonA)
        val awoken = codec.awake<SingletonA>(slumbered)

        awoken shouldBeSameInstanceAs SingletonA
    }

    "Works with objects that have properties" {
        val slumbered = codec.slumber(SingletonB)
        val awoken = codec.awake<SingletonB>(slumbered)

        awoken shouldBeSameInstanceAs SingletonB
    }
})
