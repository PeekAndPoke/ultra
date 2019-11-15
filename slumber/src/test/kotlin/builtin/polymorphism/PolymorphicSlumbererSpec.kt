package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.common.kListType
import de.peekandpoke.ultra.common.kMapType
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PolymorphicSlumbererSpec : StringSpec({

    "Slumbering a polymorphic child independently must NOT include the discriminator" {

        val codec = Codec.default

        val result = codec.slumber(PureBase.A("hello"))

        assertSoftly {
            result shouldBe mapOf("text" to "hello")
        }
    }

    "Slumbering a list of sealed class children (PureBase)" {

        val codec = Codec.default

        val data = listOf(
            PureBase.A("hello"),
            PureBase.B(100)
        )

        val result = codec.slumber(kListType<PureBase>().type, data)

        assertSoftly {
            result shouldBe listOf(
                mapOf(
                    "_type" to "A",
                    "text" to "hello"
                ),
                mapOf(
                    "_type" to "B",
                    "number" to 100
                )
            )
        }
    }

    "Slumbering a map of string to sealed class children (PureBase)" {

        val codec = Codec.default

        val data = mapOf(
            "A" to PureBase.A("hello"),
            "B" to PureBase.B(100)
        )

        val result = codec.slumber(kMapType<String, PureBase>().type, data)

        assertSoftly {
            result shouldBe mapOf(
                "A" to mapOf(
                    "_type" to "A",
                    "text" to "hello"
                ),
                "B" to mapOf(
                    "_type" to "B",
                    "number" to 100
                )
            )
        }
    }
})
