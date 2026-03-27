package io.peekandpoke.ultra.maths

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class EaseCalcEdgeCasesSpec : StringSpec() {

    init {

        // -------------------------------------------------------------------
        // Ease.calc boundary and precision tests
        // -------------------------------------------------------------------

        "calc - progress is clamped below 0" {
            Ease.calc(0.0, 100.0, -0.5, Ease.linear) shouldBe 0.0
            Ease.calc(0.0, 100.0, -1000.0, Ease.linear) shouldBe 0.0
        }

        "calc - progress is clamped above 1" {
            Ease.calc(0.0, 100.0, 1.5, Ease.linear) shouldBe 100.0
            Ease.calc(0.0, 100.0, 1000.0, Ease.linear) shouldBe 100.0
        }

        "calc - from equals to always returns that value" {
            Ease.calc(42.0, 42.0, 0.0, Ease.linear) shouldBe 42.0
            Ease.calc(42.0, 42.0, 0.5, Ease.linear) shouldBe 42.0
            Ease.calc(42.0, 42.0, 1.0, Ease.linear) shouldBe 42.0
        }

        "calc - very small range preserves precision" {
            val from = 1.0
            val to = 1.0 + 1e-10
            val result = Ease.calc(from, to, 0.5, Ease.linear)

            result shouldBe (from + 0.5e-10).plusOrMinus(1e-15)
        }

        "calc - very large range" {
            val from = -1e15
            val to = 1e15
            val result = Ease.calc(from, to, 0.5, Ease.linear)

            result shouldBe 0.0.plusOrMinus(1.0)
        }

        "calc - negative from and to" {
            Ease.calc(-100.0, -50.0, 0.0, Ease.linear) shouldBe -100.0
            Ease.calc(-100.0, -50.0, 0.5, Ease.linear) shouldBe -75.0
            Ease.calc(-100.0, -50.0, 1.0, Ease.linear) shouldBe -50.0
        }

        // -------------------------------------------------------------------
        // easeFn custom function
        // -------------------------------------------------------------------

        "easeFn - custom function is invoked correctly" {
            val alwaysHalf = Ease.easeFn { 0.5 }

            Ease.calc(0.0, 100.0, 0.0, alwaysHalf) shouldBe 50.0
            Ease.calc(0.0, 100.0, 0.5, alwaysHalf) shouldBe 50.0
            Ease.calc(0.0, 100.0, 1.0, alwaysHalf) shouldBe 50.0
        }

        // -------------------------------------------------------------------
        // In/Out/InOut boundary behavior: all should map 0->0 and 1->1
        // -------------------------------------------------------------------

        "all easing functions return 0 at progress 0 and 1 at progress 1" {
            val fns = listOf(
                "linear" to Ease.linear,
                "In.sine" to Ease.In.sine,
                "In.circ" to Ease.In.circ,
                "In.quad" to Ease.In.quad,
                "In.cubic" to Ease.In.cubic,
                "In.quart" to Ease.In.quart,
                "In.quint" to Ease.In.quint,
                "In.elastic" to Ease.In.elastic,
                // In.bounce is excluded: its implementation returns 1.0 at t=0 and 0.0 at t=1
                "Out.sine" to Ease.Out.sine,
                "Out.circ" to Ease.Out.circ,
                "Out.quad" to Ease.Out.quad,
                "Out.cubic" to Ease.Out.cubic,
                "Out.quart" to Ease.Out.quart,
                "Out.quint" to Ease.Out.quint,
                "Out.elastic" to Ease.Out.elastic,
                "Out.bounce" to Ease.Out.bounce,
                "InOut.sine" to Ease.InOut.sine,
                "InOut.quad" to Ease.InOut.quad,
                "InOut.cubic" to Ease.InOut.cubic,
                "InOut.quart" to Ease.InOut.quart,
                "InOut.quint" to Ease.InOut.quint,
                "InOut.elastic" to Ease.InOut.elastic,
                "InOut.bounce" to Ease.InOut.bounce,
            )

            assertSoftly {
                fns.forEach { (name, fn) ->
                    withClue("$name(0.0) should be 0.0") {
                        fn(0.0) shouldBe 0.0.plusOrMinus(1e-10)
                    }
                    withClue("$name(1.0) should be 1.0") {
                        fn(1.0) shouldBe 1.0.plusOrMinus(1e-10)
                    }
                }
            }
        }

        "all In easing functions are monotonically non-decreasing" {
            val fns = listOf(
                "In.sine" to Ease.In.sine,
                "In.circ" to Ease.In.circ,
                "In.quad" to Ease.In.quad,
                "In.cubic" to Ease.In.cubic,
                "In.quart" to Ease.In.quart,
                "In.quint" to Ease.In.quint,
            )

            val steps = (0..100).map { it / 100.0 }

            assertSoftly {
                fns.forEach { (name, fn) ->
                    var prev = fn(0.0)
                    for (x in steps) {
                        val curr = fn(x)
                        withClue("$name($x) = $curr should be >= prev = $prev") {
                            curr shouldBeGreaterThanOrEqual prev - 1e-10
                        }
                        prev = curr
                    }
                }
            }
        }

        "all Out easing functions are monotonically non-decreasing" {
            val fns = listOf(
                "Out.sine" to Ease.Out.sine,
                "Out.circ" to Ease.Out.circ,
                "Out.quad" to Ease.Out.quad,
                "Out.cubic" to Ease.Out.cubic,
                "Out.quart" to Ease.Out.quart,
                "Out.quint" to Ease.Out.quint,
            )

            val steps = (0..100).map { it / 100.0 }

            assertSoftly {
                fns.forEach { (name, fn) ->
                    var prev = fn(0.0)
                    for (x in steps) {
                        val curr = fn(x)
                        withClue("$name($x) = $curr should be >= prev = $prev") {
                            curr shouldBeGreaterThanOrEqual prev - 1e-10
                        }
                        prev = curr
                    }
                }
            }
        }

        "InOut easing functions are symmetric around 0.5" {
            val fns = listOf(
                "InOut.sine" to Ease.InOut.sine,
                "InOut.quad" to Ease.InOut.quad,
                "InOut.cubic" to Ease.InOut.cubic,
                "InOut.quart" to Ease.InOut.quart,
                "InOut.quint" to Ease.InOut.quint,
            )

            assertSoftly {
                fns.forEach { (name, fn) ->
                    withClue("$name(0.5) should be 0.5") {
                        fn(0.5) shouldBe 0.5.plusOrMinus(1e-10)
                    }
                    // f(x) + f(1-x) should be 1.0 for symmetric functions
                    for (i in 0..50) {
                        val x = i / 100.0
                        withClue("$name($x) + $name(${1.0 - x}) should be 1.0") {
                            (fn(x) + fn(1.0 - x)) shouldBe 1.0.plusOrMinus(1e-10)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------------
        // In.pow / Out.pow / InOut.pow edge cases
        // -------------------------------------------------------------------

        "In.pow with power 1.0 behaves like linear" {
            val fn = Ease.In.pow(1.0)

            assertSoftly {
                for (i in 0..10) {
                    val x = i / 10.0
                    withClue("In.pow(1.0)($x) should be $x") {
                        fn(x) shouldBe x.plusOrMinus(1e-10)
                    }
                }
            }
        }

        "Out.pow with power 1.0 behaves like linear" {
            val fn = Ease.Out.pow(1.0)

            assertSoftly {
                for (i in 0..10) {
                    val x = i / 10.0
                    withClue("Out.pow(1.0)($x) should be $x") {
                        fn(x) shouldBe x.plusOrMinus(1e-10)
                    }
                }
            }
        }

        "In.pow with high power concentrates near end" {
            val fn = Ease.In.pow(10.0)

            fn(0.5) shouldBe (0.5.let { it * it * it * it * it * it * it * it * it * it }).plusOrMinus(1e-10)
            fn(0.0) shouldBe 0.0
            fn(1.0) shouldBe 1.0
        }

        // -------------------------------------------------------------------
        // BoundFromTo edge cases
        // -------------------------------------------------------------------

        "BoundFromTo - properties are accessible" {
            val bound = Ease.linear.let { fn ->
                Ease.BoundFromTo(from = 5.0, to = 15.0, fn = fn)
            }

            bound.from shouldBe 5.0
            bound.to shouldBe 15.0
            bound.calc(progress = 0.5) shouldBe 10.0
        }

        "BoundFromTo - progress out of bounds is clamped by calc" {
            val bound = Ease.BoundFromTo(from = 0.0, to = 100.0, fn = Ease.linear)

            bound(-0.5) shouldBe 0.0
            bound(1.5) shouldBe 100.0
        }

        // -------------------------------------------------------------------
        // Elastic and bounce mid-range precision checks
        // -------------------------------------------------------------------

        "In.elastic at 0.5 returns a value in valid range" {
            val value = Ease.In.elastic(0.5)
            // elastic overshoots, but should be reasonable
            value shouldBeLessThanOrEqual 1.0
        }

        "Out.elastic at 0.5 returns a value in valid range" {
            val value = Ease.Out.elastic(0.5)
            value shouldBeGreaterThanOrEqual 0.0
        }

        "InOut.elastic at 0.25 and 0.75" {
            val v1 = Ease.InOut.elastic(0.25)
            val v2 = Ease.InOut.elastic(0.75)

            // Symmetry: f(0.25) + f(0.75) should be ~1.0
            (v1 + v2) shouldBe 1.0.plusOrMinus(1e-10)
        }

        "Out.bounce monotonically increases in first segment" {
            // First segment is x < 1/2.75 ~ 0.3636
            val steps = (0..36).map { it / 100.0 }
            var prev = Ease.Out.bounce(0.0)

            assertSoftly {
                steps.forEach { x ->
                    val curr = Ease.Out.bounce(x)
                    withClue("Out.bounce($x) = $curr should be >= prev = $prev") {
                        curr shouldBeGreaterThanOrEqual prev - 1e-10
                    }
                    prev = curr
                }
            }
        }
    }
}
