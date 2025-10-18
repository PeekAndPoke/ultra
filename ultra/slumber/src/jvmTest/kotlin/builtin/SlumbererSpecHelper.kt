package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.model.Tuple2
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumbererException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

abstract class SlumbererSpecHelper(
    type: TypeRef<*>,
    nonNullSamples: List<Tuple2<*, *>>,
    nullableSamples: List<*>,
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
            codec.slumber(nonNullType, input) shouldBe expected
        }
    }

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering '$input' ($inputClass) into a $clsName? should result in '$expected'" {
            codec.slumber(nullableType, input) shouldBe expected
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering a $clsName from '$input' ($inputClass) should throw" {
            shouldThrow<SlumbererException> {
                codec.slumber(nonNullType, input)
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Slumbering '$input' ($inputClass) into a $clsName? from should result in 'null'" {
            codec.slumber(nullableType, input) shouldBe null
        }
    }
})
