package io.peekandpoke.ultra.maths.stochastic

import kotlin.random.Random

fun Distribution.sample(random: Random, from: Double, to: Double): Double {
    val next = sample(random)

    return next * (to - from) + from
}
