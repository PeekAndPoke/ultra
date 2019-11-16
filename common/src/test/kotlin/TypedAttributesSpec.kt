package de.peekandpoke.ultra.common

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TypedAttributesSpec : StringSpec({

    "Getting the name of a TypedKey" {

        val key = TypedKey<LocalDateTime>("data")

        assertSoftly {
            key.name shouldBe "data"
            key.toString() shouldBe "data"
        }
    }

    "Two TypedKeys with the same name must not be equal" {

        TypedKey<LocalDateTime>("data") shouldNotBe TypedKey<LocalDateTime>("data")
    }

    "Getting an existing attribute must work" {

        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")

        val subject = TypedAttributes {
            add(key1, value1)
            add(key2, value2)
        }

        assertSoftly {
            subject.size shouldBe 2

            subject[key1] shouldBe value1
            subject[key2] shouldBe value2
        }
    }
})
