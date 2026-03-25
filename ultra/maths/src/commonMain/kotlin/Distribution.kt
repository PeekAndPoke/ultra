package de.peekandpoke.ultra.maths.stochastic

import kotlin.random.Random

sealed interface Distribution {
    fun sample(random: Random): Double
}
