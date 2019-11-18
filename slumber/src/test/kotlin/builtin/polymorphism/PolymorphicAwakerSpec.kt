package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.common.kListType
import de.peekandpoke.ultra.common.kType
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PolymorphicAwakerSpec : StringSpec({

    ////  pure sealed class and pure sealed children  //////////////////////////////////////////////////////////////////

    "Awaking a sealed class (PureBase.A) without using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            PureBase::class,
            mapOf(
                "_type" to PureBase.A::class.qualifiedName,
                "text" to "hello"
            )
        )

        assertSoftly {
            result shouldBe PureBase.A("hello")
        }
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

        assertSoftly {
            result shouldBe PureBase.B(100)
        }
    }

    "Awaking a sealed class with '_type' missing in the data must return null" {

        val codec = Codec.default

        val result = codec.awake(
            kType<PureBase>().nullable.type,
            mapOf<String, Any>()
        )

        assertSoftly {
            result shouldBe null
        }
    }

    "Awaking a sealed class with an invalid type must return null" {

        val codec = Codec.default

        val result = codec.awake(
            kType<PureBase>().nullable.type,
            mapOf(
                "_type" to "UNKNOWN"
            )
        )

        assertSoftly {
            result shouldBe null
        }
    }

    ////  Sealed parent with custom discriminator  /////////////////////////////////////////////////////////////////////

    "Awaking children with a custom discriminator name" {

        val codec = Codec.default

        val result = codec.awake(
            kListType<CustomDiscriminator>().type,
            listOf(
                mapOf(
                    "_" to CustomDiscriminator.A::class.qualifiedName,
                    "text" to "hello"
                ),
                mapOf(
                    "_" to CustomDiscriminator.B::class.qualifiedName,
                    "number" to 100
                )
            )
        )

        assertSoftly {
            result shouldBe listOf(
                CustomDiscriminator.A("hello"),
                CustomDiscriminator.B(100)
            )
        }
    }

    ////  Sealed parent with 'defaultType' configured  /////////////////////////////////////////////////////////////////

    "Awaking a sealed class with '_type' missing and 'defaultType' configured must awake the default" {

        val codec = Codec.default

        val result = codec.awake(
            BaseWithDefaultType::class,
            mapOf(
                "text" to "hello"
            )
        )

        assertSoftly {
            result shouldBe BaseWithDefaultType.A("hello")
        }
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

        assertSoftly {
            result shouldBe BaseWithDefaultType.A("hello")
        }
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

        assertSoftly {
            result shouldBe BaseWithDefaultType.A("hello")
        }
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

        assertSoftly {
            result shouldBe BaseWithDefaultType.B(100)
        }
    }

    ////  Pure sealed class with annotated children  ///////////////////////////////////////////////////////////////////

    "Awaking a sealed class (AnnotedChildrenBase.A) using Polymorphic.Child" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotedChildrenBase::class,
            mapOf(
                "_type" to "Child_A",
                "text" to "hello"
            )
        )

        assertSoftly {
            result shouldBe AnnotedChildrenBase.A("hello")
        }
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

        assertSoftly {
            result shouldBe AnnotedChildrenBase.B(100)
        }
    }

    ////  Non sealed base class annotated with Polymorphic.Parent  /////////////////////////////////////////////////////

    "Awaking a non sealed class (AnnotatedBase.A) where the parent is annotated with Polymorphic.Parent" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotatedBase::class,
            mapOf(
                "_type" to AnnotatedBase.A::class.qualifiedName,
                "text" to "hello"
            )
        )

        assertSoftly {
            result shouldBe AnnotatedBase.A("hello")
        }
    }

    "Awaking a non sealed class (AnnotatedBase.B) where the parent is annotated with Polymorphic.Parent" {

        val codec = Codec.default

        val result = codec.awake(
            AnnotatedBase::class,
            mapOf(
                "_type" to "Child_B",
                "number" to "100"
            )
        )

        assertSoftly {
            result shouldBe AnnotatedBase.B(100)
        }
    }

    //// Deeper class hierarchies  /////////////////////////////////////////////////////////////////////////////////////

    "Awaking children of deeper sealed class hierarchies" {

        val codec = Codec.default

        val result = codec.awake(
            kType<NestedRoot>().list.type,
            listOf(
                mapOf(
                    "_type" to NestedRoot.NestedA.DeeperA::class.qualifiedName,
                    "text" to "DeeperA"
                ),
                mapOf(
                    "_type" to NestedRoot.NestedA.DeeperB::class.qualifiedName,
                    "text" to "DeeperB"
                ),
                mapOf(
                    "_type" to NestedRoot.NestedB::class.qualifiedName,
                    "text" to "NestedB"
                )
            )
        )

        assertSoftly {
            result shouldBe listOf(
                NestedRoot.NestedA.DeeperA("DeeperA"),
                NestedRoot.NestedA.DeeperB("DeeperB"),
                NestedRoot.NestedB("NestedB")
            )
        }
    }

    ////  Complex examples  ////////////////////////////////////////////////////////////////////////////////////////////

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
