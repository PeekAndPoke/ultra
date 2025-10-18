package de.peekandpoke.funktor.core.broker.vault

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class IncomingPrimitiveConverterSpec : StringSpec({

    listOf(
        tuple(Int::class, true),
        tuple(Float::class, true),
        tuple(Double::class, true),
        tuple(Long::class, true),
        tuple(Boolean::class, true),
        tuple(String::class, true),
        tuple(BigDecimal::class, true),
        tuple(BigInteger::class, true),

        // negative cases
        tuple(Object::class, false),
        tuple(LocalDate::class, false)

    ).forEach { (type, supported) ->

        "Must ${if (supported) "" else " NOT "} support the type '$type'" {

            val subject = IncomingPrimitiveConverter()

            subject.canHandle(type.kType().type) shouldBe supported
        }
    }

    listOf(
        tuple("0", Int::class, 0),
        tuple("-1", Int::class, -1),
        tuple("1", Int::class, 1),

        tuple("0", Float::class, 0.0f),
        tuple("-1", Float::class, -1.0f),
        tuple("1", Float::class, 1.0f),
        tuple("1.1", Float::class, 1.1f),

        tuple("0", Double::class, 0.0),
        tuple("-1", Double::class, -1.0),
        tuple("1", Double::class, 1.0),
        tuple("1.1", Double::class, 1.1),

        tuple("0", Long::class, 0L),
        tuple("-1", Long::class, -1L),
        tuple("1", Long::class, 1L),

        tuple("0", Boolean::class, false),
        tuple("1", Boolean::class, false),
        tuple("false", Boolean::class, false),
        tuple("true", Boolean::class, true),

        tuple("", String::class, ""),
        tuple("0", String::class, "0"),
        tuple("some text", String::class, "some text"),

        tuple("0", BigDecimal::class, BigDecimal(0)),
        tuple("-1", BigDecimal::class, BigDecimal(-1)),
        tuple("1", BigDecimal::class, BigDecimal(1)),

        tuple("0", BigInteger::class, BigInteger.valueOf(0L)),
        tuple("-1", BigInteger::class, BigInteger.valueOf(-1L)),
        tuple("1", BigInteger::class, BigInteger.valueOf(1L))

    ).forEach { (input, type, expected) ->

        "Input '$input' must be correctly converted to type '$type'" {

            val subject = IncomingPrimitiveConverter()

            subject.convert(input, type.kType().type) shouldBe expected
        }
    }
})
