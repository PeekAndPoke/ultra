package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DataClassRoundTripSpec : StringSpec({

    "Data class round trip - one" {

        data class DataClass(val str: String, val int: Int)

        val codec = Codec.default
        val source = DataClass("hello", 1)
        val slumbered = codec.slumber(source)
        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf("str" to "hello", "int" to 1)
    }

    "Data class round trip - two" {

        data class DataClass(val strings: MutableList<String>)

        val codec = Codec.default
        val source = DataClass(mutableListOf("hello", "you"))
        val slumbered = codec.slumber(source)
        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf("strings" to listOf("hello", "you"))
    }

    "Data class round trip - three - generic class" {

        data class DataClass<T>(val generic: T)

        val codec = Codec.default
        val source = DataClass("hello")
        val slumbered = codec.slumber(source)
        val result = codec.awake<DataClass<String>>(slumbered)

        result shouldBe source

        slumbered shouldBe mapOf("generic" to "hello")
    }
})
