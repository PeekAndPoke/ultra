package io.peekandpoke.funktor.cluster.backgroundjobs.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Vault
import kotlin.time.Duration

/** A background job waiting in the queue to be executed. */
@Vault
data class BackgroundJobQueued(
    val type: String,
    val data: Any?,
    val dataHash: Int = calcHash(data),
    val retryPolicy: BackgroundJobRetryPolicy,
    val createdAt: MpInstant?,
    val dueAt: MpInstant = MpInstant.now(),
    val state: State = State.WAITING,
    val results: List<BackgroundJobExecutionResult> = emptyList(),
    /**
     * Slot key for cross-JVM atomic deduplication. When non-null, the storage layer enforces
     * uniqueness (partial unique index on Mongo, sparse unique index on Karango) so two
     * concurrent `queueIfNotPresent` calls on different JVMs cannot both succeed.
     * `create(...)` leaves this null so the unique constraint does not apply, allowing many
     * same-type jobs to coexist.
     *
     * Cleared atomically when the job is claimed (state -> PROCESSING) so a new
     * `queueIfNotPresent` for the same (type, dataHash) can succeed while the current one runs.
     */
    val dedupeKey: String? = null,
) {
    companion object {
        private fun calcHash(data: Any?): Int {
            val hash = try {
                Codec.default.slumber(data)?.hashCode() ?: data?.hashCode() ?: 0
            } catch (_: Exception) {
                Hash(data).hashCode()
            }

            return hash
        }

        /** Builds the cross-JVM dedupe slot key from a job's type and data hash. */
        fun makeDedupeKey(type: String, dataHash: Int): String = "$type:$dataHash"
    }

    private data class Hash(
        val data: Any?,
        val cls: String = (data?.let { it::class } ?: Any::class).let {
            it.qualifiedName ?: it.simpleName ?: "Any"
        },
    )

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
