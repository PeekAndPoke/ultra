package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TypedAttributesSpec : StringSpec({

    "Getting an existing attribute must work" {

        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")
        val key3 = TypedKey<ZonedDateTime>("XXX")

        val subject = TypedAttributes {
            add(key1, value1)
            add(key2, value2)
        }

        assertSoftly {
            subject.size shouldBe 2

            subject[key1] shouldBe value1
            subject[key2] shouldBe value2
            subject[key3] shouldBe null
        }
    }

    "asMutable() must work properly" {
        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")

        val subject = TypedAttributes {
            add(key1, value1)
            add(key2, value2)
        }

        val mutable = subject.asMutable()

        subject.size shouldBe mutable.size
        subject.entries shouldBe mutable.entries
    }
})
