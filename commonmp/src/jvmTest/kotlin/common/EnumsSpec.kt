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
}
