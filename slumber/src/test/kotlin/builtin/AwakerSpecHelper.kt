package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.Row2
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

abstract class AwakerSpecHelper(
    cls: KClass<*>,
    nonNullSamples: List<Row2<*, *>>,
    nullableSamples: List<*>
) : StringSpec({

    val clsName = cls.qualifiedName

    val nullableType = cls.createType(listOf(), true)
    val nonNullType = cls.createType(listOf(), false)

    val codec = Codec.default

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                codec.awakeOrNull(nonNullType, input) shouldBe expected
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
                codec.awakeOrNull(nullableType, input) shouldBe expected
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
                    codec.awakeOrNull(nonNullType, input)
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
                codec.awakeOrNull(nullableType, input) shouldBe null
            }
        }
    }
})
