package de.peekandpoke.ultra.common.maths

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration

typealias EaseFn = (Double) -> Double

object Ease {

    fun EaseFn.bindFromTo(from: Double, to: Double): BoundFromTo {
        return BoundFromTo(from = from, to = to, fn = this)
    }

    class Timed(
        val from: Double,
        val to: Double,
        val start: MpInstant,
        val duration: Duration,
        val fn: EaseFn,
    ) {
        private val startMillis = start.toEpochMillis()
        private val durationMillis = maxOf(1.0, duration.inWholeMilliseconds.toDouble())
        private val endMillis = startMillis + durationMillis.toInt()

        fun calc(now: MpInstant, onProgress: (progress: Double, done: Boolean) -> Unit = { _, _ -> }): Double {
            val nowMillis: Long = now.toEpochMillis()
            val progress: Double = minOf(1.0, (nowMillis - startMillis) / durationMillis)
            val isDone: Boolean = endMillis <= nowMillis

            return calc(from, to, progress, fn).also {
                onProgress(progress, isDone)
            }
        }
    }

    class BoundFromTo(val from: Double, val to: Double, val fn: EaseFn) {
        operator fun invoke(progress: Double): Double {
            return calc(progress = progress)
        }

        fun calc(progress: Double): Double {
            return calc(from = from, to = to, progress = progress, fn = fn)
        }
    }

    fun calc(from: Double, to: Double, progress: Double, fn: EaseFn): Double {
        val diff = to - from

        return from + diff * fn(max(0.0, min(progress, 1.0)))
    }

    val linear: EaseFn = { x -> x }

    object In {
        fun pow(power: Int): EaseFn {
            return { x -> x.pow(power) }
        }

        val sine: EaseFn = { x -> 1.0 - cos((x * PI) / 2.0) }

        val quad: EaseFn = pow(2)

        val cubic: EaseFn = pow(3)

        val quart: EaseFn = pow(4)

        val quint: EaseFn = pow(5)

        val circ: EaseFn = { x -> 1.0 - sqrt(1.0 - x.pow(2)) }

        val elastic: EaseFn = { x ->
            val c4 = (2.0 * PI) / 3.0

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                else -> (-2.0).pow(10.0 * x - 10.0) * sin((x * 10.0 - 10.75) * c4)
            }
        }

        val bounce: EaseFn = { x -> Out.bounce(1 - x) }
    }

    object Out {
        fun pow(power: Int): EaseFn {
            return { x -> 1.0 - (1.0 - x).pow(power) }
        }

        val sine: EaseFn = { x -> sin((x * PI) / 2) }

        val quad: EaseFn = pow(2)

        val cubic: EaseFn = pow(3)

        val quart: EaseFn = pow(4)

        val quint: EaseFn = pow(5)

        val circ: EaseFn = { x -> sqrt(1.0 - (x - 1.0).pow(2)) }

        val elastic: EaseFn = { x ->
            val c4 = (2.0 * PI) / 3.0

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                else -> (2.0).pow(-10.0 * x) * sin((x * 10.0 - 0.75) * c4) + 1.0
            }
        }

        val bounce: EaseFn = { x ->
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

    object InOut {
        fun pow(power: Int): EaseFn {
            val base = 2.0.pow(power - 1)

            return { x ->
                if (x < 0.5) {
                    base * x.pow(power)
                } else {
                    1.0 - (-2.0 * x + 2.0).pow(power) / 2.0
                }
            }
        }

        val sine: EaseFn = { x -> -(cos(PI * x) - 1.0) / 2.0 }

        val quad: EaseFn = pow(2)

        val cubic: EaseFn = pow(3)

        val quart: EaseFn = pow(4)

        val quint: EaseFn = pow(5)

        val elastic: EaseFn = { x ->
            val c5 = (2.0 * PI) / 4.5

            when {
                x <= 0.0 -> 0.0
                x >= 1.0 -> 1.0
                x <= 0.5 -> -(2.0.pow(20.0 * x - 10.0) * sin((20.0 * x - 11.125) * c5)) / 2.0
                else -> (2.0.pow(-20.0 * x + 10.0) * sin((20.0 * x - 11.125) * c5)) / 2.0 + 1.0
            }
        }

        val bounce: EaseFn = { x ->
            if (x < 0.5) {
                (1.0 - Out.bounce(1.0 - 2.0 * x)) / 2.0
            } else {
                (1.0 + Out.bounce(2.0 * x - 1.0)) / 2.0
            }
        }
    }
}
