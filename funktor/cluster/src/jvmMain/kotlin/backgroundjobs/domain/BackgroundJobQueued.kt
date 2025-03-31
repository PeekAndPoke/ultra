package de.peekandpoke.funktor.cluster.backgroundjobs.domain

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlin.time.Duration

@Karango
data class BackgroundJobQueued(
    val type: String,
    val data: Any?,
    val dataHash: Int = data.hashCode(),
    val retryPolicy: BackgroundJobRetryPolicy,
    val createdAt: MpInstant?,
    val dueAt: MpInstant = MpInstant.now(),
    val state: State = State.WAITING,
    val results: List<BackgroundJobExecutionResult> = emptyList(),
) {
    enum class State {
        WAITING,
        PROCESSING,
    }

    fun plusResult(result: BackgroundJobExecutionResult) = copy(
        results = results.plus(result)
    )

    fun toArchived(archivedAt: MpInstant, expiresAfter: Duration) = BackgroundJobArchived(
        type = type,
        data = data,
        dataHash = dataHash,
        results = results,
        retryPolicy = retryPolicy,
        createdAt = createdAt,
        archivedAt = archivedAt,
        expiresAt = archivedAt.plus(expiresAfter).toEpochSeconds(),
    )
}
