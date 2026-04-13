package io.peekandpoke.funktor.cluster.backgroundjobs.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault

/** A background job that has been executed and moved to the archive. */
@Vault
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
