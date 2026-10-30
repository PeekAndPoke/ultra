package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.maths.Smoothing.Motion
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSourceImpl
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.steady

/**
 * Smooths a stream of numbers exponentially: a new value is approached a step each frame of [clock], until the stream
 * is there, also while the source holds still (ULTRA-00242, from Klang's KLANG-00345). Each step keeps
 * [sharePer10Ms] of the way left per 10 ms, sized by the time since the last frame, so the pace is the same at any
 * frame rate. A share of 0 or less (or NaN) passes every value at once; 1 or more counts as 0.999, the slowest.
 *
 * See the overload with a [Smoothing] for `null`, [rest], [near] and the lifecycle.
 */
fun Stream<Double?>.smooth(
    sharePer10Ms: Double,
    rest: Double = 0.0,
    near: Double = 0.001,
    clock: FrameClock = FrameClock.default,
): Stream<Double?> = StreamSmoothOperator(
    source = this,
    // NaN-guard: a share that is not a number counts as none
    smoothing = steady(if (sharePer10Ms > 0.0) Smoothing.Exponential(sharePer10Ms) else null),
    rest = rest,
    near = near,
    clock = clock,
)

/**
 * Smooths a stream of numbers by the law of [smoothing]: a step each frame of [clock], until the stream is there.
 *
 * - `null` (no value, offline), a NaN and an infinity pass at once as `null` and stop the motion.
 * - The first value after someone starts listening is taken as it is; after a `null` the motion starts from [rest]
 *   (a needle rises from its rest when it comes online).
 * - A new value on the way becomes the new goal; the motion keeps its speed ([Smoothing.Spring] swings on).
 * - The motion stops once [Smoothing.isSettled] within [near], in the value's units, and lands exactly on the value:
 *   no endless trickle of values. Keep [near] above 0: at 0 it lands only at the floating-point floor.
 *
 * The source is listened to only while someone listens here, and no frame is asked for while nothing moves. On the
 * JVM and native the default clock calls back on a pool thread: pass a [FrameClock.ticker] on the thread the source
 * emits on.
 */
fun Stream<Double?>.smooth(
    smoothing: Smoothing,
    rest: Double = 0.0,
    near: Double = 0.001,
    clock: FrameClock = FrameClock.default,
): Stream<Double?> = smooth(smoothing = steady(smoothing), rest = rest, near = near, clock = clock)

/**
 * Smooths a stream of numbers by the law [smoothing] holds now, which may change while the stream moves (ULTRA-00264,
 * from Klang): the next step goes from the current position and velocity by the new law, over the real time since the
 * last frame, so a [Smoothing.Spring] taking over from a [Smoothing.Exponential] swings on at the speed it had. A law
 * of `null` passes every value at once and lands a stream that is on its way. A new law while the stream rests moves
 * nothing.
 *
 * See the overload with a [Smoothing] for `null` values, [rest], [near] and the lifecycle; [smoothing] is listened to
 * only while someone listens here.
 */
fun Stream<Double?>.smooth(
    smoothing: Stream<out Smoothing?>,
    rest: Double = 0.0,
    near: Double = 0.001,
    clock: FrameClock = FrameClock.default,
): Stream<Double?> = StreamSmoothOperator(source = this, smoothing = smoothing, rest = rest, near = near, clock = clock)

/** The smooth operator; a law of `null` passes every value at once. */
private class StreamSmoothOperator(
    private val source: Stream<Double?>,
    private val smoothing: Stream<out Smoothing?>,
    private val rest: Double,
    private val near: Double,
    private val clock: FrameClock,
) : StreamSourceImpl<Double?>(source().finiteOrNull()) {

    private companion object {
        /** A usual frame, for the first step of a motion and for a clock that stands or goes backwards. */
        const val FRAME_MS = 1000.0 / 60.0

        fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }
    }

    private var unsubscribe: Unsubscribe? = null

    private var unsubscribeLaw: Unsubscribe? = null

    /** The law the next step goes by: [smoothing]'s latest while someone listens. */
    private var law: Smoothing? = null

    /** Someone listens: the source is subscribed, frames may be asked for. */
    private var listening = false

    /** The first value of this listening has come; until then a value is taken as it is. */
    private var settledIn = false

    /** Where the stream heads: the source's latest value. */
    private var target: Double? = current

    /** Where the stream is and how fast it moves; null while the stream is `null`. */
    private var motion: Motion? = current?.let { Motion(it) }

    /** The pending frame's cancel, while the stream moves. */
    private var cancelFrame: (() -> Unit)? = null

    /** When the last step was taken, from the clock; null before the first step of a motion. */
    private var lastFrameAt: Double? = null

    /** Nobody listening: the source's value as it is now, as the other operators read through then. */
    override fun invoke(): Double? = if (listening) current else source().finiteOrNull()

    override fun onSub() {
        if (!listening) {
            listening = true
            settledIn = false
            try {
                // The law first, so the source's first value already knows it
                unsubscribeLaw = smoothing.subscribeToStream { lawChanged(it) }
                unsubscribe = source.subscribeToStream { incoming(it) }
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
        unsubscribe?.invoke()
        unsubscribe = null
        unsubscribeLaw?.invoke()
        unsubscribeLaw = null
        stop()
    }

    private fun incoming(value: Double?) {
        val next = value.finiteOrNull()

        target = next

        when {
            // The first value of a listening, nothing (offline), or no smoothing: at once
            !settledIn || next == null || law == null -> {
                settledIn = true
                stop()
                emit(next?.let { Motion(it) })
            }

            else -> {
                if (motion == null) {
                    emit(Motion(rest))
                }

                if (current != next) {
                    start()
                }
            }
        }
    }

    private fun start() {
        if (cancelFrame != null || !listening) {
            return
        }

        lastFrameAt = null
        cancelFrame = clock.next { at -> frame(at) }
    }

    private fun stop() {
        cancelFrame?.invoke()
        cancelFrame = null
        lastFrameAt = null
    }

    private fun frame(at: Double) {
        // This frame's handle stays set while it emits: a value or a subscribe meanwhile asks for no second frame
        val handle = cancelFrame
        val goal = target
        val from = motion

        val law = law

        if (law == null || goal == null || from == null) {
            cancelFrame = null
            lastFrameAt = null
            return
        }

        // NaN-guard: the first step, or a clock that stands or goes backwards, counts as one usual frame
        val elapsed = lastFrameAt?.let { at - it }?.takeIf { it > 0.0 } ?: FRAME_MS
        var done = false
        var stepped = false

        try {
            val next = law.step(from, goal, elapsed)
            // There: settled within [near], or a step that changes nothing any more (the floating-point floor)
            done = law.isSettled(next, goal, near) || next == from

            lastFrameAt = at
            emit(if (done) Motion(goal) else next)
            stepped = true
        } finally {
            // A law or a listener threw: the frame is spent, so the next value starts the motion again
            if (!stepped && cancelFrame === handle) {
                cancelFrame = null
                lastFrameAt = null
            }
        }

        // Stopped during the emit (the last listener left, a null came): then that decides
        if (cancelFrame !== handle) {
            return
        }

        // Landed, unless a listener wrote a new value to the source during the emit: then on to it
        if ((done && target == goal) || !listening) {
            cancelFrame = null
            lastFrameAt = null
        } else {
            cancelFrame = clock.next { nextAt -> frame(nextAt) }
        }
    }

    private fun lawChanged(next: Smoothing?) {
        law = next

        // On its way: moving, or stalled short of the target (its law threw)
        val underway = cancelFrame != null || (settledIn && target != null && motion?.position != target)

        when {
            // No law any more: a stream on its way lands at once
            next == null -> if (underway) {
                stop()
                emit(target?.let { Motion(it) })
            }
            // A motion that stalled goes on with the new law
            underway -> start()
        }
    }

    private fun emit(next: Motion?) {
        motion = next
        invoke(next?.position)
    }
}
