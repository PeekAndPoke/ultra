package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PolymorphicRoundTripSpec : StringSpec({

    "Slumbering and awaking round trip - one" {

        data class DataClass(val single: PureBase, val list: List<PureBase>, val map: Map<String, PureBase>)

        val codec = Codec.default

        val source = DataClass(
            single = PureBase.B(100),
            list = listOf(
                PureBase.A("hello"),
                PureBase.B(200)
            ),
            map = mapOf(
                "A" to PureBase.A("again")
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf(
            "single" to mapOf("_type" to PureBase.B::class.qualifiedName, "number" to 100),
            "list" to listOf(
                mapOf("_type" to PureBase.A::class.qualifiedName, "text" to "hello"),
                mapOf("_type" to PureBase.B::class.qualifiedName, "number" to 200)
            ),
            "map" to mapOf(
                "A" to mapOf(
                    "_type" to PureBase.A::class.qualifiedName, "text" to "again"
                )
            )
        )
    }

    "Slumbering and awaking round trip - two - list of list of sealed classes" {

        data class DataClass(val lists: List<List<PureBase>>)

        val codec = Codec.default

        val source = DataClass(
            lists = listOf(
                listOf(
                    PureBase.A("hello"),
                    PureBase.B(100)
                ),
                listOf(
                    PureBase.B(200),
                    PureBase.A("again")
                )
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf(
            "lists" to listOf(
                listOf(
                    mapOf("_type" to PureBase.A::class.qualifiedName, "text" to "hello"),
                    mapOf("_type" to PureBase.B::class.qualifiedName, "number" to 100)
                ),
                listOf(
                    mapOf("_type" to PureBase.B::class.qualifiedName, "number" to 200),
                    mapOf("_type" to PureBase.A::class.qualifiedName, "text" to "again")
                )
            )
        )
    }
})
