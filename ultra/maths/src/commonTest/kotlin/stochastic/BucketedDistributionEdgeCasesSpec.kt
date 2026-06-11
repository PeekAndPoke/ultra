package io.peekandpoke.ultra.maths.stochastic

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class BucketedDistributionEdgeCasesSpec : StringSpec() {

    init {

        // -------------------------------------------------------------------
        // Minimum bucket count (count = 1)
        // -------------------------------------------------------------------

        "createUniform with count=1 produces a single bucket" {
            val dist = BucketedDistribution.createUniform(count = 1)

            dist.numBuckets shouldBe 1
            dist.xs.size shouldBe 2
            dist.pdf.size shouldBe 2
            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTruncatedNormal with count=1 produces a valid distribution" {
            val dist = BucketedDistribution.createTruncatedNormal(count = 1)

            dist.numBuckets shouldBe 1
            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createExponential with count=1 produces a valid distribution" {
            val dist = BucketedDistribution.createExponential(count = 1)

            dist.numBuckets shouldBe 1
            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTriangular with count=1 throws due to non-positive total area" {
            shouldThrow<IllegalStateException> {
                BucketedDistribution.createTriangular(min = 0.0, mode = 0.5, max = 1.0, count = 1)
            }
        }

        // -------------------------------------------------------------------
        // createExponential edge cases
        // -------------------------------------------------------------------

        "createExponential - count < 1 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createExponential(count = 0)
            }
        }

        "createExponential - tailCut out of range throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createExponential(tailCut = 0.5) // too large
            }
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createExponential(tailCut = 1e-15) // too small
            }
        }

        // -------------------------------------------------------------------
        // createTriangular edge cases
        // -------------------------------------------------------------------

        "createTriangular - count < 1 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTriangular(min = 0.0, mode = 0.5, max = 1.0, count = 0)
            }
        }

        "createTriangular - mode at min boundary" {
            val dist = BucketedDistribution.createTriangular(min = 0.0, mode = 0.0, max = 1.0, count = 50)

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
            dist.pdf.first() shouldBe 1.0 // peak at min = mode
        }

        "createTriangular - mode at max boundary" {
            val dist = BucketedDistribution.createTriangular(min = 0.0, mode = 1.0, max = 1.0, count = 50)

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
            dist.pdf.last() shouldBe 1.0 // peak at max = mode
        }

        // -------------------------------------------------------------------
        // createTruncatedNormal edge cases
        // -------------------------------------------------------------------

        "createTruncatedNormal - count < 1 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTruncatedNormal(count = 0)
            }
        }

        "createTruncatedNormal - mean at boundary 0.0" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.0, std = 0.1, count = 50)

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTruncatedNormal - mean at boundary 1.0" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 1.0, std = 0.1, count = 50)

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTruncatedNormal - very small std concentrates samples" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.5, std = 0.01, count = 200)
            val rng = Random(42)
            val n = 10_000

            val samples = (1..n).map { dist.sample(rng) }
            val mean = samples.average()

            mean shouldBe (0.5 plusOrMinus 0.05)

            // Most samples should be near 0.5
            val nearCenter = samples.count { it in 0.4..0.6 }
            (nearCenter.toDouble() / n) shouldBeGreaterThanOrEqual 0.9
        }

        // -------------------------------------------------------------------
        // sample() determinism and edge cases
        // -------------------------------------------------------------------

        "sample with same seed on different distribution types gives deterministic results" {
            val types = listOf(
                BucketedDistribution.createUniform(),
                BucketedDistribution.createTruncatedNormal(),
                BucketedDistribution.createExponential(),
                BucketedDistribution.createTriangular(min = 0.0, mode = 0.5, max = 1.0),
            )

            types.forEach { dist ->
                val s1 = (1..50).map { dist.sample(Random(99)) }
                val s2 = (1..50).map { dist.sample(Random(99)) }
                s1 shouldBe s2
            }
        }

        // -------------------------------------------------------------------
        // inverse edge cases
        // -------------------------------------------------------------------

        "inverse of inverse restores original pdf values" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.3, count = 20)
            val doubleInverse = dist.inverse().inverse()

            assertSoftly {
                for (i in dist.pdf.indices) {
                    withClue("pdf[$i]") {
                        doubleInverse.pdf[i] shouldBe (dist.pdf[i] plusOrMinus 1e-10)
                    }
                }
            }
        }

        "inverse preserves xs" {
            val dist = BucketedDistribution.createExponential(count = 10)
            val inv = dist.inverse()

            inv.xs shouldBe dist.xs
        }

        // -------------------------------------------------------------------
        // blend edge cases
        // -------------------------------------------------------------------

        "blend at ratio 0.5 averages xs and pdf" {
            val a = BucketedDistribution.createUniform(count = 5)
            val b = BucketedDistribution.createTruncatedNormal(count = 5)
            val blended = a.blend(b, ratio = 0.5)

            assertSoftly {
                for (i in a.pdf.indices) {
                    val expected = (a.pdf[i] + b.pdf[i]) / 2.0
                    withClue("blended pdf[$i]") {
                        blended.pdf[i] shouldBe (expected plusOrMinus 1e-10)
                    }
                }
                for (i in a.xs.indices) {
                    val expected = (a.xs[i] + b.xs[i]) / 2.0
                    withClue("blended xs[$i]") {
                        blended.xs[i] shouldBe (expected plusOrMinus 1e-10)
                    }
                }
            }
        }

        "blend result has valid CDF" {
            val a = BucketedDistribution.createExponential(count = 20)
            val b = BucketedDistribution.createTruncatedNormal(count = 20)
            val blended = a.blend(b, ratio = 0.3)

            blended.cdf.first() shouldBe 0.0
            blended.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)

            for (i in 1 until blended.cdf.size) {
                blended.cdf[i] shouldBeGreaterThanOrEqual blended.cdf[i - 1]
            }
        }

        // -------------------------------------------------------------------
        // reversed then sampled stays in range
        // -------------------------------------------------------------------

        "reversed distribution samples stay in xs range" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.3, count = 50).reversed()
            val rng = Random(42)

            val samples = (1..5000).map { dist.sample(rng) }

            assertSoftly {
                samples.forEach { s ->
                    s shouldBeGreaterThanOrEqual 0.0
                    s shouldBeLessThanOrEqual 1.0
                }
            }
        }

        // -------------------------------------------------------------------
        // DEFAULT_SIZE constant
        // -------------------------------------------------------------------

        "DEFAULT_SIZE is 100" {
            BucketedDistribution.DEFAULT_SIZE shouldBe 100
        }
    }
}
