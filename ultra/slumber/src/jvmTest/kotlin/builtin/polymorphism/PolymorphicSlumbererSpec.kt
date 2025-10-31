package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.common.reflection.kMapType
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PolymorphicSlumbererSpec : StringSpec({

    //  Directly slumbering polymorphic children  //////////////////////////////////////////////////////////////////////

    "Slumbering a polymorphic sealed class child class must include the discriminator" {

        val codec = Codec.default

        val result = codec.slumber(PureSealedClass.A("hello"))

        result shouldBe mapOf(
            "_type" to PureSealedClass.A::class.qualifiedName,
            "text" to "hello",
        )
    }

    "Slumbering a polymorphic open class child class must include the discriminator" {

        val codec = Codec.default

        val result = codec.slumber(AnnotatedBase.A("hello"))

        result shouldBe mapOf(
            "_type" to AnnotatedBase.A::class.qualifiedName,
            "text" to "hello",
        )
    }

    //  pure sealed class and pure sealed children  ////////////////////////////////////////////////////////////////////

    "Slumbering a list of sealed class children (PureBase)" {

        val codec = Codec.default

        val data = listOf(
            PureSealedClass.A("hello"),
            PureSealedClass.B(100)
        )

        val result = codec.slumber(kListType<PureSealedClass>().type, data)

        result shouldBe listOf(
            mapOf(
                "_type" to PureSealedClass.A::class.qualifiedName,
                "text" to "hello"
            ),
            mapOf(
                "_type" to PureSealedClass.B::class.qualifiedName,
                "number" to 100
            )
        )
    }

    "Slumbering a map of string to sealed class children (PureBase)" {

        val codec = Codec.default

        val data = mapOf(
            "A" to PureSealedClass.A("hello"),
            "B" to PureSealedClass.B(100)
        )

        val result = codec.slumber(kMapType<String, PureSealedClass>().type, data)

        result shouldBe mapOf(
            "A" to mapOf(
                "_type" to PureSealedClass.A::class.qualifiedName,
                "text" to "hello"
            ),
            "B" to mapOf(
                "_type" to PureSealedClass.B::class.qualifiedName,
                "number" to 100
            )
        )
    }

    //  Sealed parent with custom discriminator  ///////////////////////////////////////////////////////////////////////

    "Slumbering a list of sealed class children (CustomDiscriminator) for parent with custom discriminator" {

        val codec = Codec.default

        val data = listOf(
            CustomDiscriminator.A("hello"),
            CustomDiscriminator.B(100)
        )

        val result = codec.slumber(kListType<CustomDiscriminator>().type, data)

        result shouldBe listOf(
            mapOf(
                "_" to CustomDiscriminator.A.identifier,
                "text" to "hello"
            ),
            mapOf(
                "_" to CustomDiscriminator.B.identifier,
                "number" to 100
            )
        )
    }

    //  Pure sealed class with annotated children  /////////////////////////////////////////////////////////////////////

    "Slumbering a list of sealed class children with custom type identifiers (AnnotedChildrenBase)" {

        val codec = Codec.default

        val data = listOf(
            AnnotatedChildrenBase.A("hello"),
            AnnotatedChildrenBase.B(100)
        )

        val result = codec.slumber(kListType<AnnotatedChildrenBase>().type, data)

        result shouldBe listOf(
            mapOf(
                "_type" to "Child_A",
                "text" to "hello"
            ),
            mapOf(
                "_type" to "Child_B",
                "number" to 100
            )
        )
    }

    //  Non sealed base class annotated with Polymorphic.Parent  ///////////////////////////////////////////////////////

    "Slumbering a list of children of a non sealed parent class (AnnotatedBase)" {

        val codec = Codec.default

        val data = listOf(
            AnnotatedBase.A("hello"),
            AnnotatedBase.B(100)
        )

        val result = codec.slumber(kListType<AnnotatedBase>().type, data)

        result shouldBe listOf(
            mapOf(
                "_type" to AnnotatedBase.A::class.qualifiedName,
                "text" to "hello"
            ),
            mapOf(
                "_type" to "Child_B",
                "number" to 100
            )
        )
    }

    //  Direct slumbering of a Polymorphic.Child  //////////////////////////////////////////////////////////////////////

    "Directly slumbering a polymorphic child must include the discriminator" {

        val codec = Codec.default

        val data = AnnotatedBase.B(number = 111)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_type" to AnnotatedBase.B.identifier,
            "number" to 111
        )
    }

    "Directly slumbering a polymorphic child in a deeper sealed class hierarchy must include the discriminator" {

        val codec = Codec.default

        val data = SealedRoot.NestedA.DeeperA(text = "DeeperA")

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_type" to SealedRoot.NestedA.DeeperA::class.qualifiedName,
            "text" to "DeeperA"
        )
    }

    "Directly slumbering a polymorphic with custom discriminator child must include the discriminator" {

        val codec = Codec.default

        val data = CustomDiscriminator.B(number = 111)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_" to CustomDiscriminator.B.identifier,
            "number" to 111
        )
    }

    "Directly slumbering a polymorphic within a deeper hierarchy with IndexSubclasses must include the discriminator" {

        val codec = Codec.default

        val data = ParentWithKlassIndex.Sub1.Deeper1(text = "Deeper1")

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_type" to ParentWithKlassIndex.Sub1.Deeper1::class.qualifiedName,
            "text" to "Deeper1"
        )
    }

    "Directly slumbering a polymorphic annotated with @SerialName" {

        val codec = Codec.default

        val data = ChildrenUsingAnnotation.Sub2("Sub2-value")

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_type" to "Sub2",
            "text" to "Sub2-value",
        )
    }

    "Directly slumbering a polymorphic annotated with @SerialName and @AdditionalSerialName" {

        val codec = Codec.default

        val data = ChildrenUsingAnnotation.Sub3(100)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "_type" to "Sub3",
            "num" to 100,
        )
    }
})
