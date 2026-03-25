package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NonNullSlumbererSpec : StringSpec({

    val codec = Codec.default

    "Slumbering a non-null String works" {
        val result = codec.slumber(kType<String>().type, "hello")

        result shouldBe "hello"
    }

    "Slumbering a non-null Int works" {
        val result = codec.slumber(kType<Int>().type, 42)

        result shouldBe 42
    }

    "Slumbering a non-null data class works" {
        data class DataClass(val name: String)

        val result = codec.slumber(kType<DataClass>().type, DataClass("Alice"))

        result shouldBe mapOf("name" to "Alice")
    }

    "Slumbering null as a non-nullable type throws SlumbererException" {
        shouldThrow<SlumbererException> {
            codec.slumber(kType<String>().type, null)
        }.message shouldContain "must not be null"
    }

    "Slumbering null as a nullable type returns null" {
        val result = codec.slumber(kType<String?>().type, null)

        result shouldBe null
    }
})
