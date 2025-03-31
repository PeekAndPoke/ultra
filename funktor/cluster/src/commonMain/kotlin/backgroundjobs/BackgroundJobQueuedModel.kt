package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundJobQueuedModel(
    val id: String,
    val type: String,
    val data: String,
    val dataHash: Int,
    val retryPolicy: BackgroundJobRetryPolicyModel,
    val createdAt: MpInstant?,
    val dueAt: MpInstant,
    val state: String,
    val results: List<BackgroundJobResultModel>,
) {
    fun lastTryDidFail(): Boolean {
        return results.lastOrNull()?.isFailure() ?: false
    }
}
