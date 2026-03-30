package io.peekandpoke.ultra.maths.stochastic

import kotlin.random.Random

/**
 * Draws a sample from this distribution and maps it to the range [from] to [to].
 *
 * The raw sample (in [0, 1]) is linearly scaled so that 0.0 maps to [from]
 * and 1.0 maps to [to]. When [from] > [to], the mapping is inverted.
 *
 * @param random the random number generator to use for sampling.
 * @param from   the value corresponding to a raw sample of 0.0.
 * @param to     the value corresponding to a raw sample of 1.0.
 * @return a scaled sample in the range [from, to] (or [to, from] if [from] > [to]).
 */
fun Distribution.sample(random: Random, from: Double, to: Double): Double {
    val next = sample(random)

    return next * (to - from) + from
}
