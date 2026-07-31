package io.peekandpoke.ultra.slumber.builtin.collections

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.Polymorphic
import kotlin.reflect.typeOf

class ArrayCodecSpec : FreeSpec() {

    data class Point(val x: Int, val y: Int)

    sealed class Shape {
        data class Circle(val r: Double) : Shape()
        data class Square(val side: Double) : Shape()

        companion object : Polymorphic.Parent {
            override val childTypes = setOf(Circle::class, Square::class)
        }
    }

    private val codec = Codec.default

    init {
        "slumbering" - {

            "an object array becomes a list" {
                codec.slumber(typeOf<Array<String>>(), arrayOf("a", "b")) shouldBe listOf("a", "b")
            }

            "each primitive array becomes a list" {
                codec.slumber(typeOf<IntArray>(), intArrayOf(1, 2)) shouldBe listOf(1, 2)
                codec.slumber(typeOf<LongArray>(), longArrayOf(1L, 2L)) shouldBe listOf(1L, 2L)
                codec.slumber(typeOf<ByteArray>(), byteArrayOf(1, 2)) shouldBe listOf<Byte>(1, 2)
                codec.slumber(typeOf<ShortArray>(), shortArrayOf(1, 2)) shouldBe listOf<Short>(1, 2)
                codec.slumber(typeOf<FloatArray>(), floatArrayOf(1.5f)) shouldBe listOf(1.5f)
                codec.slumber(typeOf<DoubleArray>(), doubleArrayOf(1.5)) shouldBe listOf(1.5)
                codec.slumber(typeOf<BooleanArray>(), booleanArrayOf(true, false)) shouldBe listOf(true, false)
                // CharSlumberer maps a Char to a Char (char.kt:18); it is the JSON writer that later
                // renders it as a string, not this layer.
                codec.slumber(typeOf<CharArray>(), charArrayOf('a')) shouldBe listOf('a')
            }

            "elements are slumbered recursively" {
                codec.slumber(typeOf<Array<Point>>(), arrayOf(Point(1, 2))) shouldBe
                        listOf(mapOf("x" to 1, "y" to 2))
            }

            "an empty array becomes an empty list, not null" {
                codec.slumber(typeOf<Array<String>>(), emptyArray<String>()) shouldBe emptyList<String>()
                codec.slumber(typeOf<IntArray>(), intArrayOf()) shouldBe emptyList<Int>()
            }
        }

        "awaking" - {

            "an object array is produced with the right runtime type" {
                val result = codec.awake(typeOf<Array<String>>(), listOf("a", "b"))

                result.shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly listOf("a", "b")

                withClue("Array<String> must be String[], so a consumer can use it as such") {
                    result::class.java.componentType shouldBe String::class.java
                }
            }

            "Array<Int> is boxed — Integer[], never int[]" {
                val result = codec.awake(typeOf<Array<Int>>(), listOf(1, 2))

                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                result::class.java.componentType shouldBe Integer::class.java
            }

            "each primitive array is produced with a primitive component type" {
                codec.awake(typeOf<IntArray>(), listOf(1, 2)).shouldBeInstanceOf<IntArray>()
                    .toList() shouldContainExactly listOf(1, 2)

                codec.awake(typeOf<LongArray>(), listOf(1, 2)).shouldBeInstanceOf<LongArray>()
                    .toList() shouldContainExactly listOf(1L, 2L)

                codec.awake(typeOf<ByteArray>(), listOf(1, 2)).shouldBeInstanceOf<ByteArray>()
                codec.awake(typeOf<ShortArray>(), listOf(1, 2)).shouldBeInstanceOf<ShortArray>()
                codec.awake(typeOf<FloatArray>(), listOf(1.5)).shouldBeInstanceOf<FloatArray>()
                codec.awake(typeOf<DoubleArray>(), listOf(1.5)).shouldBeInstanceOf<DoubleArray>()
                codec.awake(typeOf<BooleanArray>(), listOf(true)).shouldBeInstanceOf<BooleanArray>()
                codec.awake(typeOf<CharArray>(), listOf("a")).shouldBeInstanceOf<CharArray>()
            }

            "elements are awoken recursively" {
                val result = codec.awake(typeOf<Array<Point>>(), listOf(mapOf("x" to 1, "y" to 2)))

                result.shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly listOf(Point(1, 2))
            }

            "an empty list awakes to an empty array" {
                codec.awake(typeOf<IntArray>(), emptyList<Int>()).shouldBeInstanceOf<IntArray>()
                    .size shouldBe 0
            }

            "an array is also accepted as INPUT data, primitive arrays included" {
                withClue("object arrays were already accepted as input; primitive arrays now are too") {
                    codec.awake(typeOf<List<String>>(), arrayOf("a", "b")) shouldBe listOf("a", "b")

                    codec.awake(typeOf<List<Int>>(), intArrayOf(1, 2)) shouldBe listOf(1, 2)

                    codec.awake(typeOf<IntArray>(), intArrayOf(1, 2)).shouldBeInstanceOf<IntArray>()
                        .toList() shouldContainExactly listOf(1, 2)

                    codec.awake(typeOf<Set<Boolean>>(), booleanArrayOf(true, true)) shouldBe setOf(true)
                }
            }
        }

        "round trips" - {

            "Array<String> with a nullable element type keeps its nulls" {
                val input = arrayOf("a", null, "b")

                val slumbered = codec.slumber(typeOf<Array<String?>>(), input)
                val awoken = codec.awake(typeOf<Array<String?>>(), slumbered)

                awoken.shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly listOf("a", null, "b")
            }

            "an array of data classes round trips" {
                val input = arrayOf(Point(1, 2), Point(3, 4))

                val awoken = codec.awake(typeOf<Array<Point>>(), codec.slumber(typeOf<Array<Point>>(), input))

                awoken.shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly input.toList()
            }

            "a polymorphic element keeps its discriminator through the round trip" {
                val input = arrayOf(Shape.Circle(1.0), Shape.Square(2.0))

                val slumbered = codec.slumber(typeOf<Array<Shape>>(), input)

                withClue("the discriminator must survive, or the elements cannot be awoken back") {
                    slumbered.shouldBeInstanceOf<List<*>>()
                        .first().shouldBeInstanceOf<Map<*, *>>()
                        .containsKey("_type") shouldBe true
                }

                val awoken = codec.awake(typeOf<Array<Shape>>(), slumbered)

                awoken.shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly input.toList()
            }

            "nested containers round trip in both directions" {
                val arrayOfLists = arrayOf(listOf("a"), listOf("b"))
                codec.awake(typeOf<Array<List<String>>>(), codec.slumber(typeOf<Array<List<String>>>(), arrayOfLists))
                    .shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly arrayOfLists.toList()

                val listOfArrays = listOf(arrayOf("a"))
                val awokenListOfArrays = codec.awake(
                    typeOf<List<Array<String>>>(),
                    codec.slumber(typeOf<List<Array<String>>>(), listOfArrays),
                )
                awokenListOfArrays.shouldBeInstanceOf<List<*>>()
                    .first().shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly listOf("a")

                val nested = arrayOf(arrayOf("a"), arrayOf("b"))
                codec.awake(typeOf<Array<Array<String>>>(), codec.slumber(typeOf<Array<Array<String>>>(), nested))
                    .shouldBeInstanceOf<Array<*>>()
                    .first().shouldBeInstanceOf<Array<*>>().toList() shouldContainExactly listOf("a")
            }

            "an array inside a data class round trips" {
                @Suppress("ArrayInDataClass")
                data class Holder(val values: IntArray)

                val slumbered = codec.slumber(typeOf<Holder>(), Holder(intArrayOf(1, 2, 3)))

                slumbered shouldBe mapOf("values" to listOf(1, 2, 3))

                codec.awake(typeOf<Holder>(), slumbered)
                    .shouldBeInstanceOf<Holder>().values.toList() shouldContainExactly listOf(1, 2, 3)
            }
        }

        "a null element in a primitive array is rejected with a path, not an NPE" {
            val thrown = runCatching {
                codec.awake(typeOf<IntArray>(), listOf(1, null, 3))
            }.exceptionOrNull()

            withClue("comes from NonNullAwaker on the non-nullable element type, before the array is written") {
                val message = thrown.shouldBeInstanceOf<AwakerException>().message!!

                message.contains("must not be null") shouldBe true

                withClue("the element index must be in the path, or the error is not actionable") {
                    message.contains("'root.1'") shouldBe true
                }
            }
        }
    }
}
