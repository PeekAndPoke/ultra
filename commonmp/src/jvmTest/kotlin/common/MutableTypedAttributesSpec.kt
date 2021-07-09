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

    "Setting a value with condition must work" {

        val key1 = TypedKey<Int>("number")

        val subject = MutableTypedAttributes.empty

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
})
