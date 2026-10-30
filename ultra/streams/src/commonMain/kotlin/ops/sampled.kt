package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Takes the stream's latest value at every tick, [every] apart, also when it did not change and also when it is
 * `null` (ULTRA-00267, from Klang): the time axis is the clock's, not the producer's, so
 * `level.sampled(every = 250.milliseconds).history(capacity = 480)` keeps two minutes at four values a second, with
 * gaps where the source had none. In the browser a hidden tab's timers are slowed (to once a second and less), so
 * there the same capacity spans more time.
 *
 * [every] may change while listening: a new interval applies from the next tick (the one due keeps its time, so a
 * slider moving the interval does not hold the ticks off); a repeat of the same interval changes nothing. An interval
 * of zero or less counts as 1 ms, one beyond `Int.MAX_VALUE` ms as that; an infinite one stops the ticks at once.
 * The source and [every] are listened to only while someone listens here, which also keeps a source fresh that is
 * computed only while listened to. The ticks run on [FrameClock.ticker] in [context]: on the JVM and native pass the
 * context the source emits on, so ticks and subscribers stay on one thread (default: a pool thread).
 */
fun <T> Stream<T>.sampled(
    every: Stream<Duration>,
    context: CoroutineContext = Dispatchers.Default,
): Stream<T> = sampled(
    every = every,
    clockFor = { FrameClock.ticker(intervalMs = it.inWholeIntervalMs(), context = context) },
)

/** Takes the stream's latest value at every tick, [every] apart; see the overload with a `Stream<Duration>`. */
fun <T> Stream<T>.sampled(every: Duration, context: CoroutineContext = Dispatchers.Default): Stream<T> =
    sampled(every = steady(every), context = context)

/** Takes the stream's latest value at every tick, [everyMs] apart; see the overload with a `Stream<Duration>`. */
fun <T> Stream<T>.sampled(everyMs: Int, context: CoroutineContext = Dispatchers.Default): Stream<T> =
    sampled(every = everyMs.milliseconds, context = context)

/** Takes the stream's latest value at every frame of [clock] (animation frames, or a test's hand clock). */
fun <T> Stream<T>.sampled(clock: FrameClock): Stream<T> = sampled(every = steady(Duration.ZERO), clockFor = { clock })

/** Whole milliseconds as an `Int`, clamped: a longer interval would wrap to a negative one and tick back to back. */
internal fun Duration.inWholeIntervalMs(): Int = inWholeMilliseconds.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

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

    /** The interval [clock] ticks at; null before the first. */
    private var interval: Duration? = null

    /** The pending tick's cancel. */
    private var cancelTick: (() -> Unit)? = null

    /** Nobody listening: the source's value as it is now. */
    override fun invoke(): T = if (listening) current else source()

    override fun onSub() {
        if (!listening) {
            listening = true
            // Values are taken at the ticks only; the subscription keeps the source fresh
            // A handler of this operator's own: a lambda capturing nothing is one shared instance, and two samplers of
            // one source would then hold a single entry in its handler set, the first to leave unsubscribing both
            try {
                unsubscribeSource = source.subscribeToStream { keepFresh(it) }
                // A subscriber is told the value as it is at this moment, once the source is listened to
                current = source()
                unsubscribeEvery = every.subscribeToStream { interval(it) }
            } catch (e: Throwable) {
                // Half subscribed: undone, so the next subscriber starts afresh
                stopListening()
                throw e
            }
        }
    }

    override fun onUnSub() {
        if (subscriptions.isEmpty()) {
            stopListening()
        }
    }

    private fun stopListening() {
        listening = false
        stopTicks()
        unsubscribeEvery?.invoke()
        unsubscribeEvery = null
        interval = null
        unsubscribeSource?.invoke()
        unsubscribeSource = null
    }

    /** Values are taken at the ticks only: the subscription just keeps the source fresh. */
    @Suppress("UNUSED_PARAMETER")
    private fun keepFresh(value: T) {
    }

    private fun interval(next: Duration) {
        // A config stream may repeat its value (no stream dedupes): the same interval keeps the tick that is due
        if (next == interval) return

        interval = next

        clock = when {
            next.isInfinite() -> null
            // NaN cannot be a Duration; zero and negative count as the shortest
            else -> clockFor(next.coerceAtLeast(1.milliseconds))
        }

        when {
            // Infinite: no more ticks, not even the one due
            clock == null -> stopTicks()
            // Nothing due (the first interval, or after an infinite one): start
            cancelTick == null -> tickNext()
            // Otherwise the due tick keeps its time, and the next one is asked of the new clock
        }
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

        try {
            invoke(source())
        } finally {
            // Stopped or re-timed during the emit: then that decides. Also after a throw: one bad tick must not end
            // the ticks
            if (cancelTick === handle) {
                cancelTick = null
                tickNext()
            }
        }
    }
}
