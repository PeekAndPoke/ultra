package io.peekandpoke.ultra.maths

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A law that moves a value towards a goal over time, one step at a time (ULTRA-00242).
 *
 * Each [step] is exact for any elapsed time, so the pace does not depend on the frame rate. A law with parameters
 * that make no sense (zero, negative, NaN) lands on the goal at once.
 */
interface Smoothing {

    /** Where a smoothed value is, and how fast it moves, in units per second. */
    data class Motion(val position: Double, val velocity: Double = 0.0)

    /** The [motion] [elapsedMs] later, heading for [goal]. An [elapsedMs] of 0 or less, or NaN, leaves it as it is. */
    fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion

    /**
     * True when [motion] is within [near] of [goal], in the value's units, and stays there: the motion may land.
     * A [near] of 0 or less counts as 0: the motion lands only at the floating-point floor, after many invisible steps.
     */
    fun isSettled(motion: Motion, goal: Double, near: Double): Boolean = !(abs(motion.position - goal) > near)

    /**
     * Keeps [keepPer10Ms] of the way left per 10 ms: a quick start, a gentle arrival, no overshoot.
     *
     * A share of 1 or more counts as 0.999, the slowest there is; 0 or less lands at once.
     */
    class Exponential(val keepPer10Ms: Double) : Smoothing {
        override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion {
            if (!(elapsedMs > 0.0)) return motion
            // NaN-guard: a share that is not a number counts as none
            if (!(keepPer10Ms > 0.0)) return Motion(goal)

            val share = keepPer10Ms.coerceAtMost(MAX_KEEP)
            val keep = share.pow(elapsedMs / 10.0)
            // Shrinks the way left even under rounding, so it reaches the goal or a fixed point
            val next = goal + (motion.position - goal) * keep

            // The curve's speed at its end, per second: the same however the time was cut into steps
            return Motion(position = next, velocity = (next - goal) * ln(share) * 100.0)
        }
    }

    /**
     * Moves at [unitsPerSecond] at most, never past the goal: a slew limit. 0 or less lands at once. A stride too small
     * to change the value (below half its floating-point step) changes nothing: a stream then lands on its second
     * frame.
     */
    class Linear(val unitsPerSecond: Double) : Smoothing {
        override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion {
            if (!(elapsedMs > 0.0)) return motion
            if (!(unitsPerSecond > 0.0)) return Motion(goal)

            val way = goal - motion.position
            val stride = unitsPerSecond * elapsedMs / 1000.0

            if (stride >= abs(way)) return Motion(goal)

            return Motion(position = motion.position + way.sign * stride, velocity = way.sign * unitsPerSecond)
        }
    }

    /**
     * Meter ballistics: exponential with [up] kept per 10 ms while rising and [down] while falling. A small [up] and a
     * large [down] rise fast and fall slowly, as a VU meter does.
     */
    class AttackRelease(val up: Double, val down: Double) : Smoothing {
        private val rising = Exponential(up)
        private val falling = Exponential(down)

        override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion =
            (if (goal >= motion.position) rising else falling).step(motion, goal, elapsedMs)
    }

    /**
     * A damped spring, with inertia: `x'' = -stiffness * (x - goal) - damping * x'`, time in seconds.
     *
     * A [damping] below `2 * sqrt(stiffness)` overshoots and settles like an analog needle; at it, the spring is
     * critically damped and arrives fastest without overshoot. A [stiffness] or [damping] of 0 or less lands at once:
     * an undamped spring would swing forever. Settled means its energy is too small to leave [near] again
     * (`y^2 + v^2 / stiffness <= near^2`, damping only loses energy), so a needle passing the goal goes on.
     */
    class Spring(val stiffness: Double, val damping: Double) : Smoothing {
        override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion {
            if (!(elapsedMs > 0.0)) return motion
            if (!(stiffness > 0.0) || !(damping > 0.0)) return Motion(goal)

            val t = elapsedMs / 1000.0
            val y0 = motion.position - goal
            val v0 = motion.velocity
            val omega = sqrt(stiffness)
            val zeta = damping / (2.0 * omega)

            val (y, v) = when {
                // Near critical damping, the general solutions divide by almost zero
                abs(zeta - 1.0) < 1e-6 -> {
                    val b = v0 + omega * y0
                    val e = exp(-omega * t)

                    (y0 + b * t) * e to (v0 - omega * b * t) * e
                }

                zeta < 1.0 -> {
                    val wd = omega * sqrt(1.0 - zeta * zeta)
                    val e = exp(-zeta * omega * t)
                    val c = cos(wd * t)
                    val s = sin(wd * t)

                    e * (y0 * c + (v0 + zeta * omega * y0) / wd * s) to
                        e * (v0 * c - (omega * omega * y0 + zeta * omega * v0) / wd * s)
                }

                else -> {
                    val root = omega * sqrt(zeta * zeta - 1.0)
                    val r1 = -zeta * omega + root
                    val r2 = -zeta * omega - root
                    val a = (v0 - r2 * y0) / (r1 - r2)
                    val b = y0 - a

                    a * exp(r1 * t) + b * exp(r2 * t) to r1 * a * exp(r1 * t) + r2 * b * exp(r2 * t)
                }
            }

            return Motion(position = goal + y, velocity = v)
        }

        override fun isSettled(motion: Motion, goal: Double, near: Double): Boolean {
            val y = motion.position - goal
            // A near below 0 counts as 0, as for the other laws; NaN stays NaN
            val n = if (near < 0.0) 0.0 else near
            // NaN-guard: a motion that is not a number counts as settled, so it lands
            return !(y * y + motion.velocity * motion.velocity / stiffness > n * n)
        }
    }
}

/** The slowest share [Smoothing.Exponential] keeps: a share of 1 would never move. */
private const val MAX_KEEP = 0.999
