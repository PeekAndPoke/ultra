package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
enum class TestEnum1 {
    A, B, C
}

@Suppress("unused")
enum class TestEnum2 {
    D, E, F
}

@Suppress("unused")
enum class TestEnumWithValues(val str: String, val level: Int) {
    A("A", 0),
    B("B", 1),
}

class EnumRoundTripSpec : StringSpec({

    "Enum round trip for enums without values" {

        data class DataClass(val e1: TestEnum1, val e2: TestEnum2)

        val codec = Codec.default
        val source = DataClass(TestEnum1.B, TestEnum2.F)
        val slumbered = codec.slumber(source)
        val result = codec.awake(DataClass::class, slumbered)!!

        result shouldBe source

        slumbered shouldBe mapOf<String, Any>(
            "e1" to "B",
            "e2" to "F"
        )
    }

    "Enum round trip for enum with values" {

        data class DataClass(val e1: TestEnumWithValues, val e2: TestEnumWithValues)

        val codec = Codec.default
        val source = DataClass(TestEnumWithValues.A, TestEnumWithValues.B)
        val slumbered = codec.slumber(source)
        val result = codec.awake(DataClass::class, slumbered)!!

        result.e1 shouldBe TestEnumWithValues.A
        result.e2 shouldBe TestEnumWithValues.B
        result shouldBe source

        slumbered shouldBe mapOf<String, Any>(
            "e1" to "A",
            "e2" to "B"
        )
    }
})
