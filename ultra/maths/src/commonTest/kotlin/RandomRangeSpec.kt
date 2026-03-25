package de.peekandpoke.ultra.maths.stochastic

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RandomRangeSpec : StringSpec() {

    init {

        "OfDistribution - min and max are stored correctly" {
            val dist = BucketedDistribution.createUniform()
            val range = RandomRange.OfDistribution(
                distribution = dist,
                min = 5.0,
                max = 15.0,
            )

            range.min shouldBe 5.0
            range.max shouldBe 15.0
            range.distribution shouldBe dist
        }

        "OfConstant - min and max equal the constant" {
            val range = RandomRange.OfConstant(constant = 42.0)

            range.min shouldBe 42.0
            range.max shouldBe 42.0
            range.constant shouldBe 42.0
        }

        "OfConstant - negative constant works" {
            val range = RandomRange.OfConstant(constant = -7.5)

            range.min shouldBe -7.5
            range.max shouldBe -7.5
        }

        "OfConstant - zero constant works" {
            val range = RandomRange.OfConstant(constant = 0.0)

            range.min shouldBe 0.0
            range.max shouldBe 0.0
        }
    }
}
