package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PolymorphicRoundTripSpec : StringSpec({

    "Slumbering and awaking round trip of sealed classes" {

        data class DataClass(
            val single: PureSealedClass,
            val list: List<PureSealedClass>,
            val map: Map<String, PureSealedClass>,
        )

        val codec = Codec.default

        val source = DataClass(
            single = PureSealedClass.B(100),
            list = listOf(
                PureSealedClass.A("hello"),
                PureSealedClass.B(200)
            ),
            map = mapOf(
                "A" to PureSealedClass.A("again")
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf(
            "single" to mapOf("_type" to PureSealedClass.B::class.qualifiedName, "number" to 100),
            "list" to listOf(
                mapOf("_type" to PureSealedClass.A::class.qualifiedName, "text" to "hello"),
                mapOf("_type" to PureSealedClass.B::class.qualifiedName, "number" to 200)
            ),
            "map" to mapOf(
                "A" to mapOf(
                    "_type" to PureSealedClass.A::class.qualifiedName, "text" to "again"
                )
            )
        )
    }

    "Slumbering and awaking round trip of sealed classes - list of list of sealed classes" {

        data class DataClass(val lists: List<List<PureSealedClass>>)

        val codec = Codec.default

        val source = DataClass(
            lists = listOf(
                listOf(
                    PureSealedClass.A("hello"),
                    PureSealedClass.B(100)
                ),
                listOf(
                    PureSealedClass.B(200),
                    PureSealedClass.A("again")
                )
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf(
            "lists" to listOf(
                listOf(
                    mapOf("_type" to PureSealedClass.A::class.qualifiedName, "text" to "hello"),
                    mapOf("_type" to PureSealedClass.B::class.qualifiedName, "number" to 100)
                ),
                listOf(
                    mapOf("_type" to PureSealedClass.B::class.qualifiedName, "number" to 200),
                    mapOf("_type" to PureSealedClass.A::class.qualifiedName, "text" to "again")
                )
            )
        )
    }

    "Slumbering and awaking round trip of sealed interface" {

        data class DataClass(
            val single: SealedInterface,
            val list: List<SealedInterface>,
            val map: Map<String, SealedInterface>,
        )

        val codec = Codec.default

        val source = DataClass(
            single = SealedInterface.B(100),
            list = listOf(
                SealedInterface.A("hello"),
                SealedInterface.B(200),
            ),
            map = mapOf(
                "A" to SealedInterface.A("again"),
            )
        )

        val slumbered = codec.slumber(source)

        val result = codec.awake(DataClass::class, slumbered)

        result shouldBe source

        slumbered shouldBe mapOf(
            "single" to mapOf("_type" to "B", "number" to 100),
            "list" to listOf(
                mapOf("_type" to "A", "text" to "hello"),
                mapOf("_type" to "B", "number" to 200)
            ),
            "map" to mapOf(
                "A" to mapOf(
                    "_type" to "A", "text" to "again"
                )
            )
        )
    }
})
