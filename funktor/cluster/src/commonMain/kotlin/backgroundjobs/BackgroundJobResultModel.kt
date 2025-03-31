package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BackgroundJobResultModel {

    abstract val data: String
    abstract val serverId: String
    abstract val executedAt: MpInstant
    abstract val executionTimeMs: Long

    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean

    @Serializable
    @SerialName("success")
    data class Success(
        override val data: String,
        override val serverId: String,
        override val executedAt: MpInstant,
        override val executionTimeMs: Long,
    ) : BackgroundJobResultModel() {
        override fun isSuccess(): Boolean = true

        override fun isFailure(): Boolean = false
    }

    @Serializable
    @SerialName("failed")
    data class Failed(
        override val data: String,
        override val serverId: String,
        override val executedAt: MpInstant,
        override val executionTimeMs: Long,
    ) : BackgroundJobResultModel() {
        override fun isSuccess(): Boolean = false

        override fun isFailure(): Boolean = true
    }
}
