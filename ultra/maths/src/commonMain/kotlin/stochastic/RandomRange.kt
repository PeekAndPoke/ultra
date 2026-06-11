package io.peekandpoke.ultra.maths.stochastic

import kotlinx.serialization.SerialName

/**
 * Represents a range from which random values can be drawn.
 *
 * A [RandomRange] is either backed by a [Distribution] with explicit min/max bounds,
 * or is a constant that always yields the same value.
 */
sealed interface RandomRange {

    /**
     * A random range backed by a [Distribution] that samples values between [min] and [max].
     *
     * @property distribution the underlying distribution used for sampling.
     * @property min          the lower bound of the range.
     * @property max          the upper bound of the range.
     */
    @SerialName("of-distribution")
    data class OfDistribution(
        val distribution: Distribution,
        override val min: Double,
        override val max: Double,
    ) : RandomRange

    /**
     * A degenerate random range that always yields the same [constant] value.
     *
     * Both [min] and [max] are equal to [constant].
     *
     * @property constant the fixed value this range always produces.
     */
    @SerialName("of-constant")
    data class OfConstant(
        val constant: Double,
    ) : RandomRange {
        override val min = constant
        override val max = constant
    }

    /** The lower bound of this range. */
    val min: Double

    /** The upper bound of this range. */
    val max: Double
}
