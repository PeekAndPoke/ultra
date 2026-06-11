package io.peekandpoke.ultra.maths.stochastic

import kotlin.random.Random

/**
 * A probability distribution that can produce random samples in the range [0, 1].
 *
 * Implementations define specific distribution shapes (uniform, normal, exponential, etc.)
 * and use inverse-CDF sampling to generate values.
 *
 * @see BucketedDistribution
 */
sealed interface Distribution {
    /**
     * Draws a single random sample from this distribution.
     *
     * @param random the random number generator to use for sampling.
     * @return a value typically in [0, 1], distributed according to this distribution's shape.
     */
    fun sample(random: Random): Double
}
