package de.peekandpoke.ultra.common.maths.stochastic

import kotlin.random.Random

sealed interface Distribution {
    fun sample(random: Random): Double
}
