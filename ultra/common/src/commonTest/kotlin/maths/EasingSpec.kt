package de.peekandpoke.ultra.maths

import de.peekandpoke.ultra.common.maths.Ease
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class EasingSpec : StringSpec({

    "linear easing should return correct value" {
        Ease.linear(0.0) shouldBe 0.0
        Ease.linear(0.5) shouldBe 0.5
        Ease.linear(1.0) shouldBe 1.0
    }

    "ease-in sine should return correct value" {
        Ease.In.sine(0.0) shouldBe 0.0
        Ease.In.sine(0.5) shouldBe 0.2928932188134524.plusOrMinus(0.001)
        Ease.In.sine(1.0) shouldBe 1.0.plusOrMinus(0.001)
    }

    "ease-in quad should return correct value" {
        Ease.In.quad(0.0) shouldBe 0.0
        Ease.In.quad(0.5) shouldBe 0.25
        Ease.In.quad(1.0) shouldBe 1.0
    }

    "ease-in cubic should return correct value" {
        Ease.In.cubic(0.0) shouldBe 0.0
        Ease.In.cubic(0.5) shouldBe 0.125
        Ease.In.cubic(1.0) shouldBe 1.0
    }

    "ease-in quart should return correct value" {
        Ease.In.quart(0.0) shouldBe 0.0
        Ease.In.quart(0.5) shouldBe 0.0625
        Ease.In.quart(1.0) shouldBe 1.0
    }

    "ease-in quint should return correct value" {
        Ease.In.quint(0.0) shouldBe 0.0
        Ease.In.quint(0.5) shouldBe 0.03125
        Ease.In.quint(1.0) shouldBe 1.0
    }

    "ease-in circ should return correct value" {
        Ease.In.circ(0.0) shouldBe 0.0
        Ease.In.circ(0.5) shouldBe 0.1339745962155614.plusOrMinus(0.001)
        Ease.In.circ(1.0) shouldBe 1.0
    }

    "ease-in bounce should return correct value" {
        listOf(
            0.0 to 1.0,
            0.1 to 0.9881249999999999,
            0.2 to 0.94,
            0.3 to 0.930625,
            0.4 to 0.7725,
            0.5 to 0.765625,
            0.6 to 0.9099999999999998,
            0.7 to 0.6806250000000001,
            0.8 to 0.3024999999999999,
            0.9 to 0.07562499999999997,
            1.0 to 0.0,
        ).forEach { (input, expected) ->
            withClue("ease-in bounce($input) should be $expected") {
                Ease.In.bounce(input) shouldBe expected.plusOrMinus(0.001)
            }
        }
    }

    "ease-out sine should return correct value" {
        Ease.Out.sine(0.0) shouldBe 0.0
        Ease.Out.sine(0.5) shouldBe 0.7071067811865475.plusOrMinus(0.001)
        Ease.Out.sine(1.0) shouldBe 1.0
    }

    "ease-out quad should return correct value" {
        Ease.Out.quad(0.0) shouldBe 0.0
        Ease.Out.quad(0.5) shouldBe 0.75
        Ease.Out.quad(1.0) shouldBe 1.0
    }

    "ease-out cubic should return correct value" {
        Ease.Out.cubic(0.0) shouldBe 0.0
        Ease.Out.cubic(0.5) shouldBe 0.875
        Ease.Out.cubic(1.0) shouldBe 1.0
    }

    "ease-out quart should return correct value" {
        Ease.Out.quart(0.0) shouldBe 0.0
        Ease.Out.quart(0.5) shouldBe 0.9375
        Ease.Out.quart(1.0) shouldBe 1.0
    }

    "ease-out quint should return correct value" {
        Ease.Out.quint(0.0) shouldBe 0.0
        Ease.Out.quint(0.5) shouldBe 0.96875
        Ease.Out.quint(1.0) shouldBe 1.0
    }

    "ease-out circ should return correct value" {
        Ease.Out.circ(0.0) shouldBe 0.0
        Ease.Out.circ(0.5) shouldBe 0.8660254037844386
        Ease.Out.circ(1.0) shouldBe 1.0
    }

    "ease-out bounce should return correct value" {
        listOf(
            0.0 to 0.0,
            0.1 to 0.07562500000000001,
            0.2 to 0.30250000000000005,
            0.3 to 0.6806249999999999,
            0.4 to 0.9099999999999998,
            0.5 to 0.765625,
            0.6 to 0.7725,
            0.7 to 0.930625,
            0.8 to 0.94,
            0.9 to 0.9881249999999999,
            1.0 to 1.0,
        ).forEach { (input, expected) ->
            withClue("ease-out bounce($input) should be $expected") {
                Ease.Out.bounce(input) shouldBe expected.plusOrMinus(0.001)
            }
        }
    }

    "ease-inout sine should return correct value" {
        Ease.InOut.sine(0.0) shouldBe 0.0.plusOrMinus(0.001)
        Ease.InOut.sine(0.5) shouldBe 0.5.plusOrMinus(0.001)
        Ease.InOut.sine(1.0) shouldBe 1.0.plusOrMinus(0.001)
    }

    "ease-inout quad should return correct value" {
        Ease.InOut.quad(0.0) shouldBe 0.0
        Ease.InOut.quad(0.5) shouldBe 0.5
        Ease.InOut.quad(1.0) shouldBe 1.0
    }

    "ease-inout cubic should return correct value" {
        Ease.InOut.cubic(0.0) shouldBe 0.0
        Ease.InOut.cubic(0.5) shouldBe 0.5
        Ease.InOut.cubic(1.0) shouldBe 1.0
    }

    "ease-inout quart should return correct value" {
        Ease.InOut.quart(0.0) shouldBe 0.0
        Ease.InOut.quart(0.5) shouldBe 0.5
        Ease.InOut.quart(1.0) shouldBe 1.0
    }

    "ease-inout quint should return correct value" {
        Ease.InOut.quint(0.0) shouldBe 0.0
        Ease.InOut.quint(0.5) shouldBe 0.5
        Ease.InOut.quint(1.0) shouldBe 1.0
    }

    "ease-inout bounce should return correct value" {
        listOf(
            0.0 to 0.0,
            0.1 to 0.030000000000000027,
            0.2 to 0.11375000000000002,
            0.3 to 0.045000000000000095,
            0.4 to 0.34875000000000006,
            0.5 to 0.5,
            0.6 to 0.6512499999999999,
            0.7 to 0.9550000000000001,
            0.8 to 0.88625,
            0.9 to 0.97,
            1.0 to 1.0,
        ).forEach { (input, expected) ->
            withClue("ease-inout bounce($input) should be $expected") {
                Ease.InOut.bounce(input) shouldBe expected.plusOrMinus(0.001)
            }
        }
    }

    "elastic functions should return correct values" {
        Ease.In.elastic(0.0) shouldBe 0.0
        Ease.In.elastic(1.0) shouldBe 1.0

        Ease.Out.elastic(0.0) shouldBe 0.0
        Ease.Out.elastic(1.0) shouldBe 1.0

        Ease.InOut.elastic(0.0) shouldBe 0.0
        Ease.InOut.elastic(0.5) shouldBe 0.5
        Ease.InOut.elastic(1.0) shouldBe 1.0
    }

    "calc function should interpolate correctly" {
        Ease.calc(0.0, 100.0, 0.5, Ease.linear) shouldBe 50.0
        Ease.calc(0.0, 100.0, 0.0, Ease.linear) shouldBe 0.0
        Ease.calc(0.0, 100.0, 1.0, Ease.linear) shouldBe 100.0
    }
})
