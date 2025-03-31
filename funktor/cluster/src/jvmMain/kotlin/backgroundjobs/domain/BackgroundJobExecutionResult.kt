package de.peekandpoke.funktor.cluster.backgroundjobs.domain

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName

sealed class BackgroundJobExecutionResult {

    abstract val executedAt: MpInstant
    abstract val executionTimeMs: Long
    abstract val serverId: String

    @SerialName("success")
    data class Success(
        val data: Map<String, Any?>,
        override val serverId: String,
        override val executionTimeMs: Long = -1,
        override val executedAt: MpInstant = MpInstant.now(),
    ) : BackgroundJobExecutionResult()

    @SerialName("failed")
    data class Failed(
        val data: Map<String, Any?>,
        override val serverId: String,
        override val executionTimeMs: Long = -1,
        override val executedAt: MpInstant = MpInstant.now(),
    ) : BackgroundJobExecutionResult()
}
