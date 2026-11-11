package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** A single frame emitted by a ticker stream */
data class TickerFrame(val count: Int, val currentTime: Double, val deltaTime: Double)

/** Creates a ticker stream that emits a [TickerFrame] at the given [intervalMs] */
fun ticker(intervalMs: Int): Stream<TickerFrame> = ticker(interval = intervalMs.milliseconds)

/** Creates a ticker stream that emits a [TickerFrame] at the given [interval], on coroutines in [context] */
fun ticker(interval: Duration, context: CoroutineContext = Dispatchers.Default): Stream<TickerFrame> =
    ticker(interval = steady(interval), context = context)

/**
 * Creates a ticker stream that emits a [TickerFrame] at the [interval] it holds now, which may change (ULTRA-00271):
 * the frame that is due keeps its time, the next one comes at the new interval; the frame times run on, on one axis.
 * An interval of zero or less counts as 1 ms. The frames run on coroutines in [context]; [interval] is listened to
 * only while someone listens here.
 */
fun ticker(interval: Stream<Duration>, context: CoroutineContext = Dispatchers.Default): Stream<TickerFrame> {
    val start = TimeSource.Monotonic.markNow()

    return ticker(interval = interval) { ms -> tickerFrameClock(intervalMs = ms, context = context, start = start) }
}

/** A ticker at a changing [interval], with the clock for an interval in whole ms injectable for tests. */
internal fun ticker(interval: Stream<Duration>, clockFor: (intervalMs: Int) -> FrameClock): Stream<TickerFrame> {
    // One clock per interval: a repeat of the same interval keeps the clock
    val clocks = interval
        .map { it.inWholeIntervalMs().coerceAtLeast(1) }
        .distinct()
        .map(clockFor)

    return FrameTicker(clocks = clocks, startTime = 0.0)
}

/**
 * Creates a ticker stream that emits a [TickerFrame] on every frame of the [clock] (ULTRA-00242). The initial frame,
 * before the clock's first, carries the time 0.0; the frames after it carry the clock's times.
 */
fun ticker(clock: FrameClock): Stream<TickerFrame> = ticker(clock = clock, startTime = 0.0)

/** A ticker whose initial frame carries [startTime]: `animTicker()` starts at `performance.now()`. */
internal fun ticker(clock: FrameClock, startTime: Double): Stream<TickerFrame> =
    FrameTicker(clocks = steady(clock), startTime = startTime)

/**
 * A ticker on a [FrameClock].
 *
 * Only runs while there are active subscribers: one frame is pending at most, none while nobody listens. A frame's
 * handle stays set while it emits, so a subscribe meanwhile asks for no second frame; with a clock on other threads
 * (the JVM's) that narrows the race, it does not close it: streams are not thread-safe.
 */
private class FrameTicker(
    private val clocks: Stream<FrameClock>,
    startTime: Double,
) : StreamSourceImpl<TickerFrame>(TickerFrame(count = 0, currentTime = startTime, deltaTime = 0.0)) {

    private var cancelFrame: (() -> Unit)? = null
    private var previousTime: Double? = null
    private var count: Int = 0

    /** The clock the next frame is asked of: [clocks]' latest while someone listens. */
    private var clock: FrameClock? = null
    private var unsubscribeClocks: Unsubscribe? = null

    override fun onSub() {
        if (unsubscribeClocks == null) {
            // A new clock applies from the next frame: the one that is due keeps its time
            unsubscribeClocks = clocks.subscribeToStream { clock = it }
        }

        if (cancelFrame == null) {
            cancelFrame = clock?.next(::frame)
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            cancelFrame?.invoke()
            cancelFrame = null
            previousTime = null
            unsubscribeClocks?.invoke()
            unsubscribeClocks = null
            clock = null
        }
    }

    private fun frame(timeMs: Double) {
        val handle = cancelFrame

        val delta = previousTime?.let { timeMs - it } ?: 0.0
        previousTime = timeMs
        count++

        invoke(TickerFrame(count = count, currentTime = timeMs, deltaTime = delta))

        // A handler may have unsubscribed the last listener (handle cleared), or also resubscribed (a new frame asked
        // for): then that decides
        if (cancelFrame === handle) {
            cancelFrame = if (subscriptions.isNotEmpty()) clock?.next(::frame) else null
        }
    }
}
