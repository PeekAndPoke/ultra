package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.reflection.kType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CodecHelpersSpec : StringSpec({

    val codec = Codec.default

    "awake<reified>(data) works for simple types" {
        codec.awake<String>("hello") shouldBe "hello"
        codec.awake<Int>(42) shouldBe 42
        codec.awake<Boolean>(true) shouldBe true
    }

    "awake(KClass, data) works for data classes" {
        data class Person(val name: String, val age: Int)

        val data = mapOf("name" to "Alice", "age" to 30)
        val result = codec.awake(Person::class, data)

        result shouldBe Person("Alice", 30)
    }

    "awake(TypeRef, data) works" {
        data class Person(val name: String)

        val ref = kType<Person>()
        val result = codec.awake(ref, mapOf("name" to "Bob"))

        result shouldBe Person("Bob")
    }

    "slumber(data) infers type from data class" {
        data class Person(val name: String, val age: Int)

        val result = codec.slumber(Person("Alice", 30))

        result shouldBe mapOf("name" to "Alice", "age" to 30)
    }

    "slumber(data) handles null" {
        val result = codec.slumber(null)

        result shouldBe null
    }

    "slumber(data) infers type for data class" {
        data class Person(val name: String)

        val result = codec.slumber(Person("Alice"))

        result shouldBe mapOf("name" to "Alice")
    }

    "getAwaker<reified>() returns cached instance" {
        val a1 = codec.getAwaker<String>()
        val a2 = codec.getAwaker<String>()

        a1 shouldBeSameInstanceAs a2
    }

    "getSlumberer<reified>() returns cached instance" {
        val s1 = codec.getSlumberer<String>()
        val s2 = codec.getSlumberer<String>()

        s1 shouldBeSameInstanceAs s2
    }

    "Round-trip via helpers works" {
        data class Address(val city: String, val zip: String)
        data class Person(val name: String, val address: Address)

        val original = Person("Alice", Address("Berlin", "10115"))
        val slumbered = codec.slumber(original)
        val restored = codec.awake<Person>(slumbered)

        restored shouldBe original
    }
})
