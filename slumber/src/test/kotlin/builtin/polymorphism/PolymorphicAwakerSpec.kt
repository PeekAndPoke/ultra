package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PolymorphicAwakerSpec : StringSpec({

    //  pure sealed class and pure sealed children  ////////////////////////////////////////////////////////////////////

    "Awaking a sealed class (PureBase.A) without using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            PureBase::class,
            mapOf(
                "_type" to PureBase.A::class.qualifiedName,
                "text" to "hello"
            )
        )

        result shouldBe PureBase.A("hello")
    }

    "Awaking a sealed class (PureBase.B) without using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            PureBase::class,
            mapOf(
                "_type" to PureBase.B::class.qualifiedName,
                "number" to "100"
            )
        )

        result shouldBe PureBase.B(100)
    }

    "Awaking a sealed class with '_type' missing in the data must return null" {

        val codec = Codec.default

        val result = codec.awake(
            kType<PureBase>().nullable.type,
            mapOf<String, Any>()
        )

        result shouldBe null
    }

    "Awaking a sealed class with an invalid type must return null" {

        val codec = Codec.default

        val result = codec.awake(
            kType<PureBase>().nullable.type,
            mapOf(
                "_type" to "UNKNOWN"
            )
        )

        result shouldBe null
    }

    //  Sealed parent with custom discriminator  ///////////////////////////////////////////////////////////////////////

    "Awaking children with a custom discriminator name" {

        val codec = Codec.default

        val result = codec.awake(
            kListType<CustomDiscriminator>().type,
            listOf(
                mapOf(
                    "_" to CustomDiscriminator.A.identifier,
                    "text" to "hello"
                ),
                mapOf(
                    "_" to CustomDiscriminator.B.identifier,
                    "number" to 100
                )
            )
        )

        result shouldBe listOf(
            CustomDiscriminator.A("hello"),
            CustomDiscriminator.B(100)
        )
    }

    //  Sealed parent with 'defaultType' configured  ///////////////////////////////////////////////////////////////////

    "Awaking a sealed class with '_type' missing and 'defaultType' configured must awake the default" {

        val codec = Codec.default

        val result = codec.awake(
            BaseWithDefaultType::class,
            mapOf(
                "text" to "hello"
            )
        )

        result shouldBe BaseWithDefaultType.A("hello")
    }

    "Awaking a sealed class with unknown '_type' and 'defaultType' configured must awake the default" {

        val codec = Codec.default

        val result = codec.awake(
            BaseWithDefaultType::class,
            mapOf(
                "_type" to "UNKNOWN",
                "text" to "hello"
            )
        )

        result shouldBe BaseWithDefaultType.A("hello")
    }

    "Awaking a sealed class with 'defaultType' and '_type' (BaseWithDefaultType.A) must work" {

        val codec = Codec.default

        val result = codec.awake(
            BaseWithDefaultType::class,
            mapOf(
                "_type" to "A",
                "text" to "hello"
            )
        )

        result shouldBe BaseWithDefaultType.A("hello")
    }

    "Awaking a sealed class with 'defaultType' and '_type' (BaseWithDefaultType.B) must work" {

        val codec = Codec.default

        val result = codec.awake(
            BaseWithDefaultType::class,
            mapOf(
                "_type" to "Child_B",
                "number" to "100"
            )
        )

        result shouldBe BaseWithDefaultType.B(100)
    }

    //  Pure sealed class with annotated children  /////////////////////////////////////////////////////////////////////

    "Awaking a sealed class (AnnotedChildrenBase.A) using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotedChildrenBase::class,
            mapOf(
                "_type" to "Child_A",
                "text" to "hello"
            )
        )

        result shouldBe AnnotedChildrenBase.A("hello")
    }

    "Awaking a sealed class (AnnotedChildrenBase.B) using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotedChildrenBase::class,
            mapOf(
                "_type" to "Child_B",
                "number" to "100"
            )
        )

        result shouldBe AnnotedChildrenBase.B(100)
    }

    //  Non sealed base class annotated with Polymorphic.Parent  ///////////////////////////////////////////////////////

    "Directly Awaking a non sealed class (AnnotatedBase.A) where the parent is annotated with Polymorphic.Parent" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotatedBase::class,
            mapOf(
                "_type" to AnnotatedBase.A::class.qualifiedName,
                "text" to "hello"
            )
        )

        result shouldBe AnnotatedBase.A("hello")
    }

    "Directly Awaking a non sealed class (AnnotatedBase.B) where the parent is annotated with Polymorphic.Parent" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotatedBase::class,
            mapOf(
                "_type" to "Child_B",
                "number" to "100"
            )
        )

        result shouldBe AnnotatedBase.B(100)
    }

    "Directly Awaking a polymorphic annotated with @SerialName" {

        val codec = Codec.default

        val result = codec.awake(
            ChildrenUsingAnnotation::class,
            mapOf(
                "_type" to "Sub2",
                "text" to "Sub2-value",
            ),
        )

        result shouldBe ChildrenUsingAnnotation.Sub2("Sub2-value")
    }

    // Deeper class hierarchies  ///////////////////////////////////////////////////////////////////////////////////////

    "Awaking children of deeper sealed class hierarchies" {

        val codec = Codec.default

        val result = codec.awake(
            kType<SealedRoot>().list.type,
            listOf(
                mapOf(
                    "_type" to SealedRoot.NestedA.DeeperA::class.qualifiedName,
                    "text" to "DeeperA"
                ),
                mapOf(
                    "_type" to SealedRoot.NestedA.DeeperB::class.qualifiedName,
                    "text" to "DeeperB"
                ),
                mapOf(
                    "_type" to SealedRoot.NestedB::class.qualifiedName,
                    "text" to "NestedB"
                )
            )
        )

        result shouldBe listOf(
            SealedRoot.NestedA.DeeperA("DeeperA"),
            SealedRoot.NestedA.DeeperB("DeeperB"),
            SealedRoot.NestedB("NestedB")
        )
    }

    //  Using ClassIndex  //////////////////////////////////////////////////////////////////////////////////////////////

    "Awaking children using ClassIndex - childTypes using indexSubClasses must be correct" {

        ParentWithKlassIndex.childTypes shouldBe setOf(
            ParentWithKlassIndex.Sub1::class,
            ParentWithKlassIndex.Sub1.Deeper1::class,
            ParentWithKlassIndex.Sub1.Deeper2::class,
            ParentWithKlassIndex.Sub2::class
        )
    }

    "Awaking children of deeper hierarchies using ClassIndex" {

        val codec = Codec.default

        val result = codec.awake(
            kType<ParentWithKlassIndex>().list.type,
            listOf(
                mapOf(
                    "_type" to ParentWithKlassIndex.Sub1.Deeper1::class.qualifiedName,
                    "text" to "Deeper1"
                ),
                mapOf(
                    "_type" to ParentWithKlassIndex.Sub1.Deeper2::class.qualifiedName,
                    "text" to "Deeper2"
                ),
                mapOf(
                    "_type" to ParentWithKlassIndex.Sub2::class.qualifiedName,
                    "text" to "Sub2"
                )
            )
        )

        result shouldBe listOf(
            ParentWithKlassIndex.Sub1.Deeper1("Deeper1"),
            ParentWithKlassIndex.Sub1.Deeper2("Deeper2"),
            ParentWithKlassIndex.Sub2("Sub2")
        )
    }

    "Awaking children of deeper hierarchies using ClassIndex and @SerialName Annotations" {

        val codec = Codec.default

        val result = codec.awake(
            kType<ChildrenUsingAnnotation>().list.type,
            listOf(
                mapOf(
                    "_type" to "Sub.Deeper1",
                    "text" to "Deeper1"
                ),
                mapOf(
                    "_type" to "Sub.Deeper2",
                    "text" to "Deeper2"
                ),
                mapOf(
                    "_type" to "Sub2",
                    "text" to "Sub2"
                )
            )
        )

        result shouldBe listOf(
            ChildrenUsingAnnotation.Sub.Deeper1("Deeper1"),
            ChildrenUsingAnnotation.Sub.Deeper2("Deeper2"),
            ChildrenUsingAnnotation.Sub2("Sub2")
        )
    }

    //  Complex examples  //////////////////////////////////////////////////////////////////////////////////////////////

    "Awaking a data class that contains polymorphics - one" {

        data class DataClass(
            val single: AnnotedChildrenBase,
            val list: List<AnnotedChildrenBase>,
            val map: Map<String, AnnotedChildrenBase>
        )

        val codec = Codec.default

        println(PureBase.B::class.qualifiedName)

        val result = codec.awake(
            DataClass::class,
            mapOf(
                "single" to mapOf("_type" to "Child_B", "number" to 100),
                "list" to listOf(
                    mapOf("_type" to "Child_A", "text" to "hello"),
                    mapOf("_type" to "Child_B", "number" to 200)
                ),
                "map" to mapOf(
                    "A" to mapOf(
                        "_type" to "Child_A", "text" to "again"
                    )
                )
            )
        )

        result shouldBe DataClass(
            single = AnnotedChildrenBase.B(100),
            list = listOf(
                AnnotedChildrenBase.A("hello"),
                AnnotedChildrenBase.B(200)
            ),
            map = mapOf(
                "A" to AnnotedChildrenBase.A("again")
            )
        )
    }

    "Awaking a data class that contains polymorphics - two" {

        data class DataClass(val lists: List<List<AnnotedChildrenBase>>)

        val codec = Codec.default

        val result = codec.awake(
            DataClass::class,
            mapOf(
                "lists" to listOf(
                    listOf(
                        mapOf("_type" to "Child_A", "text" to "hello"),
                        mapOf("_type" to "Child_B", "number" to 100)
                    ),
                    listOf(
                        mapOf("_type" to "Child_B", "number" to 200),
                        mapOf("_type" to "Child_A", "text" to "again")
                    )
                )
            )
        )

        result shouldBe DataClass(
            lists = listOf(
                listOf(
                    AnnotedChildrenBase.A("hello"),
                    AnnotedChildrenBase.B(100)
                ),
                listOf(
                    AnnotedChildrenBase.B(200),
                    AnnotedChildrenBase.A("again")
                )
            )
        )
    }
})
