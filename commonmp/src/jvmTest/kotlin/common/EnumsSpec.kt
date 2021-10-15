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

        "safeEnumValueOf()" - {
            "safeEnumValueOf() must work for existing enum values" {
                safeEnumValueOf("ValueA", TestEnum.ValueC) shouldBe TestEnum.ValueA
                safeEnumValueOf("ValueB", TestEnum.ValueC) shouldBe TestEnum.ValueB
                safeEnumValueOf("ValueC", TestEnum.ValueC) shouldBe TestEnum.ValueC
            }

            "safeEnumValueOf() must fall back on unknown values" {
                safeEnumValueOf(null, TestEnum.ValueC) shouldBe TestEnum.ValueC
                safeEnumValueOf("", TestEnum.ValueC) shouldBe TestEnum.ValueC
                safeEnumValueOf("X", TestEnum.ValueC) shouldBe TestEnum.ValueC
            }
        }

        "safeEnumValueOrNull()" - {
            "safeEnumValueOrNull() must work for existing enum values" {
                safeEnumValueOrNull<TestEnum>("ValueA") shouldBe TestEnum.ValueA
                safeEnumValueOrNull<TestEnum>("ValueB") shouldBe TestEnum.ValueB
                safeEnumValueOrNull<TestEnum>("ValueC") shouldBe TestEnum.ValueC
            }

            "safeEnumValueOrNull() must fall back to null unknown values" {
                safeEnumValueOrNull<TestEnum>(null) shouldBe null
                safeEnumValueOrNull<TestEnum>("") shouldBe null
                safeEnumValueOrNull<TestEnum>("X") shouldBe null
            }
        }
    }
}
