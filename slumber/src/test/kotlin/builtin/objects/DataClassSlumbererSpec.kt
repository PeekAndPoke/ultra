package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.Slumber
import de.peekandpoke.ultra.slumber.SlumbererException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.full.createType

class DataClassSlumbererSpec : StringSpec() {

    private interface GenericLowerBound

    init {

        "Slumbering a data class with two parameters" {

            data class DataClass(val str: String, val int: Int)

            val codec = Codec.default

            val result = codec.slumber(DataClass("hello", 1))

            result shouldBe mapOf("str" to "hello", "int" to 1)
        }

        "Slumbering a data class with private parameters" {
            data class DataClass(private val str: String, private val int: Int)

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

            withClue("A value must be converted correctly") {
                codec.slumber(DataClass("hello")) shouldBe mapOf("str" to "hello")
            }

            withClue("A null value must be converted correctly") {
                codec.slumber(DataClass(null)) shouldBe mapOf("str" to null)
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

            codec.slumber(DataClass(listOf("hello", "you"))) shouldBe mapOf("strings" to listOf("hello", "you"))
        }

        "Slumbering a data class with a Iterable<String> and a Iterable<Any> parameter" {

            data class DataClass(val strings: Iterable<String>, val ints: Iterable<Any>)

            val codec = Codec.default

            codec.slumber(
                DataClass(listOf("hello", "you"), listOf(1, 2, 3))
            ) shouldBe mapOf(
                "strings" to listOf("hello", "you"),
                "ints" to listOf(1, 2, 3)
            )
        }

        "Slumbering a data class with a Map<String, Int> and a Map<Int, Any> parameter" {

            data class DataClass(val strings: Map<String, Int>, val ints: Map<Int, Any?>)

            val codec = Codec.default

            codec.slumber(
                DataClass(mapOf("hello" to 1, "you" to 2), mapOf(1 to null, 2 to "a", 3 to 10))
            ) shouldBe mapOf(
                "strings" to mapOf("hello" to 1, "you" to 2),
                "ints" to mapOf(1 to null, 2 to "a", 3 to 10)
            )
        }

        "Slumbering 'null' as a data class must throw" {

            data class DataClass(val int: Int)

            val codec = Codec.default

            shouldThrow<SlumbererException> {
                codec.slumber(DataClass::class, null)
            }
        }

        "Slumbering 'null' as a nullable data class must work" {

            data class DataClass(val int: Int)

            val codec = Codec.default

            val type = DataClass::class.createType(nullable = true)

            codec.slumber(type, null) shouldBe null
        }

        "Slumbering a generic data class must work" {

            data class Inner(val str: String)

            data class DataClass<T>(val inner: T)

            val codec = Codec.default

            val result = codec.slumber(DataClass(Inner("hello")))

            result shouldBe mapOf("inner" to mapOf("str" to "hello"))
        }

        "Slumbering a generic data class with lower bound must work" {

            data class Inner(val str: String) : GenericLowerBound

            data class DataClass<T : GenericLowerBound>(val inner: T)

            val codec = Codec.default

            val result = codec.slumber(DataClass(Inner("hello")))

            result shouldBe mapOf("inner" to mapOf("str" to "hello"))
        }

        "Slumbering a generic data class with lower bound and inner list must work" {

            data class Inner(val str: String) : GenericLowerBound

            data class DataClass<T : GenericLowerBound>(val inner: List<T>)

            val codec = Codec.default

            val result = codec.slumber(DataClass(inner = listOf(Inner("hello"))))

            result shouldBe mapOf(
                "inner" to listOf(
                    mapOf("str" to "hello")
                )
            )
        }

        "Slumbering a generic data class with lower bound and inner map must work" {

            data class Inner(val str: String) : GenericLowerBound

            data class DataClass<T : GenericLowerBound>(val inner: Map<String, T>)

            val codec = Codec.default

            val result = codec.slumber(DataClass(inner = mapOf("1" to Inner("hello"))))

            result shouldBe mapOf(
                "inner" to mapOf(
                    "1" to mapOf(
                        "str" to "hello"
                    )
                )
            )
        }

        "Slumbering a generic data class with an inner generic type must work" {

            data class Inner<T>(val str: T)

            data class DataClass<T>(val inner: T)

            val codec = Codec.default

            val result = codec.slumber(DataClass(Inner("hello")))

            result shouldBe mapOf("inner" to mapOf("str" to "hello"))
        }

        "Slumbering a data class with only null values must work" {
            data class AllNull(val a: Int? = null, val b: Int? = null)

            val codec = Codec.default

            val result = codec.slumber(AllNull())

            result shouldBe mapOf("a" to null, "b" to null)
        }

        "Slumbering a data class with only null values and annotated getter must work" {
            data class AllNull(val a: Int? = null, val b: Int? = null) {
                @Slumber.Field
                val text get() = "text"
            }

            val codec = Codec.default

            val result = codec.slumber(AllNull())

            result shouldBe mapOf("a" to null, "b" to null, "text" to "text")
        }
    }
}
