package io.peekandpoke.ultra.slumber

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.reflection.kType

class NonNullAwakerSpec : StringSpec({

    val codec = Codec.default

    "Awaking a non-nullable String from valid data works" {
        val result = codec.awake(kType<String>().type, "hello")

        result shouldBe "hello"
    }

    "Awaking a non-nullable String from null throws AwakerException" {
        shouldThrow<AwakerException> {
            codec.awake(kType<String>().type, null)
        }.message shouldContain "must not be null"
    }

    "Awaking a nullable String from null returns null" {
        val result = codec.awake(kType<String?>().type, null)

        result shouldBe null
    }

    "Awaking a non-nullable Int from null throws AwakerException" {
        shouldThrow<AwakerException> {
            codec.awake(kType<Int>().type, null)
        }.message shouldContain "must not be null"
    }

    "Awaking a nullable Int from null returns null" {
        val result = codec.awake(kType<Int?>().type, null)

        result shouldBe null
    }

    "Awaking a non-nullable data class field from null throws" {
        data class DataClass(val name: String)

        shouldThrow<AwakerException> {
            codec.awake(kType<DataClass>().type, mapOf("name" to null))
        }.message shouldContain "must not be null"
    }
})
