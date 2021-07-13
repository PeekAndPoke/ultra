package de.peekandpoke.ultra.common

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe

class NumbersSpec : FreeSpec() {

    init {

        "Number.toFixed(decimals)" {
            table(
                headers("Input", "Decimals", "Expected"),
                row(0, 0, "0"),
                row(0, 1, "0.0"),
                row(1, 0, "1"),
                row(-1, 0, "-1"),
                row(11, 0, "11"),
                row(-11, 0, "-11"),
                row(11.9, 2, "11.90"),
                row(-11.9, 3, "-11.900"),
                row(1.1, 1, "1.1"),
                row(1.1, 2, "1.10"),
                row(1.11, 2, "1.11"),
                row(1.11, 3, "1.110"),
                row(1.11f, 3, "1.110"),
                row(1.111, 3, "1.111"),
                row(1.111f, 3, "1.111"),
                row(1.1111f, 3, "1.111"),
                row(1.11111f, 3, "1.111"),
                row(21.1, 1, "21.1"),
                row(21.1, 2, "21.10"),
                row(21.11, 2, "21.11"),
                row(21.11, 3, "21.110"),
                row(21.11f, 3, "21.110"),
                row(21.111, 3, "21.111"),
                row(21.111f, 3, "21.111"),
                row(21.1111f, 3, "21.111"),
                row(21.11111f, 3, "21.111"),
                row(99.9, 0, "99"),
                row(99.9, 1, "99.9"),
                row(99.9, 2, "99.90"),
                row(99.99, 2, "99.99"),
                row(99.99, 3, "99.990"),
                row(99.99f, 3, "99.990"),
                row(99.999, 3, "99.999"),
                row(99.999f, 3, "99.999"),
                row(99.9999f, 3, "99.999"),
                row(99.99999f, 3, "99.999"),
                row(-1.1, 1, "-1.1"),
                row(-1.1, 2, "-1.10"),
                row(-9.9, 1, "-9.9"),
                row(-9.9, 2, "-9.90"),
                row(-9.99, 2, "-9.99"),
                row(-99.99, 3, "-99.990"),
                row(-99.999f, 3, "-99.999"),
                row(-99.9999f, 3, "-99.999"),
                row(-99.99999f, 3, "-99.999"),
            ).forAll { input: Number, decimals: Int, expected: String ->
                input.toFixed(decimals) shouldBe expected
            }
        }

        "roundWithPrecision(precisions)" {

            table(
                headers("Input", "Precision", "Expected"),
                row(0 as Number, 0, 0.0),
                row(0.0 as Number, 0, 0.0),
                row(0.0f as Number, 0, 0.0),
                row(1.0f as Number, 0, 1.0),
                // rounding down
                row(1111.1111f as Number, 5, 1111.11108),
                row(1111.1111f as Number, 4, 1111.1111),
                row(1111.1111f as Number, 3, 1111.1110),
                row(1111.1111f as Number, 2, 1111.1100),
                row(1111.1111f as Number, 1, 1111.1000),
                row(1111.1111f as Number, 0, 1111.0000),
                row(1111.1111f as Number, -1, 1110.0000),
                row(1111.1111f as Number, -2, 1100.0000),
                row(1111.1111f as Number, -3, 1000.0000),
                row(1111.1111f as Number, -4, 0000.0000),
                row(1111.1111f as Number, -5, 0000.0000),
                // rounding up
                row(5555.5555f as Number, 0, 5556.0000),
                row(5555.5555f as Number, 1, 5555.6000),
                row(5555.5555f as Number, 2, 5555.5600),
                row(5555.5555f as Number, 3, 5555.5560),
                row(5555.5555f as Number, 4, 5555.5557),
                row(5555.5555f as Number, 5, 5555.55566),
                row(5555.5555f as Number, -1, 5560.0000),
                row(5555.5555f as Number, -2, 5600.0000),
                row(5555.5555f as Number, -3, 6000.0000),
                row(5555.5555f as Number, -4, 10000.0000),
                row(5555.5555f as Number, -5, 0000.0000),
            ).forAll { input: Number, precision: Int, expected: Double ->

                withClue("Number.roundWithPrecision") {
                    input.roundWithPrecision(precision) shouldBe expected
                }

                withClue("Double.roundWithPrecision") {
                    input.toDouble().roundWithPrecision(precision) shouldBe expected
                }

                withClue("Float.roundWithPrecision") {
                    input.toFloat().roundWithPrecision(precision) shouldBe expected.toFloat()
                }
            }
        }
    }
}
