package de.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.doubles.percent
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class RandomSpec : FreeSpec() {

    init {

        "nextBin() - double array" - {

            "Empty weights should throw" {

                shouldThrow<IllegalArgumentException> {
                    Random(1).nextBin()
                }

                shouldThrow<IllegalArgumentException> {
                    Random(1).nextBin(doubleArrayOf())
                }
            }

            "Single weight should return 0" {
                Random(1).nextBin(0.1) shouldBe 0
                Random(1).nextBin(1) shouldBe 0
            }

            "Multiple weights should return correctly" {
                val random = Random(1)

                val bins = mutableMapOf(
                    0 to 0,
                    1 to 0,
                    2 to 0,
                    3 to 0,
                    4 to 0,
                )

                val weights = doubleArrayOf(1.0, 3.0, 2.0, 0.0, 5.0)
                val weightsSum = weights.sum()
                val rounds = 100000

                repeat(rounds) {
                    val bin = random.nextBin(weights)
                    bins[bin] = bins[bin]!! + 1
                }

                println(bins)

                val roundsDivSum = rounds / weightsSum

                withClue("Bin 0") {
                    bins[0]!!.toDouble() shouldBe (roundsDivSum * weights[0]).plusOrMinus(1.percent)
                }

                withClue("Bin 1") {
                    bins[1]!!.toDouble() shouldBe (roundsDivSum * weights[1]).plusOrMinus(1.percent)
                }

                withClue("Bin 2") {
                    bins[2]!!.toDouble() shouldBe (roundsDivSum * weights[2]).plusOrMinus(1.percent)
                }

                withClue("Bin 3") {
                    bins[3]!!.toDouble() shouldBe (roundsDivSum * weights[3]).plusOrMinus(1.percent)
                }

                withClue("Bin 4") {
                    bins[4]!!.toDouble() shouldBe (roundsDivSum * weights[4]).plusOrMinus(1.percent)
                }
            }
        }

        "nextBin() - pairs of weight to value" - {

            "Multiple weights should return correctly" {
                val random = Random(1)

                data class V(val v: Int)

                val v1 = V(1)
                val v2 = V(2)
                val v3 = V(3)
                val v4 = V(4)
                val v5 = V(5)

                val bins = mutableMapOf(
                    v1 to 0,
                    v2 to 0,
                    v3 to 0,
                    v4 to 0,
                    v5 to 0,
                )

                val weights = listOf(1.0 to v1, 3.0 to v2, 2.0 to v3, 0.0 to v4, 5.0 to v5)
                val weightsSum = weights.sumOf { it.first }
                val rounds = 100000

                repeat(rounds) {
                    val bin = random.nextBin(weights)
                    bins[bin] = bins[bin]!! + 1
                }

                println(bins)

                val roundsDivSum = rounds / weightsSum

                withClue("Bin v1") {
                    bins[v1]!!.toDouble() shouldBe (roundsDivSum * weights[0].first).plusOrMinus(1.percent)
                }

                withClue("Bin v2") {
                    bins[v2]!!.toDouble() shouldBe (roundsDivSum * weights[1].first).plusOrMinus(1.percent)
                }

                withClue("Bin v3") {
                    bins[v3]!!.toDouble() shouldBe (roundsDivSum * weights[2].first).plusOrMinus(1.percent)
                }

                withClue("Bin v4") {
                    bins[v4]!!.toDouble() shouldBe (roundsDivSum * weights[3].first).plusOrMinus(1.percent)
                }

                withClue("Bin v5") {
                    bins[v5]!!.toDouble() shouldBe (roundsDivSum * weights[4].first).plusOrMinus(1.percent)
                }
            }
        }
    }
}
