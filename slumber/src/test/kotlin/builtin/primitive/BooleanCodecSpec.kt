package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class BooleanCodecSpec : StringSpec({

    val codec = Codec.default

    @Suppress("BooleanLiteralArgument")
    val samples = listOf(
        row(true, true),
        row(1, true),
        row(2, true),
        row(1.1, true),
        row("true", true),
        row("1", true),
        row("2", true),

        row(false, false),
        row(0, false),
        row(0.9999, false),
        row("0", false),
        row("false", false),
        row("stuff", false),

        row(null, null),
        row(emptyList<Any>(), null),
        row(emptyMap<Any, Any>(), null)
    )

    samples.forEach { (input, expected) ->

        val inputClass = when {
            input != null -> input::class.qualifiedName
            else -> "null"
        }

        "Awaking a Boolean from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                withClue("Using Codec.default must work") {
                    codec.awakeOrNull(Boolean::class, input) shouldBe expected
                }
            }
        }

        "Slumbering a Boolean from '$input' ($inputClass) should result in '$expected'" {

            assertSoftly {
                withClue("Using Codec.default must work") {
                    codec.slumber(Boolean::class, input) shouldBe expected
                }
            }
        }
    }
})
