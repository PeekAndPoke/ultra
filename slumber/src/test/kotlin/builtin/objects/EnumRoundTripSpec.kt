package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@Suppress("unused")
enum class TestEnum1 {
    A, B, C
}

@Suppress("unused")
enum class TestEnum2 {
    D, E, F
}

class EnumRoundTripSpec : StringSpec({

    "Enum round trip - one" {

        data class DataClass(val e1: TestEnum1, val e2: TestEnum2)

        val codec = Codec.default

        val source = DataClass(TestEnum1.B, TestEnum2.F)

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)!!

        assertSoftly {
            result shouldBe source

            slumbered shouldBe mapOf<String, Any>(
                "e1" to "B",
                "e2" to "F"
            )
        }
    }
})
