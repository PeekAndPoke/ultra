package io.peekandpoke.funktor.cluster.backgroundjobs.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.SerialName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Defines how and whether a failed background job should be retried. */
sealed class BackgroundJobRetryPolicy {

    /**
     * Returns the [MpInstant] at which the next retry for the Job is due.
     *
     * If the method returns 'null' then the Job will be archived.
     */
    open suspend fun scheduleRetry(job: Stored<BackgroundJobQueued>, now: MpInstant): MpInstant? {
        return null
    }

    /** No retries; the job is archived on first failure. */
    @SerialName("none")
    data object None : BackgroundJobRetryPolicy()

    /** Retries up to [maxTries] times with a fixed delay between attempts. */
    @SerialName("linear-delay")
    data class LinearDelay(val delayInMs: Long = 1_000, val maxTries: Int = 10) : BackgroundJobRetryPolicy() {

        companion object {
            operator fun invoke(delay: Duration, maxTries: Int) = LinearDelay(
                delayInMs = delay.inWholeMilliseconds,
                maxTries = maxTries
            )
        }

        override suspend fun scheduleRetry(job: Stored<BackgroundJobQueued>, now: MpInstant): MpInstant? {

            if (job.resolve().results.size >= maxTries) {
                return null
            }

            return now.plus(delayInMs.milliseconds)
        }
    }
}
