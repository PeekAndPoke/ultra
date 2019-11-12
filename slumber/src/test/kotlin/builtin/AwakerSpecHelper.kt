package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.TypeRef
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.Row2

abstract class AwakerSpecHelper(
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

        "Awaking a $clsName from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                codec.awake(nonNullType, input) shouldBe expected
            }
        }
    }

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName? from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                codec.awake(nullableType, input) shouldBe expected
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName from '$input' ($inputClass) should throw" {

            assertSoftly {
                shouldThrow<AwakerException> {
                    codec.awake(nonNullType, input)
                }
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName? from '$input' ($inputClass) should result in 'null'" {

            assertSoftly {
                codec.awake(nullableType, input) shouldBe null
            }
        }
    }
})
