package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NoArgCtorRoundTripSpec : StringSpec() {

    private data object Empty

    init {
        "No arg ctor class round trip - one" {
            class DataClass

            val codec = Codec.default
            val source = DataClass()
            val slumbered = codec.slumber(source)
            val result = codec.awake(DataClass::class, slumbered)!!

            result::class shouldBe DataClass::class

            slumbered shouldBe mapOf<String, Any>()
        }

        "No arg ctor data object round trip" {
            val codec = Codec.default
            val source = Empty
            val slumbered = codec.slumber(source)
            val result = codec.awake(Empty::class, slumbered)!!

            result::class shouldBe Empty::class

            slumbered shouldBe mapOf<String, Any>()
        }
    }
}
