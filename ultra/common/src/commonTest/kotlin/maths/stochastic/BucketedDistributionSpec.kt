package de.peekandpoke.ultra.common.maths.stochastic

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class BucketedDistributionSpec : StringSpec() {

    init {

        // ---------------------------------------------------------------
        // Construction & validation
        // ---------------------------------------------------------------

        "Creating with mismatched xs and pdf sizes should throw" {
            shouldThrow<IllegalStateException> {
                BucketedDistribution(
                    xs = listOf(0.0, 0.5, 1.0),
                    pdf = listOf(1.0, 1.0),
                )
            }
        }

        "Creating with empty xs should throw" {
            shouldThrow<IllegalStateException> {
                BucketedDistribution(
                    xs = emptyList(),
                    pdf = emptyList(),
                )
            }
        }

        "CDF should start at 0 and end at 1" {
            val dist = BucketedDistribution.createUniform()

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "CDF should be monotonically non-decreasing" {
            val dist = BucketedDistribution.createTruncatedNormal()

            for (i in 1 until dist.cdf.size) {
                withClue("cdf[$i] should be >= cdf[${i - 1}]") {
                    dist.cdf[i] shouldBeGreaterThanOrEqual dist.cdf[i - 1]
                }
            }
        }

        // ---------------------------------------------------------------
        // createUniform
        // ---------------------------------------------------------------

        "createUniform - default count produces correct bucket count" {
            val dist = BucketedDistribution.createUniform()
            dist.numBuckets shouldBe BucketedDistribution.DEFAULT_SIZE
        }

        "createUniform - custom count works" {
            val dist = BucketedDistribution.createUniform(count = 10)
            dist.numBuckets shouldBe 10
        }

        "createUniform - count < 1 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createUniform(count = 0)
            }
        }

        "createUniform - all pdf values are 1.0" {
            val dist = BucketedDistribution.createUniform(count = 10)

            dist.pdf.forEach { it shouldBe 1.0 }
        }

        "createUniform - CDF is linear" {
            val count = 10
            val dist = BucketedDistribution.createUniform(count = count)

            assertSoftly {
                for (i in 0..count) {
                    val expected = i.toDouble() / count
                    withClue("cdf[$i] should be $expected") {
                        dist.cdf[i] shouldBe (expected plusOrMinus 1e-10)
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // createTruncatedNormal
        // ---------------------------------------------------------------

        "createTruncatedNormal - default params produce valid distribution" {
            val dist = BucketedDistribution.createTruncatedNormal()

            dist.numBuckets shouldBe BucketedDistribution.DEFAULT_SIZE
            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTruncatedNormal - mean outside 0..1 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTruncatedNormal(mean = 1.5)
            }
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTruncatedNormal(mean = -0.1)
            }
        }

        "createTruncatedNormal - std <= 0 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTruncatedNormal(std = 0.0)
            }
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTruncatedNormal(std = -0.1)
            }
        }

        "createTruncatedNormal - PDF peaks near the mean" {
            val mean = 0.5
            val dist = BucketedDistribution.createTruncatedNormal(mean = mean, count = 100)

            val maxPdfIdx = dist.pdf.indices.maxByOrNull { dist.pdf[it] }!!
            val peakX = dist.xs[maxPdfIdx]

            withClue("Peak x=$peakX should be near mean=$mean") {
                peakX shouldBe (mean plusOrMinus 0.02)
            }
        }

        "createTruncatedNormal - PDF is symmetric when mean = 0.5" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.5, std = 0.15, count = 100)

            assertSoftly {
                for (i in 0..50) {
                    val mirror = 100 - i
                    withClue("pdf[$i] should equal pdf[$mirror]") {
                        dist.pdf[i] shouldBe (dist.pdf[mirror] plusOrMinus 1e-10)
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // createExponential
        // ---------------------------------------------------------------

        "createExponential - default params produce valid distribution" {
            val dist = BucketedDistribution.createExponential()

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createExponential - lambda <= 0 throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createExponential(lambda = 0.0)
            }
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createExponential(lambda = -1.0)
            }
        }

        "createExponential - PDF is monotonically decreasing" {
            val dist = BucketedDistribution.createExponential(lambda = 2.0, count = 50)

            for (i in 1 until dist.pdf.size) {
                withClue("pdf[$i] should be <= pdf[${i - 1}]") {
                    dist.pdf[i] shouldBeLessThanOrEqual dist.pdf[i - 1]
                }
            }
        }

        "createExponential - higher lambda concentrates samples closer to 0" {
            val n = 50_000

            val lowLambda = BucketedDistribution.createExponential(lambda = 1.0)
            val highLambda = BucketedDistribution.createExponential(lambda = 5.0)

            val meanLow = (1..n).map { lowLambda.sample(Random(42)) }.average()
            val meanHigh = (1..n).map { highLambda.sample(Random(42)) }.average()

            withClue("Higher lambda should produce lower mean (meanLow=$meanLow, meanHigh=$meanHigh)") {
                meanHigh shouldBeLessThanOrEqual meanLow
            }
        }

        // ---------------------------------------------------------------
        // createTriangular
        // ---------------------------------------------------------------

        "createTriangular - valid params produce correct distribution" {
            val dist = BucketedDistribution.createTriangular(min = 0.0, mode = 0.5, max = 1.0)

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)
        }

        "createTriangular - max <= min throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTriangular(min = 1.0, mode = 0.5, max = 0.0)
            }
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTriangular(min = 1.0, mode = 1.0, max = 1.0)
            }
        }

        "createTriangular - mode outside min..max throws" {
            shouldThrow<IllegalArgumentException> {
                BucketedDistribution.createTriangular(min = 0.0, mode = 1.5, max = 1.0)
            }
        }

        "createTriangular - PDF peaks at mode" {
            val mode = 0.7
            val dist = BucketedDistribution.createTriangular(min = 0.0, mode = mode, max = 1.0, count = 100)

            val maxPdfIdx = dist.pdf.indices.maxByOrNull { dist.pdf[it] }!!
            val peakX = dist.xs[maxPdfIdx]

            withClue("Peak x=$peakX should be at mode=$mode") {
                peakX shouldBe (mode plusOrMinus 0.02)
            }
        }

        "createTriangular - PDF is 0 at min and max" {
            val dist = BucketedDistribution.createTriangular(min = 0.0, mode = 0.5, max = 1.0, count = 100)

            dist.pdf.first() shouldBe 0.0
            dist.pdf.last() shouldBe 0.0
        }

        // ---------------------------------------------------------------
        // sample()
        // ---------------------------------------------------------------

        "sample - with seeded Random, results are deterministic" {
            val dist = BucketedDistribution.createUniform()

            val samples1 = (1..100).map { dist.sample(Random(123)) }
            val samples2 = (1..100).map { dist.sample(Random(123)) }

            samples1 shouldBe samples2
        }

        "sample - uniform distribution produces roughly uniform samples" {
            val dist = BucketedDistribution.createUniform()
            val rng = Random(42)
            val n = 50_000

            val samples = (1..n).map { dist.sample(rng) }

            // Split into 10 bins and check each has roughly 10% of samples
            val bins = IntArray(10)
            samples.forEach { s ->
                val bin = (s * 10).toInt().coerceIn(0, 9)
                bins[bin]++
            }

            assertSoftly {
                val expected = n / 10.0
                for (i in bins.indices) {
                    withClue("bin[$i] count=${bins[i]} should be near $expected") {
                        bins[i].toDouble() shouldBe (expected plusOrMinus expected * 0.15)
                    }
                }
            }
        }

        "sample - normal distribution mean of samples approximates configured mean" {
            val configuredMean = 0.3
            val dist = BucketedDistribution.createTruncatedNormal(mean = configuredMean, std = 0.1)
            val rng = Random(42)
            val n = 50_000

            val sampleMean = (1..n).map { dist.sample(rng) }.average()

            withClue("Sample mean=$sampleMean should approximate configured mean=$configuredMean") {
                sampleMean shouldBe (configuredMean plusOrMinus 0.02)
            }
        }

        "sample - exponential distribution samples are in 0..1 range" {
            val dist = BucketedDistribution.createExponential()
            val rng = Random(42)

            val samples = (1..10_000).map { dist.sample(rng) }

            assertSoftly {
                samples.forEach { s ->
                    s shouldBeGreaterThanOrEqual 0.0
                    s shouldBeLessThanOrEqual 1.0
                }
            }
        }

        "sample - triangular distribution samples are in min..max range" {
            val min = 2.0
            val max = 8.0
            val dist = BucketedDistribution.createTriangular(min = min, mode = 5.0, max = max)
            val rng = Random(42)

            val samples = (1..10_000).map { dist.sample(rng) }

            assertSoftly {
                samples.forEach { s ->
                    s shouldBeGreaterThanOrEqual min
                    s shouldBeLessThanOrEqual max
                }
            }
        }

        // ---------------------------------------------------------------
        // inverse()
        // ---------------------------------------------------------------

        "inverse - of uniform is still uniform" {
            val dist = BucketedDistribution.createUniform(count = 10)
            val inv = dist.inverse()

            assertSoftly {
                // All pdf values should be equal (pMax + pMin - 1.0 = 1.0 for uniform)
                val first = inv.pdf.first()
                inv.pdf.forEach { it shouldBe (first plusOrMinus 1e-10) }
            }
        }

        "inverse - PDF values are mirrored correctly" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.5, count = 10)
            val inv = dist.inverse()

            val pMax = dist.pdf.max()
            val pMin = dist.pdf.min()

            assertSoftly {
                for (i in dist.pdf.indices) {
                    withClue("inverse pdf[$i]") {
                        inv.pdf[i] shouldBe ((pMax + pMin - dist.pdf[i]) plusOrMinus 1e-10)
                    }
                }
            }
        }

        "inverse - CDF is valid" {
            val dist = BucketedDistribution.createTruncatedNormal().inverse()

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)

            for (i in 1 until dist.cdf.size) {
                dist.cdf[i] shouldBeGreaterThanOrEqual dist.cdf[i - 1]
            }
        }

        // ---------------------------------------------------------------
        // reversed()
        // ---------------------------------------------------------------

        "reversed - PDF is reversed while xs stays the same" {
            val dist = BucketedDistribution.createTruncatedNormal(mean = 0.3, count = 10)
            val rev = dist.reversed()

            assertSoftly {
                // xs unchanged
                dist.xs shouldBe rev.xs

                // pdf reversed
                for (i in dist.pdf.indices) {
                    rev.pdf[i] shouldBe dist.pdf[dist.pdf.size - 1 - i]
                }
            }
        }

        "reversed - reversing twice gives back the original pdf" {
            val dist = BucketedDistribution.createExponential(count = 20)
            val doubleReversed = dist.reversed().reversed()

            assertSoftly {
                for (i in dist.pdf.indices) {
                    doubleReversed.pdf[i] shouldBe (dist.pdf[i] plusOrMinus 1e-10)
                }
            }
        }

        "reversed - CDF is valid" {
            val dist = BucketedDistribution.createExponential().reversed()

            dist.cdf.first() shouldBe 0.0
            dist.cdf.last() shouldBe (1.0 plusOrMinus 1e-10)

            for (i in 1 until dist.cdf.size) {
                dist.cdf[i] shouldBeGreaterThanOrEqual dist.cdf[i - 1]
            }
        }

        // ---------------------------------------------------------------
        // blend()
        // ---------------------------------------------------------------

        "blend - two identical distributions returns the same distribution" {
            val dist = BucketedDistribution.createUniform(count = 10)
            val blended = dist.blend(dist, ratio = 0.5)

            assertSoftly {
                for (i in dist.pdf.indices) {
                    blended.pdf[i] shouldBe (dist.pdf[i] plusOrMinus 1e-10)
                }
                for (i in dist.xs.indices) {
                    blended.xs[i] shouldBe (dist.xs[i] plusOrMinus 1e-10)
                }
            }
        }

        "blend - ratio 0.0 returns this distribution" {
            val a = BucketedDistribution.createUniform(count = 10)
            val b = BucketedDistribution.createTruncatedNormal(count = 10)
            val blended = a.blend(b, ratio = 0.0)

            assertSoftly {
                for (i in a.pdf.indices) {
                    blended.pdf[i] shouldBe (a.pdf[i] plusOrMinus 1e-10)
                }
            }
        }

        "blend - ratio 1.0 returns other distribution" {
            val a = BucketedDistribution.createUniform(count = 10)
            val b = BucketedDistribution.createTruncatedNormal(count = 10)
            val blended = a.blend(b, ratio = 1.0)

            assertSoftly {
                for (i in b.pdf.indices) {
                    blended.pdf[i] shouldBe (b.pdf[i] plusOrMinus 1e-10)
                }
            }
        }

        "blend - mismatched bucket count throws" {
            val a = BucketedDistribution.createUniform(count = 10)
            val b = BucketedDistribution.createUniform(count = 20)

            shouldThrow<IllegalArgumentException> {
                a.blend(b)
            }
        }

        "blend - ratio is clamped to 0..1" {
            val a = BucketedDistribution.createUniform(count = 10)
            val b = BucketedDistribution.createTruncatedNormal(count = 10)

            // ratio = -1.0 should behave like 0.0
            val blendedNeg = a.blend(b, ratio = -1.0)
            // ratio = 2.0 should behave like 1.0
            val blendedOver = a.blend(b, ratio = 2.0)

            assertSoftly {
                for (i in a.pdf.indices) {
                    blendedNeg.pdf[i] shouldBe (a.pdf[i] plusOrMinus 1e-10)
                    blendedOver.pdf[i] shouldBe (b.pdf[i] plusOrMinus 1e-10)
                }
            }
        }
    }
}
