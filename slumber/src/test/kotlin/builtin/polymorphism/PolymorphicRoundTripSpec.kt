package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

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

        assertSoftly {
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

        assertSoftly {
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
    }

    "Slumbering and awaking round trip - three - list children using annotations" {

        data class DataClass(val list: List<ParentWithChildrenUsingAnnotation>)

        val codec = Codec.default

        val source = DataClass(
            list = listOf(
                ParentWithChildrenUsingAnnotation.Sub.Deeper1("deeper1"),
                ParentWithChildrenUsingAnnotation.Sub.Deeper2("deeper2"),
                ParentWithChildrenUsingAnnotation.Sub2("sub2")
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        assertSoftly {
            result shouldBe source

            slumbered shouldBe mapOf(
                "list" to listOf(
                    mapOf("_type" to "Sub.Deeper1", "text" to "deeper1"),
                    mapOf("_type" to "Sub.Deeper2", "text" to "deeper2"),
                    mapOf("_type" to "Sub2", "text" to "sub2")
                )
            )
        }
    }

})
