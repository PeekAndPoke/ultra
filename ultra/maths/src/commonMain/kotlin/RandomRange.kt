package io.peekandpoke.ultra.maths.stochastic

import kotlinx.serialization.SerialName

sealed interface RandomRange {

    @SerialName("of-distribution")
    data class OfDistribution(
        val distribution: Distribution,
        override val min: Double,
        override val max: Double,
    ) : RandomRange

    @SerialName("of-constant")
    data class OfConstant(
        val constant: Double,
    ) : RandomRange {
        override val min = constant
        override val max = constant
    }

    val min: Double
    val max: Double
}
