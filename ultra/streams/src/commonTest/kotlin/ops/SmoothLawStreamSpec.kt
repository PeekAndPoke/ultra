package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.maths.Smoothing.Motion
import io.peekandpoke.ultra.streams.StreamSource
import kotlin.math.abs

/** `smooth(smoothing: Stream<Smoothing?>)` (ULTRA-00264, from Klang): a law that changes while the stream moves. */
class SmoothLawStreamSpec : StringSpec({

    val frame = 1000.0 / 60.0

    "a new law mid-motion steps on from the current position and velocity" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val exponential = Smoothing.Exponential(keepPer10Ms = 0.5)
        val spring = Smoothing.Spring(stiffness = 100.0, damping = 5.0)
        val law = StreamSource<Smoothing?>(exponential)
        val smooth = source.smooth(smoothing = law, clock = clock)

        smooth.subscribeToStream { }
        source(10.0)

        // The same motion by hand, frame by frame
        var motion = Motion(0.0)
        repeat(3) {
            clock.frame(frame)
            motion = exponential.step(motion, goal = 10.0, elapsedMs = frame)
        }
        smooth()!! shouldBe (motion.position plusOrMinus 1e-9)

        // Started over from the position alone, the speed would be lost
        val restarted = spring.step(Motion(motion.position), goal = 10.0, elapsedMs = frame)

        law(spring)
        clock.frame(frame)
        motion = spring.step(motion, goal = 10.0, elapsedMs = frame)

        smooth()!! shouldBe (motion.position plusOrMinus 1e-9)
        abs(smooth()!! - restarted.position) shouldBeGreaterThan 1e-3
    }

    "a law of null lands a moving stream at once and asks for no more frames" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val law = StreamSource<Smoothing?>(Smoothing.Exponential(keepPer10Ms = 0.5))
        val smooth = source.smooth(smoothing = law, clock = clock)

        smooth.subscribeToStream { }
        source(10.0)
        clock.frame()
        smooth()!! shouldBeLessThan 10.0

        law(null)

        smooth() shouldBe 10.0
        clock.waiting shouldBe false
    }

    "a new law while the stream rests moves nothing and asks for no frame" {
        val clock = HandClock()
        val source = StreamSource<Double?>(3.0)
        val law = StreamSource<Smoothing?>(Smoothing.Exponential(keepPer10Ms = 0.5))
        val smooth = source.smooth(smoothing = law, clock = clock)
        val seen = mutableListOf<Double?>()

        smooth.subscribeToStream { seen.add(it) }
        seen.clear()

        law(Smoothing.Spring(stiffness = 100.0, damping = 5.0))

        seen shouldBe emptyList()
        clock.waiting shouldBe false
    }

    "without a law values pass at once; a law then smooths the next value" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val law = StreamSource<Smoothing?>(null)
        val smooth = source.smooth(smoothing = law, clock = clock)

        smooth.subscribeToStream { }

        source(5.0)
        smooth() shouldBe 5.0
        clock.waiting shouldBe false

        law(Smoothing.Linear(unitsPerSecond = 60.0))
        source(6.0)
        smooth() shouldBe 5.0
        clock.frame(frame)
        smooth()!! shouldBe (6.0 plusOrMinus 1e-9)
    }

    "a stream of a narrower law type is a law stream too (Stream<out Smoothing?>)" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val springs: StreamSource<Smoothing.Spring> = StreamSource(Smoothing.Spring(stiffness = 100.0, damping = 5.0))
        val smooth = source.smooth(smoothing = springs, clock = clock)

        smooth.subscribeToStream { }
        source(1.0)
        clock.frame(frame)

        smooth()!! shouldBeLessThan 1.0
    }

    "the law is listened to only while someone listens" {
        val source = StreamSource<Double?>(0.0)
        val law = StreamSource<Smoothing?>(Smoothing.Exponential(keepPer10Ms = 0.5))
        val smooth = source.smooth(smoothing = law, clock = HandClock())

        law.subscriptions.size shouldBe 0

        val unsubscribe = smooth.subscribeToStream { }
        law.subscriptions.size shouldBe 1

        unsubscribe()
        law.subscriptions.size shouldBe 0
    }
})
