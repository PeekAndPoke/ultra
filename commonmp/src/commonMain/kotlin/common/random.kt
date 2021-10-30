package de.peekandpoke.ultra.common

import kotlin.jvm.JvmName
import kotlin.random.Random

fun Random.nextBin(vararg weights: Number) = nextBin(weights.map { it.toDouble() }.toDoubleArray())

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

@JvmName("nextBin_list")
fun <T> Random.nextBin(weightsToValue: List<Pair<Double, T>>): T {
    return nextBin(weightsToValue.toTypedArray())
}

@JvmName("nextBin_vararg")
fun <T> Random.nextBin(vararg weightToValue: Pair<Double, T>): T {
    return nextBin(weightToValue)
}

@JvmName("nextBin_array")
fun <T> Random.nextBin(weightsToValue: Array<out Pair<Double, T>>): T {

    val weights = weightsToValue.map { it.first }.toDoubleArray()
    val bin = nextBin(weights)

    return weightsToValue[bin].second
}
