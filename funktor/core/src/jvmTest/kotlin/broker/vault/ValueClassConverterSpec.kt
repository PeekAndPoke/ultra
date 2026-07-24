package io.peekandpoke.funktor.core.broker.vault

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.kType

/**
 * The generic `@JvmInline value class` param converters (inbound + outbound). Together they let a
 * value-class route param (e.g. `RealmParam(val realm: RealmId)`) bind from / render to a URL segment.
 */
class ValueClassConverterSpec : StringSpec({

    val incoming = IncomingValueClassConverter(IncomingPrimitiveConverter())
    val outgoing = OutgoingValueClassConverter()

    "canHandle is true for a user value class, false for scalars, data classes, and kotlin stdlib value classes" {
        incoming.canHandle(kType<VcString>().type) shouldBe true
        incoming.canHandle(kType<VcInt>().type) shouldBe true
        incoming.canHandle(kType<String>().type) shouldBe false
        incoming.canHandle(kType<Int>().type) shouldBe false
        incoming.canHandle(kType<PlainData>().type) shouldBe false
        // kotlin stdlib value classes are excluded (they need dedicated codecs)
        incoming.canHandle(kType<UInt>().type) shouldBe false

        outgoing.canHandle(kType<VcString>().type) shouldBe true
        outgoing.canHandle(kType<String>().type) shouldBe false
        outgoing.canHandle(kType<UInt>().type) shouldBe false
    }

    "inbound converts a raw string to a String-backed value class" {
        incoming.convert("b2b", kType<VcString>().type) shouldBe VcString("b2b")
    }

    "inbound converts a raw string to an Int-backed value class via the primitive converter" {
        incoming.convert("42", kType<VcInt>().type) shouldBe VcInt(42)
    }

    "inbound runs the value class init invariant, rejecting invalid input at the boundary" {
        // This is why a blank realm URL segment becomes a 404 rather than an empty RealmId flowing on.
        shouldThrow<IllegalArgumentException> {
            incoming.convert("", kType<VcNonBlank>().type)
        }
    }

    "outbound unwraps a String-backed value class to its scalar" {
        outgoing.convert(VcString("b2b"), kType<VcString>().type) shouldBe "b2b"
    }

    "outbound unwraps an Int-backed value class to its scalar string" {
        outgoing.convert(VcInt(42), kType<VcInt>().type) shouldBe "42"
    }
})

@JvmInline
value class VcString(val value: String)

@JvmInline
value class VcInt(val value: Int)

@JvmInline
value class VcNonBlank(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

data class PlainData(val x: String)
