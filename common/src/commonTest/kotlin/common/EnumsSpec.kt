package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class EnumsSpec : FreeSpec() {

    enum class TestEnum {
        ValueA,
        ValueB,
        ValueC,
    }

    init {

        "safeEnumOf() must work for existing enum values" {
            safeEnumOf("ValueA", TestEnum.ValueC) shouldBe TestEnum.ValueA
            safeEnumOf("ValueB", TestEnum.ValueC) shouldBe TestEnum.ValueB
            safeEnumOf("ValueC", TestEnum.ValueC) shouldBe TestEnum.ValueC
        }

        "safeEnumOf() must fall back on unknown values" {
            safeEnumOf(null, TestEnum.ValueC) shouldBe TestEnum.ValueC
            safeEnumOf("", TestEnum.ValueC) shouldBe TestEnum.ValueC
            safeEnumOf("X", TestEnum.ValueC) shouldBe TestEnum.ValueC
        }

        "safeEnumOrNull() must work for existing enum values" {
            safeEnumOrNull<TestEnum>("ValueA") shouldBe TestEnum.ValueA
            safeEnumOrNull<TestEnum>("ValueB") shouldBe TestEnum.ValueB
            safeEnumOrNull<TestEnum>("ValueC") shouldBe TestEnum.ValueC
        }

        "safeEnumOrNull() must fall back to null unknown values" {
            safeEnumOrNull<TestEnum>(null) shouldBe null
            safeEnumOrNull<TestEnum>("") shouldBe null
            safeEnumOrNull<TestEnum>("X") shouldBe null
        }

        "safeEnumValuesOf() must work" - {

            val result = safeEnumsOf<TestEnum>(listOf(TestEnum.ValueA.name, TestEnum.ValueB.name).joinToString(" , "))
            result shouldBe listOf(TestEnum.ValueA, TestEnum.ValueB)
        }
    }
}
