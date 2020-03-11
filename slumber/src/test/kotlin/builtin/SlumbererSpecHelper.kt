package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumbererException
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.Row2

abstract class SlumbererSpecHelper(
    type: TypeRef<*>,
    nonNullSamples: List<Row2<*, *>>,
    nullableSamples: List<*>
) : StringSpec({

    val clsName = type.type.toString()

    val nonNullType = type.type
    val nullableType = type.nullable.type

    val codec = Codec.default

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering a $clsName from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                codec.slumber(nonNullType, input) shouldBe expected
            }
        }
    }

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering '$input' ($inputClass) into a $clsName? should result in '$expected'" {

            assertSoftly {
                codec.slumber(nullableType, input) shouldBe expected
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering a $clsName from '$input' ($inputClass) should throw" {

            assertSoftly {
                shouldThrow<SlumbererException> {
                    codec.slumber(nonNullType, input)
                }
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering '$input' ($inputClass) into a $clsName? from should result in 'null'" {

            assertSoftly {
                codec.slumber(nullableType, input) shouldBe null
            }
        }
    }
})
