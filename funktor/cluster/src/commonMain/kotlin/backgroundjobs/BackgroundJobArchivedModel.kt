package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundJobArchivedModel(
    val id: String,
    val type: String,
    val data: String,
    val dataHash: Int,
    val retryPolicy: BackgroundJobRetryPolicyModel,
    val results: List<BackgroundJobResultModel>,
    val createdAt: MpInstant?,
    val archivedAt: MpInstant,
) {
    fun lastTryDidSucceed(): Boolean {
        return results.lastOrNull()?.isSuccess() ?: false
    }

    fun lastTryDidFail(): Boolean {
        return results.lastOrNull()?.isFailure() ?: false
    }
}
