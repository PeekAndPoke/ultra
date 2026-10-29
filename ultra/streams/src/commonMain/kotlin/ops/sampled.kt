package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Takes the stream's latest value at every tick, [every] apart, also when it did not change and also when it is
 * `null` (ULTRA-00267, from Klang): the time axis is the clock's, not the producer's, so
 * `level.sampled(every = 250.milliseconds).history(capacity = 480)` keeps two minutes at four values a second, with
 * gaps where the source had none.
 *
 * [every] may change while listening: the next tick comes [every] after the change. An interval of zero or less counts
 * as 1 ms; an infinite one stops the ticks. The source and [every] are listened to only while someone listens here,
 * which also keeps a source fresh that is computed only while listened to. The ticks run on [FrameClock.ticker].
 */
fun <T> Stream<T>.sampled(every: Stream<Duration>): Stream<T> =
    sampled(every = every, clockFor = { FrameClock.ticker(intervalMs = it.inWholeMilliseconds.toInt()) })

/** Takes the stream's latest value at every tick, [every] apart; see the overload with a `Stream<Duration>`. */
fun <T> Stream<T>.sampled(every: Duration): Stream<T> = sampled(every = steady(every))

/** Takes the stream's latest value at every tick, [everyMs] apart; see the overload with a `Stream<Duration>`. */
fun <T> Stream<T>.sampled(everyMs: Int): Stream<T> = sampled(every = everyMs.milliseconds)

/** Takes the stream's latest value at every frame of [clock] (animation frames, or a test's hand clock). */
fun <T> Stream<T>.sampled(clock: FrameClock): Stream<T> = sampled(every = steady(Duration.ZERO), clockFor = { clock })

/** The sampling operator, with the clock for an interval injectable for tests. */
internal fun <T> Stream<T>.sampled(every: Stream<Duration>, clockFor: (Duration) -> FrameClock): Stream<T> =
    StreamSampledOperator(source = this, every = every, clockFor = clockFor)

private class StreamSampledOperator<T>(
    private val source: Stream<T>,
    private val every: Stream<Duration>,
    private val clockFor: (Duration) -> FrameClock,
) : StreamSourceImpl<T>(source()) {

    private var listening = false

    private var unsubscribeSource: Unsubscribe? = null

    private var unsubscribeEvery: Unsubscribe? = null

    /** The clock of the current interval; null for an infinite one. */
    private var clock: FrameClock? = null

    /** The pending tick's cancel. */
    private var cancelTick: (() -> Unit)? = null

    /** Nobody listening: the source's value as it is now. */
    override fun invoke(): T = if (listening) current else source()

    override fun onSub() {
        if (!listening) {
            listening = true
            // A subscriber is told the value as it is at this moment, not from when the operator was built
            current = source()
            // Values are taken at the ticks only; the subscription keeps the source fresh
            unsubscribeSource = source.subscribeToStream { }
            unsubscribeEvery = every.subscribeToStream { interval(it) }
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            listening = false
            stopTicks()
            unsubscribeEvery?.invoke()
            unsubscribeEvery = null
            unsubscribeSource?.invoke()
            unsubscribeSource = null
        }
    }

    private fun interval(next: Duration) {
        stopTicks()

        clock = when {
            next.isInfinite() -> null
            // NaN cannot be a Duration; zero and negative count as the shortest
            else -> clockFor(next.coerceAtLeast(1.milliseconds))
        }

        tickNext()
    }

    private fun tickNext() {
        if (listening && cancelTick == null) {
            cancelTick = clock?.next { tick() }
        }
    }

    private fun stopTicks() {
        cancelTick?.invoke()
        cancelTick = null
    }

    private fun tick() {
        // This tick's handle stays set while it emits: a subscribe meanwhile asks for no second tick
        val handle = cancelTick

        invoke(source())

        // Stopped or re-timed during the emit: then that decides
        if (cancelTick === handle) {
            cancelTick = null
            tickNext()
        }
    }
}
