package de.peekandpoke.ultra.common.maths

import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration

/**
 * Easing functions
 */
object Ease {

    /** Defines an easing function */
    interface Fn {
        operator fun invoke(progress: Double): Double
    }

    /** Impl of [Fn] */
    private class EaseFnImpl(private val fn: (Double) -> Double) : Fn {
        override operator fun invoke(progress: Double): Double {
            return fn(progress)
        }
    }

    /** Creates an easing function from the given function */
    fun easeFn(fn: (Double) -> Double): Fn = EaseFnImpl(fn)

    /**
     * Binds an easing function to the range [from] to [to]
     *
     * Can be called with invoke(progress) where progress is 0.0 to 1.0 to return the eased value.
     */
    fun Fn.bindFromTo(from: Number, to: Number): BoundFromTo {
        return BoundFromTo(from = from.toDouble(), to = to.toDouble(), fn = this)
    }

    /**
     * Creates a [Timed] easing function.
     */
    fun Fn.timed(
        from: Number,
        to: Number,
        duration: Duration,
        kronos: Kronos = Kronos.systemUtc,
        start: MpInstant = kronos.instantNow(),
    ): Timed {
        return Timed(
            fn = this,
            from = from.toDouble(),
            to = to.toDouble(),
            duration = duration,
            kronos = kronos,
            start = start,
        )
    }

    /**
     * An easing function that is bound to a range of values
     */
    class BoundFromTo(val from: Double, val to: Double, val fn: Fn) : Fn {
        override operator fun invoke(progress: Double): Double {
            return calc(progress = progress)
        }

        fun calc(progress: Double): Double {
            return calc(from = from, to = to, progress = progress, fn = fn)
        }
    }

    /**
     * Eases a value from [from] to [to] over the given [duration] starting at [start]
     *
     * Can be called with invoke(now: MpInstant) to get the eased value at the given time.
     *
     * Also calls the given onProgress callback with the current progress and whether the easing is done.
     */
    class Timed(
        /** The easing function */
        val fn: Fn,
        /** The start value */
        val from: Double,
        /** The end value */
        val to: Double,
        /** The duration of the easing */
        val duration: Duration,
        /** The kronos time-source */
        val kronos: Kronos = Kronos.systemUtc,
        /** The start time */
        val start: MpInstant = kronos.instantNow(),
    ) : Fn {
        private val startMillis = start.toEpochMillis()
        private val durationMillis = maxOf(1.0, duration.inWholeMilliseconds.toDouble())
        private val endMillis = startMillis + durationMillis.toInt()

        /** True when the easing is done and the [duration] has elapsed */
        val isDone get() = endMillis <= kronos.instantNow().toEpochMillis()

        /** Invokes the easing function with the given [progress] and returns the eased value */
        override fun invoke(progress: Double): Double {
            return calc(onProgress = { _, _ -> })
        }

        /** Invokes the easing function with the current timestamp and returns the eased value */
        operator fun invoke(onProgress: (progress: Double, done: Boolean) -> Unit = { _, _ -> }): Double {
            return calc(onProgress = onProgress)
        }

        /** Invokes the easing function with the current timestamp and returns the eased value */
        fun calc(onProgress: (progress: Double, done: Boolean) -> Unit = { _, _ -> }): Double {
            val nowMillis: Long = kronos.instantNow().toEpochMillis()
            val progress: Double = ((nowMillis - startMillis) / durationMillis).coerceIn(0.0, 1.0)
            val isDone: Boolean = endMillis <= nowMillis

            return calc(from, to, progress, fn).also {
                onProgress(progress, isDone)
            }
        }
    }

    /**
     * Calculates the eased value for the given [progress] between [from] and [to] using the given [fn]
     */
    fun calc(from: Double, to: Double, progress: Double, fn: Fn): Double {
        val diff = to - from

        return from + diff * fn(progress.coerceIn(0.0, 1.0))
    }

    /** No easing at all, always returns 0.0 */
    val stuck: Fn = easeFn { _ -> 0.0 }

    /** Immediate easing; always returns 1.0 */
    val immediate: Fn = easeFn { _ -> 1.0 }

    /** Linear easing */
    val linear: Fn = easeFn { x -> x.coerceIn(0.0, 1.0) }

    /** Ease In */
    object In {
        /** Sinus shaped */
        val sine: Fn = easeFn { x -> 1.0 - cos((x * PI) / 2.0) }

        /** Circularly shaped */
        val circ: Fn = easeFn { x -> 1.0 - sqrt(1.0 - x.pow(2)) }

        /** Exponentially shaped */
        fun pow(power: Double): Fn = easeFn { x -> x.pow(power) }

        /** Exponentially shaped by the power of 2.0 */
        val quad: Fn = pow(2.0)

        /** Exponentially shaped by the power of 3.0 */
        val cubic: Fn = pow(3.0)

        /** Exponentially shaped by the power of 4.0 */
        val quart: Fn = pow(4.0)

        /** Exponentially shaped by the power of 5.0 */
        val quint: Fn = pow(5.0)

        /** Elastically shaped */
        val elastic: Fn = easeFn { x ->
            val c4 = (2.0 * PI) / 3.0

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                else -> (-2.0).pow(10.0 * x - 10.0) * sin((x * 10.0 - 10.75) * c4)
            }
        }

        /** Bouncing shaped */
        val bounce: Fn = easeFn { x -> Out.bounce(1 - x) }
    }

    /** Ease Out */
    object Out {
        /** Sinus shaped */
        val sine: Fn = easeFn { x -> sin((x * PI) / 2) }

        /** Circularly shaped */
        val circ: Fn = easeFn { x -> sqrt(1.0 - (x - 1.0).pow(2)) }

        /** Exponentially shaped */
        fun pow(power: Double): Fn = easeFn { x -> 1.0 - (1.0 - x).pow(power) }

        /** Exponentially shaped by the power of 2.0 */
        val quad: Fn = pow(2.0)

        /** Exponentially shaped by the power of 3.0 */
        val cubic: Fn = pow(3.0)

        /** Exponentially shaped by the power of 4.0 */
        val quart: Fn = pow(4.0)

        /** Exponentially shaped by the power of 5.0 */
        val quint: Fn = pow(5.0)

        /** Elastically shaped */
        val elastic: Fn = easeFn { x ->
            val c4 = (2.0 * PI) / 3.0

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                else -> (2.0).pow(-10.0 * x) * sin((x * 10.0 - 0.75) * c4) + 1.0
            }
        }

        /** Bouncing shaped */
        val bounce: Fn = easeFn { x ->
            val n1 = 7.5625
            val d1 = 2.75

            when {
                x < 1.0 / d1 -> n1 * x * x

                x < 2.0 / d1 -> {
                    val t2 = x - 1.5 / d1
                    n1 * t2 * t2 + 0.75
                }

                x < 2.5 / d1 -> {
                    val t2 = x - 2.25 / d1
                    n1 * t2 * t2 + 0.9375
                }

                else -> {
                    val t2 = x - 2.625 / d1
                    n1 * t2 * t2 + 0.984375
                }
            }
        }
    }

    // Ease In and Out */
    object InOut {
        /** Sinus shaped */
        val sine: Fn = easeFn { x -> -(cos(PI * x) - 1.0) / 2.0 }

        /** Exponentially shaped */
        fun pow(power: Double): Fn {
            val base = 2.0.pow(power - 1.0)

            return easeFn { x ->
                if (x < 0.5) {
                    base * x.pow(power)
                } else {
                    1.0 - (-2.0 * x + 2.0).pow(power) / 2.0
                }
            }
        }

        /** Exponentially shaped by the power of 2.0 */
        val quad: Fn = pow(2.0)

        /** Exponentially shaped by the power of 3.0 */
        val cubic: Fn = pow(3.0)

        /** Exponentially shaped by the power of 4.0 */
        val quart: Fn = pow(4.0)

        /** Exponentially shaped by the power of 5.0 */
        val quint: Fn = pow(5.0)

        /** Elastically shaped */
        val elastic: Fn = easeFn { x ->
            val c5 = (2.0 * PI) / 4.5

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                x <= 0.5 -> -(2.0.pow(20.0 * x - 10.0) * sin((20.0 * x - 11.125) * c5)) / 2.0
                else -> (2.0.pow(-20.0 * x + 10.0) * sin((20.0 * x - 11.125) * c5)) / 2.0 + 1.0
            }
        }

        /** Bouncing shaped */
        val bounce: Fn = easeFn { x ->
            if (x < 0.5) {
                (1.0 - Out.bounce(1.0 - 2.0 * x)) / 2.0
            } else {
                (1.0 + Out.bounce(2.0 * x - 1.0)) / 2.0
            }
        }
    }
}
