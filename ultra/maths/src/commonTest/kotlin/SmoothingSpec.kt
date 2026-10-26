package io.peekandpoke.ultra.maths

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.maths.Smoothing.Motion

/** Runs [law] from [from] towards [goal] in frames of [frameMs], at most [frames] frames, and returns every motion. */
private fun run(law: Smoothing, from: Double, goal: Double, frameMs: Double = 1000.0 / 60.0, frames: Int = 600) =
    generateSequence(Motion(from)) { law.step(it, goal, frameMs) }.take(frames + 1).toList()

/** The [Smoothing] laws (ULTRA-00242), stepped by hand: no clock. */
class SmoothingSpec : StringSpec({

    "every law follows the time, not the frames: two 10 ms steps go as far as one of 20 ms" {
        val laws = listOf(
            Smoothing.Exponential(0.8),
            Smoothing.Linear(10.0),
            Smoothing.AttackRelease(up = 0.5, down = 0.9),
            Smoothing.Spring(stiffness = 120.0, damping = 8.0),
            Smoothing.Spring(stiffness = 100.0, damping = 20.0),
            Smoothing.Spring(stiffness = 100.0, damping = 40.0),
        )

        laws.forEach { law ->
            val start = Motion(position = 0.0, velocity = 0.0)
            val twice = law.step(law.step(start, 1.0, 10.0), 1.0, 10.0)
            val once = law.step(start, 1.0, 20.0)

            twice.position shouldBe (once.position plusOrMinus 1e-9)
            twice.velocity shouldBe (once.velocity plusOrMinus 1e-6)
        }
    }

    "a time of 0 or less, or NaN, leaves the motion as it is" {
        val motion = Motion(position = 0.3, velocity = 2.0)

        listOf(Smoothing.Exponential(0.5), Smoothing.Linear(1.0), Smoothing.Spring(100.0, 10.0)).forEach { law ->
            law.step(motion, 1.0, 0.0) shouldBe motion
            law.step(motion, 1.0, -5.0) shouldBe motion
            law.step(motion, 1.0, Double.NaN) shouldBe motion
        }
    }

    "Exponential keeps its share of the way per 10 ms and never overshoots" {
        Smoothing.Exponential(0.5).step(Motion(0.0), 1.0, 10.0).position shouldBe (0.5 plusOrMinus 1e-12)
        Smoothing.Exponential(0.5).step(Motion(0.0), 1.0, 20.0).position shouldBe (0.75 plusOrMinus 1e-12)

        run(Smoothing.Exponential(0.9), from = 0.0, goal = 1.0).forEach { it.position shouldBeLessThanOrEqual 1.0 }
        run(Smoothing.Exponential(0.9), from = 1.0, goal = 0.0).last().position shouldBeLessThan 0.001
    }

    "Exponential: a share of 1 or more moves as 0.999, one of 0 or less or NaN lands at once" {
        Smoothing.Exponential(1.0).step(Motion(0.0), 1.0, 10.0).position shouldBe (0.001 plusOrMinus 1e-12)
        Smoothing.Exponential(5.0).step(Motion(0.0), 1.0, 10.0).position shouldBe (0.001 plusOrMinus 1e-12)

        listOf(0.0, -1.0, Double.NaN).forEach { share ->
            Smoothing.Exponential(share).step(Motion(0.0, 3.0), 1.0, 10.0) shouldBe Motion(1.0)
        }
    }

    "Linear moves at its speed, both ways, and lands exactly on the goal" {
        Smoothing.Linear(100.0).step(Motion(0.0), 50.0, 100.0) shouldBe Motion(10.0, 100.0)
        Smoothing.Linear(100.0).step(Motion(0.0), -50.0, 100.0) shouldBe Motion(-10.0, -100.0)
        Smoothing.Linear(100.0).step(Motion(45.0), 50.0, 100.0) shouldBe Motion(50.0)

        Smoothing.Linear(0.0).step(Motion(0.0), 50.0, 1.0) shouldBe Motion(50.0)
        Smoothing.Linear(Double.NaN).step(Motion(0.0), 50.0, 1.0) shouldBe Motion(50.0)
    }

    "AttackRelease rises with up and falls with down" {
        val meter = Smoothing.AttackRelease(up = 0.1, down = 0.9)

        meter.step(Motion(0.0), 1.0, 10.0).position shouldBe (0.9 plusOrMinus 1e-12)
        meter.step(Motion(1.0), 0.0, 10.0).position shouldBe (0.9 plusOrMinus 1e-12)
    }

    "Spring, underdamped: overshoots, swings back and settles on the goal" {
        val needle = Smoothing.Spring(stiffness = 200.0, damping = 10.0)
        val motions = run(needle, from = 0.0, goal = 1.0)

        motions.maxOf { it.position } shouldBeGreaterThan 1.05
        motions.maxOf { it.position } shouldBeLessThan 2.0
        motions.last().position shouldBe (1.0 plusOrMinus 1e-6)
        needle.isSettled(motions.last(), goal = 1.0, near = 0.001).shouldBeTrue()
    }

    "Spring, critically damped and overdamped: no overshoot" {
        listOf(
            Smoothing.Spring(stiffness = 100.0, damping = 20.0),
            Smoothing.Spring(stiffness = 100.0, damping = 50.0),
        ).forEach { spring ->
            val motions = run(spring, from = 0.0, goal = 1.0)

            motions.forEach { it.position shouldBeLessThanOrEqual 1.0 + 1e-9 }
            motions.last().position shouldBe (1.0 plusOrMinus 1e-3)
        }
    }

    "Spring is not settled while it passes the goal at speed" {
        val spring = Smoothing.Spring(stiffness = 200.0, damping = 10.0)

        spring.isSettled(Motion(position = 1.0, velocity = 5.0), goal = 1.0, near = 0.001).shouldBeFalse()
        spring.isSettled(Motion(position = 1.0, velocity = 0.01), goal = 1.0, near = 0.001).shouldBeTrue()
        Smoothing.Exponential(0.5).isSettled(Motion(position = 1.0, velocity = 5.0), 1.0, 0.001).shouldBeTrue()
    }

    "Spring is not settled while its energy can still carry it out of near: a soft spring crossing the goal slowly" {
        // Spring(10, 1) crossing at 0.06/s swings out to about 0.019 more: 19 times near
        val soft = Smoothing.Spring(stiffness = 10.0, damping = 1.0)

        soft.isSettled(Motion(position = 1.0, velocity = 0.06), goal = 1.0, near = 0.001).shouldBeFalse()

        val start = Motion(position = 1.0, velocity = 0.06)
        val furthest = generateSequence(start) { soft.step(it, 1.0, 1000.0 / 60.0) }.take(120).maxOf { it.position }

        furthest shouldBeGreaterThan 1.005
    }

    "Spring: a stiffness or damping of 0 or less, or NaN, lands at once" {
        listOf(0.0 to 10.0, 100.0 to 0.0, -1.0 to 10.0, Double.NaN to 10.0, 100.0 to Double.NaN).forEach { (k, c) ->
            Smoothing.Spring(stiffness = k, damping = c).step(Motion(0.0, 3.0), 1.0, 10.0) shouldBe Motion(1.0)
        }
    }

    "quantize snaps to the nearest offset + k * size; the offset shifts the grid" {
        0.26.quantize(0.1) shouldBe (0.3 plusOrMinus 1e-12)
        0.24.quantize(0.1) shouldBe (0.2 plusOrMinus 1e-12)
        (-0.26).quantize(0.1) shouldBe (-0.3 plusOrMinus 1e-12)
        0.26.quantize(0.1, offset = 0.05) shouldBe (0.25 plusOrMinus 1e-12)
        0.31.quantize(0.1, offset = 0.05) shouldBe (0.35 plusOrMinus 1e-12)
        7.0.quantize(5.0, offset = 1.0) shouldBe 6.0
    }

    "quantize: halfway goes up" {
        0.25.quantize(0.5) shouldBe 0.5
        (-0.25).quantize(0.5) shouldBe 0.0
    }

    "quantize: a size of 0 or less or not finite, an offset not finite, or a value not finite: as it is" {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { size -> 0.26.quantize(size) shouldBe 0.26 }
        0.26.quantize(0.1, offset = Double.NaN) shouldBe 0.26
        Double.POSITIVE_INFINITY.quantize(0.1) shouldBe Double.POSITIVE_INFINITY
        Double.NaN.quantize(0.1).isNaN().shouldBeTrue()
    }

    "quantize: a grid too fine for the value would overflow: the value as it is" {
        1e300.quantize(1e-10) shouldBe 1e300
        1e308.quantize(1.0, offset = -1e308) shouldBe 1e308
    }
})
