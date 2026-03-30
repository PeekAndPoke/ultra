package io.peekandpoke.ultra.maths.stochastic

import kotlinx.serialization.SerialName
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/**
 * A discrete approximation of a continuous probability distribution using buckets.
 *
 * The distribution is defined by a sorted grid of x-values and corresponding probability
 * density values. Internally, a CDF is computed via the trapezoid rule, and sampling uses
 * inverse-CDF interpolation.
 *
 * @property xs  sorted list of x-values defining bucket boundaries (size = N+1).
 * @property pdf probability density values at each x-value (same size as [xs]).
 *
 * @see Distribution
 */
@SerialName("bucketed")
data class BucketedDistribution(val xs: List<Double>, val pdf: List<Double>) : Distribution {

    private val _xs: DoubleArray = xs.toDoubleArray()
    private val _pdf: DoubleArray = pdf.toDoubleArray()

    companion object {
        const val DEFAULT_SIZE = 100

        /**
         * Uniform distribution on [0,1]
         */
        fun createUniform(count: Int = DEFAULT_SIZE): BucketedDistribution {
            require(count >= 1) { "count must be greater than 0" }

            val xs = List(count + 1) { i -> i.toDouble() / count }
            val pdf = List(count + 1) { i -> 1.0 }

            return BucketedDistribution(xs = xs, pdf = pdf)
        }

        /**
         * A normal curve truncated to [0,1], sampled via bucketed inverse‐CDF.
         */
        fun createTruncatedNormal(
            mean: Double = 0.5, // center inside [0,1]
            std: Double = 0.15, // standard deviation
            count: Int = DEFAULT_SIZE, // number of intervals
        ): BucketedDistribution {
            require(mean in 0.0..1.0) { "mean must lie in [0,1]" }
            require(std > 0.0) { "std must be positive" }
            require(count >= 1) { "count must be ≥ 1" }

            // 1) build xs on [0,1]
            val xs = List(count + 1) { i ->
                i.toDouble() / count
            }

            // 2) unnormalized Gaussian pdf at each grid point
            val pdf = List(count + 1) { i ->
                val x = xs[i]
                // exp(−½ * ((x − mean)/std)²)
                exp(-0.5 * ((x - mean) / std).pow(2))
            }

            // 3) normalize & build CDF inside the constructor
            return BucketedDistribution(xs = xs, pdf = pdf)
        }

        /**
         * Exponential distribution on [0, ∞), truncated at Xmax so the tail is negligible.
         *
         * @param lambda  rate parameter (λ > 0)
         * @param count   number of intervals (≥1)
         * @param tailCut tail probability beyond Xmax (e.g. 1e-3)
         */
        fun createExponential(
            lambda: Double = 1.0,
            count: Int = DEFAULT_SIZE,
            tailCut: Double = 1e-3,
        ): BucketedDistribution {
            require(lambda > 0.0) { "lambda must be >0" }
            require(count >= 1) { "count must be ≥1" }
            require(tailCut in 1e-12..1e-1) { "tailCut should be small" }

            // pick Xmax so P(X>Xmax)=tailCut
            val xMax = -kotlin.math.ln(tailCut) / lambda

            // build xs evenly on [0,1]
            val xs = List(count + 1) { i ->
                i.toDouble() / count
            }

            // use unnormalized exp(−λ⋅(x⋅Xmax)), which ∈ (tailCut,1]
            val pdf = List(count + 1) { i ->
                val x = xs[i] * xMax
                exp(-lambda * x)
            }

            return BucketedDistribution(xs = xs, pdf = pdf)
        }

        /**
         * Triangular distribution on [min, max] with peak at mode.
         *
         * @param min    lower bound
         * @param mode   peak location (in [min,max])
         * @param max    upper bound
         * @param count  number of intervals (≥1)
         */
        fun createTriangular(
            min: Double,
            mode: Double,
            max: Double,
            count: Int = DEFAULT_SIZE,
        ): BucketedDistribution {
            require(max > min) { "max must be > min" }
            require(mode in min..max) { "mode must be in [min,max]" }
            require(count >= 1) { "count must be ≥1" }

            // build xs on [min, max]
            val xs = List(count + 1) { i ->
                min + (max - min) * (i.toDouble() / count)
            }

            // pdf rises linearly to the mode, then falls linearly
            val pdf = List(count + 1) { i ->
                val x = xs[i]
                when {
                    x < mode -> (x - min) / (mode - min)
                    x > mode -> (max - x) / (max - mode)
                    else -> 1.0
                }
            }

            return BucketedDistribution(xs = xs, pdf = pdf)
        }
    }

    init {
        if (_xs.size != _pdf.size) {
            error("xs and cdf must have same size")
        }

        if (_xs.isEmpty()) {
            error("xs must have at least one element")
        }
    }

    /** The number of buckets (intervals) in this distribution, equal to `xs.size - 1`. */
    val numBuckets = _xs.size - 1

    /** The cumulative distribution function computed from [pdf] via the trapezoid rule. */
    val cdf = computeCdf(_xs, _pdf)

    /**
     * Draws a random sample from this distribution using inverse-CDF interpolation.
     *
     * @param random the random number generator to use.
     * @return a sampled value within the range defined by [xs].
     */
    override fun sample(random: Random): Double {
        // 1) Draw uniform [0,1) from the instance Random
        val u = random.nextDouble()

        // 2) Find the insertion-point in the CDF
        val hi = cdf.binarySearch(u)

        // Edge-cases
        if (hi <= 0) return _xs[0]
        if (hi >= _xs.size) return _xs.last()

        // 3) Interpolate between CDF[lo] and CDF[hi], not PDF
        val lo = hi - 1
        val x0 = _xs[lo]
        val x1 = _xs[hi]
        val f0 = cdf[lo]
        val f1 = cdf[hi]

        // linear interpolation on the inverse-CDF:
        //   x = x0 + (u − F0) * (x1 − x0) / (F1 − F0)
        return x0 + (u - f0) * (x1 - x0) / (f1 - f0)
    }

    /**
     * Creates the inverse of this distribution.
     *
     * The inverse mirrors the PDF so that high-probability regions become low-probability
     * and vice-versa. Specifically, `inversePdf[i] = (pMax + pMin) - pdf[i]`.
     *
     * @return a new [BucketedDistribution] with the inverted PDF.
     */
    fun inverse(): BucketedDistribution {
        val pMax = _pdf.max()
        val pMin = _pdf.min()

        // newPdf[i] = (pMax + pMin) − pdf[i]
        // ensures the new minimum is 0 and the new maximum is pMax − pMin,
        // so we never get a constant-zero array unless the original was degenerate.
        val invPdf = _pdf.map { p -> (pMax + pMin) - p }

        return BucketedDistribution(xs = xs, pdf = invPdf)
    }

    /**
     * Creates a reversed copy of this distribution.
     *
     * The x-values remain unchanged, but the PDF values are reversed so that the
     * distribution's shape is mirrored.
     *
     * @return a new [BucketedDistribution] with the reversed PDF.
     */
    fun reversed(): BucketedDistribution {
        return BucketedDistribution(xs = xs, pdf = pdf.reversed())
    }

    /**
     * Blends this distribution with [other] using the given [ratio].
     *
     * Both xs and pdf values are linearly interpolated:
     * - At `ratio = 0.0`, the result equals this distribution.
     * - At `ratio = 1.0`, the result equals [other].
     * - The ratio is clamped to [0, 1].
     *
     * @param other the distribution to blend with; must have the same [numBuckets].
     * @param ratio the blend weight toward [other], clamped to [0, 1]. Default is 0.5.
     * @return a new blended [BucketedDistribution].
     * @throws IllegalArgumentException if the two distributions have different bucket counts.
     */
    fun blend(other: BucketedDistribution, ratio: Double = 0.5): BucketedDistribution {
        // TODO: interpolate other, when size is different
        require(numBuckets == other.numBuckets) { "buckets must have same size" }

        val clampedRatio = ratio.coerceIn(0.0, 1.0)
        val oneMinusRatio = 1.0 - clampedRatio

        return BucketedDistribution(
            xs = _xs.mapIndexed { idx, it ->
                oneMinusRatio * it + clampedRatio * other._xs[idx]
            },
            pdf = _pdf.mapIndexed { idx, it ->
                oneMinusRatio * it + clampedRatio * other._pdf[idx]
            }
        )
    }

    /**
     * Find the insertion index for `u` in a sorted array `arr`.
     *
     * Returns the smallest i such that arr(i) >= u (i.e. the “hi” index).
     */
    private fun DoubleArray.binarySearch(u: Double): Int {
        val arr = this

        var lo = 0
        var hi = arr.size // search in [0 .. size]

        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (arr[mid] < u) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        return lo
    }

    /**
     * Compute the CDF via the trapezoid rule.
     */
    private fun computeCdf(xs: DoubleArray, pdf: DoubleArray): DoubleArray {
        val n = xs.size

        val raw = DoubleArray(n)

        raw[0] = 0.0

        for (i in 1 until n) {
            val dx = xs[i] - xs[i - 1]
            raw[i] = raw[i - 1] + 0.5 * (pdf[i - 1] + pdf[i]) * dx
        }
        val total = raw.last().takeIf { it > 0 } ?: error("Non-positive total area")

        return DoubleArray(n) { i -> raw[i] / total }
    }
}
