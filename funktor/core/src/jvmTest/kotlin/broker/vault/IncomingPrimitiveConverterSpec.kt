package de.peekandpoke.ktorfx.core.broker.vault

import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

class IncomingPrimitiveConverterSpec : StringSpec({

    listOf(
        row(Int::class, true),
        row(Float::class, true),
        row(Double::class, true),
        row(Long::class, true),
        row(Boolean::class, true),
        row(String::class, true),
        row(BigDecimal::class, true),
        row(BigInteger::class, true),

        // negative cases
        row(Object::class, false),
        row(LocalDate::class, false)

    ).forEach { (type, supported) ->

        "Must ${if (supported) "" else " NOT "} support the type '$type'" {

            val subject = IncomingPrimitiveConverter()

            subject.canHandle(type.kType().type) shouldBe supported
        }
    }

    listOf(
        row("0", Int::class, 0),
        row("-1", Int::class, -1),
        row("1", Int::class, 1),

        row("0", Float::class, 0.0f),
        row("-1", Float::class, -1.0f),
        row("1", Float::class, 1.0f),
        row("1.1", Float::class, 1.1f),

        row("0", Double::class, 0.0),
        row("-1", Double::class, -1.0),
        row("1", Double::class, 1.0),
        row("1.1", Double::class, 1.1),

        row("0", Long::class, 0L),
        row("-1", Long::class, -1L),
        row("1", Long::class, 1L),

        row("0", Boolean::class, false),
        row("1", Boolean::class, false),
        row("false", Boolean::class, false),
        row("true", Boolean::class, true),

        row("", String::class, ""),
        row("0", String::class, "0"),
        row("some text", String::class, "some text"),

        row("0", BigDecimal::class, BigDecimal(0)),
        row("-1", BigDecimal::class, BigDecimal(-1)),
        row("1", BigDecimal::class, BigDecimal(1)),

        row("0", BigInteger::class, BigInteger.valueOf(0L)),
        row("-1", BigInteger::class, BigInteger.valueOf(-1L)),
        row("1", BigInteger::class, BigInteger.valueOf(1L))

    ).forEach { (input, type, expected) ->

        "Input '$input' must be correctly converted to type '$type'" {

            val subject = IncomingPrimitiveConverter()

            subject.convert(input, type.kType().type) shouldBe expected
        }
    }
})
