package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.Row2
import io.kotest.matchers.shouldBe

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
            codec.awake(nonNullType, input) shouldBe expected
        }
    }

    nonNullSamples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName? from '$input' ($inputClass) should result in '$expected'" {
            codec.awake(nullableType, input) shouldBe expected
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName from '$input' ($inputClass) should throw" {
            shouldThrow<AwakerException> {
                codec.awake(nonNullType, input)
            }
        }
    }

    nullableSamples.forEach { input ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a $clsName? from '$input' ($inputClass) should result in 'null'" {
            codec.awake(nullableType, input) shouldBe null
        }
    }
})
