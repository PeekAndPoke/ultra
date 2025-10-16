package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.funktor.core.model.CpuProfile
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BackgroundJobResultModel {

    @Serializable
    @SerialName("success")
    data class Success(
        override val data: String,
        override val serverId: String,
        override val startedAt: MpInstant,
        override val endedAt: MpInstant,
        override val cpuProfile: CpuProfile?,
    ) : BackgroundJobResultModel {
        override fun isSuccess(): Boolean = true

        override fun isFailure(): Boolean = false
    }

    @Serializable
    @SerialName("failed")
    data class Failed(
        override val data: String,
        override val serverId: String,
        override val startedAt: MpInstant,
        override val endedAt: MpInstant,
        override val cpuProfile: CpuProfile?,
    ) : BackgroundJobResultModel {
        override fun isSuccess(): Boolean = false

        override fun isFailure(): Boolean = true
    }

    val data: String
    val serverId: String
    val startedAt: MpInstant
    val endedAt: MpInstant
    val cpuProfile: CpuProfile?

    val executionDuration get() = endedAt - startedAt
    val executionDurationMs get() = executionDuration.inWholeMilliseconds

    fun isSuccess(): Boolean
    fun isFailure(): Boolean
}
