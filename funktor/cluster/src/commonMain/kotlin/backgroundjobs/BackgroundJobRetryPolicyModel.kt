package de.peekandpoke.funktor.cluster.backgroundjobs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BackgroundJobRetryPolicyModel {

    @Serializable
    @SerialName("none")
    object None : BackgroundJobRetryPolicyModel()

    @Serializable
    @SerialName("linear-delay")
    data class LinearDelay(
        val delayInMs: Long,
        val maxTries: Int,
    ) : BackgroundJobRetryPolicyModel()
}
