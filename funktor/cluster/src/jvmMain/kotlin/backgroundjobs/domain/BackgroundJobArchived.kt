package de.peekandpoke.funktor.cluster.backgroundjobs.domain

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant

@Karango
data class BackgroundJobArchived(
    val type: String,
    val data: Any?,
    val dataHash: Int,
    val retryPolicy: BackgroundJobRetryPolicy,
    val results: List<BackgroundJobExecutionResult>,
    val createdAt: MpInstant?,
    val archivedAt: MpInstant,
    val expiresAt: Long,
) {
    fun didFinallySucceed(): Boolean {
        return results.lastOrNull() is BackgroundJobExecutionResult.Success
    }
}
