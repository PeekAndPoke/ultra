package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumbererException
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import kotlin.reflect.full.createType

class DataClassSlumbererSpec : StringSpec({

    "Slumbering a data class with two parameters" {

        data class DataClass(val str: String, val int: Int)

        val codec = Codec.default

        val result = codec.slumber(DataClass("hello", 1))

        result shouldBe mapOf("str" to "hello", "int" to 1)
    }

    "Slumbering a data class with one String parameter" {

        data class DataClass(val str: String)

        val codec = Codec.default

        val result = codec.slumber(DataClass("hello"))

        result shouldBe mapOf("str" to "hello")
    }

    "Slumbering a data class with one String? parameter" {

        data class DataClass(val str: String?)

        val codec = Codec.default

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

        val codec = Codec.default

        val result = codec.slumber(DataClass(Inner("hello")))

        result shouldBe mapOf("inner" to mapOf("str" to "hello"))
    }

    "Slumbering a data class with one Iterable<String> parameter" {

        data class DataClass(val strings: Iterable<String>)

        val codec = Codec.default

        assertSoftly {
            codec.slumber(DataClass(listOf("hello", "you"))) shouldBe mapOf("strings" to listOf("hello", "you"))
        }
    }

    "Slumbering a data class with a Iterable<String> and a Iterable<Any> parameter" {

        data class DataClass(val strings: Iterable<String>, val ints: Iterable<Any>)

        val codec = Codec.default

        assertSoftly {
            codec.slumber(
                DataClass(listOf("hello", "you"), listOf(1, 2, 3))
            ) shouldBe mapOf(
                "strings" to listOf("hello", "you"),
                "ints" to listOf(1, 2, 3)
            )
        }
    }

    "Slumbering a data class with a Map<String, Int> and a Map<Int, Any> parameter" {

        data class DataClass(val strings: Map<String, Int>, val ints: Map<Int, Any?>)

        val codec = Codec.default

        assertSoftly {
            codec.slumber(
                DataClass(mapOf("hello" to 1, "you" to 2), mapOf(1 to null, 2 to "a", 3 to 10))
            ) shouldBe mapOf(
                "strings" to mapOf("hello" to 1, "you" to 2),
                "ints" to mapOf(1 to null, 2 to "a", 3 to 10)
            )
        }
    }

    "Slumbering 'null' as a data class must throw" {

        data class DataClass(val int: Int)

        val codec = Codec.default

        assertSoftly {
            shouldThrow<SlumbererException> {
                codec.slumber(DataClass::class, null)
            }
        }
    }

    "Slumbering 'null' as a nullable data class must work" {

        data class DataClass(val int: Int)

        val codec = Codec.default

        val type = DataClass::class.createType(nullable = true)

        assertSoftly {
            codec.slumber(type, null) shouldBe null
        }
    }
})
