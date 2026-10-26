package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import kotlin.time.Duration

/** A single frame emitted by a ticker stream */
data class TickerFrame(val count: Int, val currentTime: Double, val deltaTime: Double)

/** Creates a ticker stream that emits a [TickerFrame] at the given [intervalMs] */
fun ticker(intervalMs: Int): Stream<TickerFrame> = ticker(FrameClock.ticker(intervalMs))

/** Creates a ticker stream that emits a [TickerFrame] at the given [interval] */
fun ticker(interval: Duration): Stream<TickerFrame> = ticker(interval.inWholeMilliseconds.toInt())

/**
 * Creates a ticker stream that emits a [TickerFrame] on every frame of the [clock] (ULTRA-00242). The initial frame,
 * before the clock's first, carries the time 0.0; the frames after it carry the clock's times.
 */
fun ticker(clock: FrameClock): Stream<TickerFrame> = ticker(clock = clock, startTime = 0.0)

/** A ticker whose initial frame carries [startTime]: `animTicker()` starts at `performance.now()`. */
internal fun ticker(clock: FrameClock, startTime: Double): Stream<TickerFrame> =
    FrameTicker(clock = clock, startTime = startTime)

/**
 * A ticker on a [FrameClock].
 *
 * Only runs while there are active subscribers: one frame is pending at most, none while nobody listens. A frame's
 * handle stays set while it emits, so a subscribe meanwhile asks for no second frame; with a clock on other threads
 * (the JVM's) that narrows the race, it does not close it: streams are not thread-safe.
 */
private class FrameTicker(
    private val clock: FrameClock,
    startTime: Double,
) : StreamSourceImpl<TickerFrame>(TickerFrame(count = 0, currentTime = startTime, deltaTime = 0.0)) {

    private var cancelFrame: (() -> Unit)? = null
    private var previousTime: Double? = null
    private var count: Int = 0

    override fun onSub() {
        if (cancelFrame == null) {
            cancelFrame = clock.next(::frame)
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            cancelFrame?.invoke()
            cancelFrame = null
            previousTime = null
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
            cancelFrame = if (subscriptions.isNotEmpty()) clock.next(::frame) else null
        }
    }
}
