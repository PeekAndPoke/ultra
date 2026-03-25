package de.peekandpoke.ultra.common.maths.stochastic

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class DistributionExtensionsSpec : StringSpec() {

    init {

        "sample(from, to) - maps 0..1 sample to from..to range" {
            val dist = BucketedDistribution.createUniform()
            val rng = Random(42)
            val n = 10_000
            val from = 10.0
            val to = 20.0

            val samples = (1..n).map { dist.sample(rng, from, to) }

            assertSoftly {
                samples.forEach { s ->
                    s shouldBeGreaterThanOrEqual from
                    s shouldBeLessThanOrEqual to
                }
            }

            // Mean should be roughly the midpoint
            val mean = samples.average()
            withClue("Mean=$mean should be near midpoint=${(from + to) / 2}") {
                mean shouldBe ((from + to) / 2 plusOrMinus 0.5)
            }
        }

        "sample(from, to) - from > to inverts range correctly" {
            val dist = BucketedDistribution.createUniform()
            val rng = Random(42)
            val n = 10_000
            val from = 20.0
            val to = 10.0

            val samples = (1..n).map { dist.sample(rng, from, to) }

            assertSoftly {
                samples.forEach { s ->
                    // With from > to, values go from 20 down to 10
                    s shouldBeGreaterThanOrEqual to
                    s shouldBeLessThanOrEqual from
                }
            }
        }

        "sample(from, to) - from == to always returns that value" {
            val dist = BucketedDistribution.createUniform()
            val rng = Random(42)
            val value = 42.0

            val samples = (1..100).map { dist.sample(rng, value, value) }

            assertSoftly {
                samples.forEach { it shouldBe value }
            }
        }
    }
}
