package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.steady

/** `smooth` with `rest` and `near` as streams (ULTRA-00271). */
class SmoothConfigStreamSpec : StringSpec({

    val law = steady<Smoothing?>(Smoothing.Exponential(keepPer10Ms = 0.5))

    "a new rest applies to the next motion that starts from null" {
        val clock = HandClock()
        val source = StreamSource<Double?>(1.0)
        val rest = StreamSource(0.0)
        val smooth = source.smooth(smoothing = law, rest = rest, clock = clock)
        val seen = mutableListOf<Double?>()

        smooth.subscribeToStream { seen.add(it) }
        source(null)
        rest(5.0)
        source(10.0)

        // The motion towards 10 starts from the new rest
        seen.last() shouldBe 5.0
    }

    "a rest that is not finite counts as 0" {
        val clock = HandClock()
        val source = StreamSource<Double?>(1.0)
        val smooth = source.smooth(smoothing = law, rest = steady(Double.NaN), clock = clock)
        val seen = mutableListOf<Double?>()

        smooth.subscribeToStream { seen.add(it) }
        source(null)
        source(10.0)

        seen.last() shouldBe 0.0
    }

    "a new near applies from the next frame: a wide one lands the motion at once" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val near = StreamSource(0.001)
        val smooth = source.smooth(smoothing = law, rest = steady(0.0), near = near, clock = clock)

        smooth.subscribeToStream { }
        source(10.0)
        clock.frame()
        (smooth()!! < 10.0) shouldBe true

        near(100.0)
        clock.frame()

        smooth() shouldBe 10.0
        clock.waiting shouldBe false
    }

    "rest and near are listened to only while someone listens" {
        val source = StreamSource<Double?>(0.0)
        val rest = StreamSource(0.0)
        val near = StreamSource(0.001)
        val smooth = source.smooth(smoothing = law, rest = rest, near = near, clock = HandClock())

        val unsubscribe = smooth.subscribeToStream { }
        rest.subscriptions.size shouldBe 1
        near.subscriptions.size shouldBe 1

        unsubscribe()
        rest.subscriptions.size shouldBe 0
        near.subscriptions.size shouldBe 0
    }

    "a new subscriber is told the first value once (ULTRA-00309)" {
        val source = StreamSource<Double?>(1.0)
        val smooth = source.smooth(smoothing = law, rest = steady(0.0), clock = HandClock())
        val seen = mutableListOf<Double?>()

        smooth.subscribeToStream { seen.add(it) }

        seen shouldBe listOf(1.0)
    }
})
