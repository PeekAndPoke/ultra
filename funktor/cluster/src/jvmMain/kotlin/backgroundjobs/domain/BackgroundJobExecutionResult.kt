package de.peekandpoke.funktor.cluster.backgroundjobs.domain

import de.peekandpoke.funktor.core.model.CpuProfile
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName

sealed interface BackgroundJobExecutionResult {

    @SerialName("success")
    data class Success(
        override val data: Map<String, Any?>,
        override val serverId: String,
        override val startedAt: MpInstant,
        override val endedAt: MpInstant,
        override val cpuProfile: CpuProfile? = null,
    ) : BackgroundJobExecutionResult {
        override fun withCpuProfile(profile: CpuProfile): Success {
            return copy(cpuProfile = profile)
        }
    }

    @SerialName("failed")
    data class Failed(
        override val data: Map<String, Any?>,
        override val serverId: String,
        override val startedAt: MpInstant,
        override val endedAt: MpInstant,
        override val cpuProfile: CpuProfile? = null,
    ) : BackgroundJobExecutionResult {
        override fun withCpuProfile(profile: CpuProfile): Failed {
            return copy(cpuProfile = profile)
        }
    }

    val data: Map<String, Any?>
    val serverId: String
    val startedAt: MpInstant
    val endedAt: MpInstant
    val cpuProfile: CpuProfile?

    fun withCpuProfile(profile: CpuProfile): BackgroundJobExecutionResult
}
