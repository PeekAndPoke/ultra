package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.maths.Smoothing.Motion
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
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

    "a law that throws ends the frame, and the next value moves the stream again" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        var throwNext = true
        val failing = object : Smoothing {
            override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion {
                if (throwNext) {
                    throwNext = false
                    error("a law fails once")
                }
                return Smoothing.Linear(unitsPerSecond = 60.0).step(motion, goal, elapsedMs)
            }
        }
        val smooth = source.smooth(smoothing = StreamSource<Smoothing?>(failing), clock = clock)

        smooth.subscribeToStream { }
        source(10.0)
        runCatching { clock.frame(frame) }

        source(20.0)
        clock.waiting shouldBe true
    }

    "a motion that a throwing law stalled goes on when the law changes" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val failing = object : Smoothing {
            override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion = error("a law fails")
        }
        val law = StreamSource<Smoothing?>(failing)
        val smooth = source.smooth(smoothing = law, clock = clock)

        smooth.subscribeToStream { }
        source(10.0)
        runCatching { clock.frame(frame) }
        clock.waiting shouldBe false

        law(Smoothing.Linear(unitsPerSecond = 60.0))

        clock.waiting shouldBe true
        clock.frame(frame)
        smooth()!! shouldBeGreaterThan 0.0
    }

    "a law of null lands a motion that a throwing law stalled" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val failing = object : Smoothing {
            override fun step(motion: Motion, goal: Double, elapsedMs: Double): Motion = error("a law fails")
        }
        val law = StreamSource<Smoothing?>(failing)
        val smooth = source.smooth(smoothing = law, clock = clock)

        smooth.subscribeToStream { }
        source(10.0)
        runCatching { clock.frame(frame) }

        law(null)

        smooth() shouldBe 10.0
        clock.waiting shouldBe false
    }

    "a handler that throws on its first value is not kept, and nothing is left listening" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val law = StreamSource<Smoothing?>(Smoothing.Exponential(keepPer10Ms = 0.5))
        val smooth = source.smooth(smoothing = law, clock = clock)

        runCatching { smooth.subscribeToStream { error("a handler fails") } }.isFailure shouldBe true

        source.subscriptions.size shouldBe 0
        law.subscriptions.size shouldBe 0
    }

    "a source that throws on subscribe leaves nothing half subscribed" {
        val inner = StreamSource<Double?>(0.0)
        var failNext = true
        val source = object : Stream<Double?> {
            override fun invoke(): Double? = inner()

            override fun subscribeToStream(sub: (Double?) -> Unit): Unsubscribe {
                if (failNext) {
                    failNext = false
                    error("a source fails once")
                }
                return inner.subscribeToStream(sub)
            }
        }
        val law = StreamSource<Smoothing?>(null)
        val smooth = source.smooth(smoothing = law, clock = HandClock())

        // The failed subscriber got no unsubscribe: it must not be told anything later
        var leaked = 0
        runCatching { smooth.subscribeToStream { leaked++ } }.isFailure shouldBe true
        law.subscriptions.size shouldBe 0

        var seen: Double? = null
        smooth.subscribeToStream { seen = it }
        inner(5.0)

        seen shouldBe 5.0
        leaked shouldBe 0
        law.subscriptions.size shouldBe 1
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
