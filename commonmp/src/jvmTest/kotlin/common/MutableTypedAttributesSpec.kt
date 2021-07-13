package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.min

class MutableTypedAttributesSpec : StringSpec({

    "Getting an existing attribute must work" {

        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")
        val key3 = TypedKey<ZonedDateTime>("XXX")

        val subject = MutableTypedAttributes {
            add(key1, value1)
        }

        subject[key2] = value2

        assertSoftly {
            subject.size shouldBe 2

            subject[key1] shouldBe value1
            subject[key2] shouldBe value2
            subject[key3] shouldBe null
        }
    }

    "Checking if a key is present must work" {

        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")
        val key3 = TypedKey<Any?>("Nullable")

        val subject = MutableTypedAttributes {
            add(key1, value1)
        }

        subject[key2] = value2

        assertSoftly {
            subject.size shouldBe 2

            subject.has(key1) shouldBe true
            subject.has(key2) shouldBe true
            subject.has(key3) shouldBe false

            subject.remove(key1)

            subject.size shouldBe 1

            subject.has(key1) shouldBe false
            subject.has(key2) shouldBe true
            subject.has(key3) shouldBe false

            subject[key3] = null

            subject.size shouldBe 2

            subject.has(key1) shouldBe false
            subject.has(key2) shouldBe true
            subject.has(key3) shouldBe true
        }
    }

    "Setting a value with condition must work" {

        val key1: TypedKey<Int> = TypedKey("number")

        val subject: MutableTypedAttributes = MutableTypedAttributes.empty()

        assertSoftly {
            subject.size shouldBe 0

            val max = 10

            repeat(20) { counter ->

                val expectedValue = min(counter, max)
                val expectedReturn = expectedValue == counter

                subject.setWhen(key1, counter) { (it ?: 0) < max } shouldBe expectedReturn
                subject[key1] shouldBe expectedValue
                subject.size shouldBe 1
            }
        }
    }

    "Removing entries must work" {

        val value1 = LocalDateTime.now()
        val value2 = ZonedDateTime.now()

        val key1 = TypedKey<LocalDateTime>("local")
        val key2 = TypedKey<ZonedDateTime>("zoned")
        val key3 = TypedKey<ZonedDateTime>("XXX")

        val subject = MutableTypedAttributes {
            add(key1, value1)
            add(key2, value2)
        }

        assertSoftly {
            subject.size shouldBe 2

            subject.remove(key2)
            subject.remove(key3)

            subject.size shouldBe 1
            subject[key1] shouldBe value1
            subject[key2] shouldBe null
            subject[key3] shouldBe null
        }
    }

    "clone() must create a new instance with shallow cloned entries" {

        val key1: TypedKey<Int> = TypedKey("number")

        val original = MutableTypedAttributes {
            add(key1, 1)
        }

        val cloned = original.clone()

        assertSoftly {
            original[key1] shouldBe 1
            cloned[key1] shouldBe 1

            original[key1] = 2
            original[key1] shouldBe 2
            cloned[key1] shouldBe 1

            cloned[key1] = 3
            original[key1] shouldBe 2
            cloned[key1] shouldBe 3
        }
    }
})
