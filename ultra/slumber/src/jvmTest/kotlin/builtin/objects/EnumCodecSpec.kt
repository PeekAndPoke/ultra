package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

enum class TestColor { RED, GREEN, BLUE }

class EnumCodecSpec : StringSpec({

    val codec = Codec.default

    "Awake from valid string" {
        codec.awake<TestColor>("RED") shouldBe TestColor.RED
        codec.awake<TestColor>("GREEN") shouldBe TestColor.GREEN
        codec.awake<TestColor>("BLUE") shouldBe TestColor.BLUE
    }

    "Awake from invalid string returns null for nullable type" {
        codec.awake<TestColor?>("INVALID") shouldBe null
    }

    "Awake from non-string returns null for nullable type" {
        codec.awake<TestColor?>(123) shouldBe null
        codec.awake<TestColor?>(mapOf("a" to 1)) shouldBe null
    }

    "Awake from null returns null for nullable type" {
        codec.awake<TestColor?>(null) shouldBe null
    }

    "Enum names are case-sensitive" {
        codec.awake<TestColor?>("red") shouldBe null
        codec.awake<TestColor?>("Red") shouldBe null
    }

    "Slumber produces enum name" {
        codec.slumber(TestColor.RED) shouldBe "RED"
        codec.slumber(TestColor.GREEN) shouldBe "GREEN"
    }

    "Roundtrip" {
        TestColor.entries.forEach { color ->
            codec.awake<TestColor>(codec.slumber(color)) shouldBe color
        }
    }
})
