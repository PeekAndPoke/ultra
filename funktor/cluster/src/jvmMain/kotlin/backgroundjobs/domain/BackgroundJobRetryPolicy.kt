package de.peekandpoke.funktor.cluster.backgroundjobs.domain

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.SerialName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

sealed class BackgroundJobRetryPolicy {

    /**
     * Returns the [MpInstant] at which the next retry for the Job is due.
     *
     * If the method returns 'null' then the Job will be archived.
     */
    open fun scheduleRetry(job: Stored<BackgroundJobQueued>, now: MpInstant): MpInstant? {
        return null
    }

    @SerialName("none")
    data object None : BackgroundJobRetryPolicy()

    @SerialName("linear-delay")
    data class LinearDelay(val delayInMs: Long = 1_000, val maxTries: Int = 10) : BackgroundJobRetryPolicy() {

        companion object {
            operator fun invoke(delay: Duration, maxTries: Int) = LinearDelay(
                delayInMs = delay.inWholeMilliseconds,
                maxTries = maxTries
            )
        }

        override fun scheduleRetry(job: Stored<BackgroundJobQueued>, now: MpInstant): MpInstant? {

            if (job.value.results.size >= maxTries) {
                return null
            }

            return now.plus(delayInMs.milliseconds)
        }
    }
}
