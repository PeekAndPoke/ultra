package io.peekandpoke.ultra.common

import kotlin.jvm.JvmName
import kotlin.random.Random

/**
 * Returns a random bin index selected according to the given [weights].
 *
 * Higher weights increase the probability that the corresponding index is selected.
 *
 * @throws IllegalArgumentException if [weights] is empty.
 */
fun Random.nextBin(vararg weights: Number) = nextBin(weights.map { it.toDouble() }.toDoubleArray())

/**
 * Returns a random bin index selected according to the given [weights].
 *
 * Higher weights increase the probability that the corresponding index is selected.
 *
 * @throws IllegalArgumentException if [weights] is empty.
 */
fun Random.nextBin(weights: DoubleArray): Int {

    if (weights.isEmpty()) {
        throw IllegalArgumentException("Weights must not be empty")
    }

    val sum = weights.sum()
    val rand = nextDouble(sum)

    var idx = 0
    var acc = 0.0

    @Suppress("UseWithIndex")
    for (it in weights) {
        acc += it
        if (rand < acc) {
            return idx
        }
        ++idx
    }

    return weights.size - 1
}

/**
 * Returns a randomly selected value from [weightsToValue], where each pair maps a weight to a value.
 *
 * Higher weights increase the probability of the associated value being selected.
 */
@JvmName("nextBin_list")
fun <T> Random.nextBin(weightsToValue: List<Pair<Double, T>>): T {
    return nextBin(weightsToValue.toTypedArray())
}

/**
 * Returns a randomly selected value from [weightToValue], where each pair maps a weight to a value.
 *
 * Higher weights increase the probability of the associated value being selected.
 */
@JvmName("nextBin_vararg")
fun <T> Random.nextBin(vararg weightToValue: Pair<Double, T>): T {
    return nextBin(weightToValue)
}

/**
 * Returns a randomly selected value from [weightsToValue], where each pair maps a weight to a value.
 *
 * Higher weights increase the probability of the associated value being selected.
 */
@JvmName("nextBin_array")
fun <T> Random.nextBin(weightsToValue: Array<out Pair<Double, T>>): T {

    val weights = weightsToValue.map { it.first }.toDoubleArray()
    val bin = nextBin(weights)

    return weightsToValue[bin].second
}
