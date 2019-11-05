package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class CharCodecSpec : StringSpec({

    val codec = Codec.default

    @Suppress("BooleanLiteralArgument")
    val samples = listOf(
        row('a', 'a'),
        row('b', 'b'),
        row("true", 't'),
        row("FALSE", 'F'),
        row("stuff", 's'),

        row(null, null),
        row(false, null),
        row(true, null),
        row("", null),
        row(emptyList<Any>(), null),
        row(emptyMap<Any, Any>(), null)
    )

    samples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a Char from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                withClue("Using Codec.default must work") {
                    codec.awakeOrNull(Char::class, input) shouldBe expected
                }
            }
        }

        "Slumbering a Boolean from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                withClue("Using Codec.default must work") {
                    codec.slumber(Char::class, input) shouldBe expected
                }
            }
        }
    }
})
