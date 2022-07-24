package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.slumberTestClasses.DataClassWithPrivateCtor
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class DataClassAwakerSpec : StringSpec({

    "Awaking a data class with a private primary constructor" {

        val codec = Codec.default

        val result = codec.awake(DataClassWithPrivateCtor::class, mapOf("str" to "hello"))

        result shouldBe DataClassWithPrivateCtor.of("hello")
    }

    "Awaking a data class with a private primary constructor from typeref" {

        val codec = Codec.default

        val result = codec.awake(kType<DataClassWithPrivateCtor>(), mapOf("str" to "hello"))

        result shouldBe DataClassWithPrivateCtor.of("hello")
    }

    "Awaking a data class with two parameters" {

        data class DataClass(val str: String, val int: Int)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("str" to "hello", "int" to 1))

        result shouldBe DataClass("hello", 1)
    }

    "Awaking a nullable data class with two parameters" {

        data class DataClass(val str: String, val int: Int)

        val codec = Codec.default

        val type = DataClass::class.createType(nullable = true)

        val result = codec.awake(type, mapOf("str" to "hello", "int" to 1))

        result shouldBe DataClass("hello", 1)
    }

    "Awaking a data class with one String parameter" {

        data class DataClass(val str: String)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("str" to "hello"))

        result shouldBe DataClass("hello")
    }

    "Awaking a data class with one String? parameter" {

        data class DataClass(val str: String?)

        val codec = Codec.default

        withClue("A given value must be set") {
            codec.awake(DataClass::class, mapOf("str" to "hello")) shouldBe DataClass("hello")
        }

        withClue("A given null value must be set") {
            codec.awake(DataClass::class, mapOf("str" to null)) shouldBe DataClass(null)
        }

        withClue("A missing value must be used like null") {
            codec.awake(DataClass::class, emptyMap<String, Any>()) shouldBe DataClass(null)
        }

        withClue("Any data that is not a Map is not acceptable even when all fields are nullable") {
            shouldThrow<AwakerException> {
                codec.awake(DataClass::class, null)
            }

            shouldThrow<AwakerException> {
                codec.awake(DataClass::class, "invalid")
            }
        }
    }

    "Awaking a nullable data class with one String? parameter" {

        data class DataClass(val str: String?)

        val codec = Codec.default

        val type = DataClass::class.createType(nullable = true)

        withClue("A given value must be set") {
            codec.awake(type, mapOf("str" to "hello")) shouldBe DataClass("hello")
        }

        withClue("A given null value must be set") {
            codec.awake(type, mapOf("str" to null)) shouldBe DataClass(null)
        }

        withClue("A missing value must be used like null") {
            codec.awake(type, emptyMap<String, Any>()) shouldBe DataClass(null)
        }

        withClue("Any data that is not a Map is not acceptable even when all fields are nullable") {
            codec.awake(type, null) shouldBe null

            codec.awake(type, "invalid") shouldBe null
        }
    }

    "Awaking a data class with one default parameter with value present" {

        data class DataClass(val str: String = "default")

        val codec = Codec.default

        withClue("A given value must overwrite the default value") {
            codec.awake(DataClass::class, mapOf("str" to "hello")) shouldBe DataClass("hello")
        }

        withClue("When no value is given the default value must be used") {
            codec.awake(DataClass::class, emptyMap<String, Any>()) shouldBe DataClass("default")
        }

        withClue("Null is acceptable for non-nullable parameters when having a default value") {
            codec.awake(DataClass::class, mapOf("str" to "default"))
        }

        withClue("Any data that is not a Map is not acceptable even when all fields are optional") {
            shouldThrow<AwakerException> {
                codec.awake(DataClass::class, null)
            }

            shouldThrow<AwakerException> {
                codec.awake(DataClass::class, "invalid")
            }
        }
    }

    "Awaking a data class with one data class parameter" {

        data class Inner(val str: String)

        data class DataClass(val inner: Inner)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf("inner" to mapOf("str" to "hello")))

        result shouldBe DataClass(Inner("hello"))
    }

    "Awaking a data class with a mandatory field and no data sent" {

        data class DataClass(val optional: String)

        val codec = Codec.default

        shouldThrow<Throwable> {
            codec.awake(DataClass::class, mapOf<String, Any>())
        }
    }

    "Awaking a data class with a mandatory field and valid data sent" {

        data class DataClass(val optional: String)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>("optional" to "data"))

        result shouldBe DataClass(optional = "data")
    }

    "Awaking a data class with a mandatory field and invalid data sent" {

        data class DataClass(val optional: String)

        val codec = Codec.default

        shouldThrow<Throwable> {
            codec.awake(DataClass::class, mapOf<String, Any>("optional" to emptyList<String>()))
        }
    }

    "Awaking a data class with a nullable field and no data sent" {

        data class DataClass(val optional: String?)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>())

        result shouldBe DataClass(optional = null)
    }

    "Awaking a data class with a nullable field and some data sent" {

        data class DataClass(val optional: String?)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>("optional" to "data"))

        result shouldBe DataClass(optional = "data")
    }

    "Awaking a data class with a nullable field and wrong data sent" {

        data class DataClass(val optional: String?)

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>("optional" to emptyList<String>()))

        result shouldBe DataClass(optional = null)
    }

    "Awaking a data class with a optional field and no data sent" {

        data class DataClass(val optional: String = "default")

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>())

        result shouldBe DataClass(optional = "default")
    }

    "Awaking a data class with a optional field and some data sent" {

        data class DataClass(val optional: String = "default")

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>("optional" to "data"))

        result shouldBe DataClass(optional = "data")
    }

    "Awaking a data class with a optional field and wrong data sent" {

        data class DataClass(val optional: String = "default")

        val codec = Codec.default

        val result = codec.awake(DataClass::class, mapOf<String, Any>("optional" to emptyList<String>()))

        result shouldBe DataClass(optional = "default")
    }

    "Awaking a data class with one Iterable<String> parameter" {

        data class DataClass(val strings: Iterable<String>)

        val codec = Codec.default

        codec.awake(DataClass::class, mapOf("strings" to listOf("hello", "you"))) shouldBe
                DataClass(listOf("hello", "you"))

        codec.awake(DataClass::class, mapOf("strings" to arrayOf("hello", "you"))) shouldBe
                DataClass(listOf("hello", "you"))
    }

    "Awaking a data class with one List<String> parameter" {

        data class DataClass(val strings: List<String>)

        val codec = Codec.default

        codec.awake(DataClass::class, mapOf("strings" to listOf("hello", "you"))) shouldBe
                DataClass(listOf("hello", "you"))

        codec.awake(DataClass::class, mapOf("strings" to arrayOf("hello", "you"))) shouldBe
                DataClass(listOf("hello", "you"))
    }

    "Awaking a data class with one List<String> parameter mixing in nulls" {

        data class DataClass(val strings: List<String>)

        val codec = Codec.default

        val exception = shouldThrow<AwakerException> {
            codec.awake(DataClass::class, mapOf("strings" to listOf("hello", null)))
        }

        exception.message shouldContain "root.strings.1"
    }

    "Awaking a data class with one MutableList<String> parameter" {

        data class DataClass(val strings: MutableList<String>)

        val codec = Codec.default

        codec.awake(DataClass::class, mapOf("strings" to listOf("hello", "you"))) shouldBe
                DataClass(mutableListOf("hello", "you"))

        codec.awake(DataClass::class, mapOf("strings" to arrayOf("hello", "you"))) shouldBe
                DataClass(mutableListOf("hello", "you"))
    }

    "Awaking a data class with one Set<String> parameter" {

        data class DataClass(val strings: Set<String>)

        val codec = Codec.default

        codec.awake(DataClass::class, mapOf("strings" to listOf("hello", "you"))) shouldBe
                DataClass(setOf("hello", "you"))

        codec.awake(DataClass::class, mapOf("strings" to arrayOf("hello", "you"))) shouldBe
                DataClass(setOf("hello", "you"))
    }

    "Awaking a data class with one MutableSet<String> parameter" {

        data class DataClass(val strings: MutableSet<String>)

        val codec = Codec.default

        codec.awake(DataClass::class, mapOf("strings" to listOf("hello", "you"))) shouldBe
                DataClass(mutableSetOf("hello", "you"))

        codec.awake(DataClass::class, mapOf("strings" to arrayOf("hello", "you"))) shouldBe
                DataClass(mutableSetOf("hello", "you"))
    }

    "Awaking a data class with one generic parameter" {

        data class DataClass<T>(val generic: T)

        val codec = Codec.default

        val stringType = DataClass::class.createType(
            listOf(KTypeProjection.invariant(String::class.createType()))
        )

        val intType = DataClass::class.createType(
            listOf(KTypeProjection.invariant(Int::class.createType()))
        )

        codec.awake(stringType, mapOf("generic" to "hello")) shouldBe DataClass("hello")

        codec.awake(intType, mapOf("generic" to 100)) shouldBe DataClass(100)
    }

    "Awaking a data class with two generic parameter" {

        data class DataClass<F, S>(val first: F, val second: S)

        val codec = Codec.default

        val stringIntType = DataClass::class.createType(
            listOf(
                KTypeProjection.invariant(String::class.createType()),
                KTypeProjection.invariant(Int::class.createType())
            )
        )

        val intStringType = DataClass::class.createType(
            listOf(
                KTypeProjection.invariant(Int::class.createType()),
                KTypeProjection.invariant(String::class.createType())
            )
        )

        codec.awake(
            stringIntType, mapOf("first" to "hello", "second" to 100)
        ) shouldBe DataClass("hello", 100)

        codec.awake(
            intStringType, mapOf("first" to 100, "second" to "hello")
        ) shouldBe DataClass(100, "hello")
    }

    "Awaking a data class with two generic parameter forwarded to a Map" {

        data class DataClass<F, S>(val map: Map<F, S>)

        val codec = Codec.default

        val stringIntType = DataClass::class.createType(
            listOf(
                KTypeProjection.invariant(String::class.createType()),
                KTypeProjection.invariant(Int::class.createType())
            )
        )

        val intStringType = DataClass::class.createType(
            listOf(
                KTypeProjection.invariant(Int::class.createType()),
                KTypeProjection.invariant(String::class.createType())
            )
        )

        codec.awake(
            stringIntType, mapOf("map" to mapOf("hello" to 1, "you" to 2))
        ) shouldBe DataClass(mapOf("hello" to 1, "you" to 2))

        codec.awake(
            intStringType, mapOf("map" to mapOf(1 to "hello", 2 to "you"))
        ) shouldBe DataClass(mapOf(1 to "hello", 2 to "you"))
    }
})
