package io.peekandpoke.ultra.streams.ops

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * The clock a stream's motion runs on (ULTRA-00242): [next] calls back once, on the next frame, with the frame's time
 * in milliseconds, and returns the way to cancel that call. A test drives its own clock frame by frame.
 */
fun interface FrameClock {

    fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit

    companion object {
        /** The platform's clock: the animation frames on JS, a 60 Hz [ticker] elsewhere. */
        val default: FrameClock get() = platformFrameClock

        /**
         * A frame every [intervalMs], on coroutines in [context], timed by the monotonic clock from the clock's
         * creation. On the JVM and native the frames run on [context]'s threads.
         */
        fun ticker(intervalMs: Int = 16, context: CoroutineContext = Dispatchers.Default): FrameClock =
            TickerFrameClock(intervalMs = intervalMs, context = context)
    }
}

/** The clock [FrameClock.default] returns. */
internal expect val platformFrameClock: FrameClock

private class TickerFrameClock(private val intervalMs: Int, context: CoroutineContext) : FrameClock {
    private val scope = CoroutineScope(context + SupervisorJob())
    private val start = TimeSource.Monotonic.markNow()

    override fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit {
        val job = scope.launch {
            delay(intervalMs.toLong())
            onFrame(start.elapsedNow().toDouble(DurationUnit.MILLISECONDS))
        }

        return { job.cancel() }
    }
}
