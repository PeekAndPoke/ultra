package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.Config
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class DataClassSlumbererSpec : StringSpec({

    "Slumbering a data class with two parameters" {

        data class DataClass(val str: String, val int: Int)

        val codec = Codec(Config())

        val result = codec.slumber(DataClass("hello", 1))

        result shouldBe mapOf("str" to "hello", "int" to 1)
    }

    "Slumbering a data class with one String parameter" {

        data class DataClass(val str: String)

        val codec = Codec(Config())

        val result = codec.slumber(DataClass("hello"))

        result shouldBe mapOf("str" to "hello")
    }

    "Slumbering a data class with one String? parameter" {

        data class DataClass(val str: String?)

        val codec = Codec(Config())

        assertSoftly {

            withClue("A value must be converted correctly") {
                codec.slumber(DataClass("hello")) shouldBe mapOf("str" to "hello")
            }

            withClue("A null value must be converted correctly") {
                codec.slumber(DataClass(null)) shouldBe mapOf("str" to null)
            }
        }
    }

    "Slumbering a data class with one data class parameter" {

        data class Inner(val str: String)

        data class DataClass(val inner: Inner)

        val codec = Codec(Config())

        val result = codec.slumber(DataClass(Inner("hello")))

        result shouldBe mapOf("inner" to mapOf("str" to "hello"))
    }

    "Slumbering a data class with one Iterable<String> parameter" {

        data class DataClass(val strings: Iterable<String>)

        val codec = Codec(Config())

        assertSoftly {
            codec.slumber(DataClass(listOf("hello", "you"))) shouldBe mapOf("strings" to listOf("hello", "you"))
        }
    }
})
