package de.peekandpoke.ktorfx.cluster.workers

import de.peekandpoke.ultra.common.datetime.MpInstant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A worker is continuously running in a single server.
 *
 * Every worker must have a system-wide unique [id].
 *
 * The execution is repeated after a certain time has passed. See [shouldRun].
 *
 * The work is done in [execute].
 *
 * The [WorkersRunner] frequently calls [shouldRun] on each registered work.
 * If it returns true the worker gets executed.
 */
interface Worker {

    companion object {
        val counter: AtomicInteger = AtomicInteger(0)
    }

    object Every {
        operator fun invoke(duration: Duration): (MpInstant, MpInstant) -> Boolean =
            { lastRun: MpInstant, now: MpInstant ->
                now.minus(duration) > lastRun
            }

        fun milliseconds(milliseconds: Int) = invoke(milliseconds.milliseconds)

        fun seconds(seconds: Int) = invoke(seconds.seconds)

        fun minutes(minutes: Int) = invoke(minutes.minutes)

        fun hours(hours: Int) = invoke(hours.hours)
    }

    /** The unique id of the worker. */
    val id: String
        get() = this::class.qualifiedName
            ?: this::class.simpleName
            ?: "service-${counter.incrementAndGet()}"

    /** Returns true when the worker should run. */
    val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean

    /** Does the work */
    suspend fun execute(state: StateProvider)
}
