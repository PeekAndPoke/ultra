package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NumbersSpec : StringSpec() {
    init {
        "Number.toFixed(decimals)" {
            listOf(
                tuple(0, 0, "0"),
                tuple(0, 1, "0.0"),
                tuple(1E-1, 2, "0.10"),
                tuple(-1E-1, 2, "-0.10"),
                tuple(1.1E-10, 2, "0.00"),
                tuple(-1.1E-10, 2, "-0.00"),
                tuple(1.1E+10, 2, "11000000000.00"),
                tuple(-1.1E+10, 2, "-11000000000.00"),
                tuple(0, -1, "0"),
                tuple(1, -1, "1"),
                tuple(1, 0, "1"),
                tuple(-1, 0, "-1"),
                tuple(11, 0, "11"),
                tuple(-11, 0, "-11"),
                tuple(11.9, 2, "11.90"),
                tuple(-11.9, 3, "-11.900"),
                tuple(1.1, 1, "1.1"),
                tuple(1.1, 2, "1.10"),
                tuple(1.11, 2, "1.11"),
                tuple(1.11, 3, "1.110"),
                tuple(1.11f, 3, "1.110"),
                tuple(1.111, 3, "1.111"),
                tuple(1.111f, 3, "1.111"),
                tuple(1.1111f, 3, "1.111"),
                tuple(1.11111f, 3, "1.111"),
                tuple(21.1, 1, "21.1"),
                tuple(21.1, 2, "21.10"),
                tuple(21.11, 2, "21.11"),
                tuple(21.11, 3, "21.110"),
                tuple(21.11f, 3, "21.110"),
                tuple(21.111, 3, "21.111"),
                tuple(21.111f, 3, "21.111"),
                tuple(21.1111f, 3, "21.111"),
                tuple(21.11111f, 3, "21.111"),
                tuple(99.9, 0, "100"),
                tuple(99.9, 1, "99.9"),
                tuple(99.9, 2, "99.90"),
                tuple(99.99, 0, "100"),
                tuple(99.99, 1, "100.0"),
                tuple(99.99, 2, "99.99"),
                tuple(99.99, 3, "99.990"),
                tuple(99.999, 3, "99.999"),
                tuple(99.999, 4, "99.9990"),
                tuple(99.9999, 4, "99.9999"),
                tuple(99.9999, 5, "99.99990"),
                tuple(-1.1, 1, "-1.1"),
                tuple(-1.1, 2, "-1.10"),
                tuple(-99.9, 0, "-100"),
                tuple(-99.9, 1, "-99.9"),
                tuple(-99.9, 2, "-99.90"),
                tuple(-99.99, 0, "-100"),
                tuple(-99.99, 1, "-100.0"),
                tuple(-99.99, 2, "-99.99"),
                tuple(-99.99, 3, "-99.990"),
                tuple(-99.999, 3, "-99.999"),
                tuple(-99.999, 4, "-99.9990"),
                tuple(-99.9999, 4, "-99.9999"),
                tuple(-99.9999, 5, "-99.99990"),
            ).forEach { (input: Number, decimals: Int, expected: String) ->
                withClue("$input.toFixed($decimals) should be $expected") {
                    input.toFixed(decimals) shouldBe expected
                }
            }
        }

        "roundWithPrecision(precisions)" {

            listOf(
                tuple(0 as Number, 0, 0.0),
                tuple(0.0 as Number, 0, 0.0),
                tuple(0.0f as Number, 0, 0.0),
                tuple(1.0f as Number, 0, 1.0),
                // rounding down
                tuple(1111.1111 as Number, 5, 1111.1111),
                tuple(1111.1111 as Number, 4, 1111.1111),
                tuple(1111.1111 as Number, 3, 1111.1110),
                tuple(1111.1111 as Number, 2, 1111.1100),
                tuple(1111.1111 as Number, 1, 1111.1000),
                tuple(1111.1111 as Number, 0, 1111.0000),
                tuple(1111.1111 as Number, -1, 1110.0000),
                tuple(1111.1111 as Number, -2, 1100.0000),
                tuple(1111.1111 as Number, -3, 1000.0000),
                tuple(1111.1111 as Number, -4, 0000.0000),
                tuple(1111.1111 as Number, -5, 0000.0000),
                // rounding up
                tuple(5555.5555 as Number, 0, 5556.0000),
                tuple(5555.5555 as Number, 1, 5555.6000),
                tuple(5555.5555 as Number, 2, 5555.5600),
                tuple(5555.5555 as Number, 3, 5555.5560),
                tuple(5555.5555 as Number, 4, 5555.5555),
                tuple(5555.5555 as Number, 5, 5555.55550),
                tuple(5555.5555 as Number, -1, 5560.0000),
                tuple(5555.5555 as Number, -2, 5600.0000),
                tuple(5555.5555 as Number, -3, 6000.0000),
                tuple(5555.5555 as Number, -4, 10000.0000),
                tuple(5555.5555 as Number, -5, 0000.0000),
            ).forEach { (input: Number, precision: Int, expected: Double) ->

                withClue("$input.roundWithPrecision($precision) should be $expected") {
                    input.roundWithPrecision(precision) shouldBe expected
                }

                withClue("$input.toDouble().roundWithPrecision($precision) should be $expected") {
                    input.toDouble().roundWithPrecision(precision) shouldBe expected
                }

                withClue("$input.toFloat().roundWithPrecision($precision) should be $expected") {
                    input.toFloat().roundWithPrecision(precision) shouldBe expected.toFloat()
                }
            }
        }
    }
}
