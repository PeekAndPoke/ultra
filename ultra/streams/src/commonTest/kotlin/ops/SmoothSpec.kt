package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlin.math.pow

/**
 * A source that has its value only once someone listens, as a filtered stream does: read before that, it is null.
 */
private class LateStream(private val value: Double) : Stream<Double?> {
    private var subscribed = false

    override fun invoke(): Double? = if (subscribed) value else null

    override fun subscribeToStream(sub: (Double?) -> Unit): Unsubscribe {
        subscribed = true
        sub(value)

        return { subscribed = false }
    }
}

/**
 * `Stream.smooth()` (ULTRA-00242): the motion, its pace, offline and the clock's lifetime. The rows up to the
 * `Smoothing` ones come from Klang's KLANG-00345.
 */
class SmoothSpec : StringSpec({

    "a new value is approached a step a frame, lands exactly on it, and then asks for no more frames" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)
        val seen = mutableListOf<Double?>()

        smooth.subscribeToStream { seen.add(it) }

        source(1.0)

        // Not moved before its first frame
        smooth() shouldBe 0.0

        clock.frame()

        smooth()!! shouldBeGreaterThan 0.0
        smooth()!! shouldBeLessThan 1.0

        repeat(100) { clock.frame() }

        smooth() shouldBe 1.0
        clock.waiting shouldBe false
    }

    "the pace follows the time, not the frames: two 10 ms frames go as far as one of 20 ms" {
        fun after(vararg frames: Double): Double {
            val clock = HandClock()
            val source = StreamSource<Double?>(0.0)
            val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

            smooth.subscribeToStream {}
            source(1.0)
            frames.forEach { clock.frame(it) }

            return smooth()!!
        }

        // The first frame counts as a usual frame; after it, the time between frames sizes the step
        after(16.0, 10.0, 10.0) shouldBe (after(16.0, 20.0) plusOrMinus 1e-12)

        // A step of 10 ms keeps half of the way left
        val firstStep = after(16.0)

        after(16.0, 10.0) shouldBe ((1.0 - (1.0 - firstStep) * 0.5) plusOrMinus 1e-12)
        firstStep shouldBe ((1.0 - 0.5.pow((1000.0 / 60.0) / 10.0)) plusOrMinus 1e-12)
    }

    "a value arriving on the way becomes the new goal, and the motion goes on to it" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        smooth.subscribeToStream {}
        source(1.0)
        clock.frame()
        source(-1.0)

        repeat(200) { clock.frame() }

        smooth() shouldBe -1.0
        clock.waiting shouldBe false
    }

    "null, a NaN and an infinity pass at once as null and stop the motion; after them it starts from rest" {
        for (gone in listOf<Double?>(null, Double.NaN, Double.POSITIVE_INFINITY)) {
            val clock = HandClock()
            val source = StreamSource<Double?>(0.5)
            val smooth = source.smooth(sharePer10Ms = 0.5, rest = 0.0, clock = clock)

            smooth.subscribeToStream {}
            source(1.0)
            clock.frame()
            source(gone)

            smooth() shouldBe null
            clock.waiting shouldBe false

            // Online again: from rest, not from where it was
            source(1.0)

            smooth() shouldBe 0.0

            clock.frame()

            smooth()!! shouldBeLessThan 1.0
        }
    }

    "a share of 0 passes every value at once" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.0, clock = clock)

        smooth.subscribeToStream {}
        source(0.7)

        smooth() shouldBe 0.7
        clock.waiting shouldBe false
    }

    "the source is listened to only while someone listens, and the motion stops when the last one goes" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        val unsubscribe = smooth.subscribeToStream {}

        source(1.0)

        clock.waiting shouldBe true

        unsubscribe()

        clock.waiting shouldBe false

        // Nobody listens: a new value moves nothing
        source(0.3)

        clock.waiting shouldBe false
    }

    "the first value after someone starts listening is taken as it is: a needle shown anew starts at its reading" {
        val clock = HandClock()
        val smooth = LateStream(0.7).smooth(sharePer10Ms = 0.5, clock = clock)

        smooth.subscribeToStream {}

        smooth() shouldBe 0.7
        clock.waiting shouldBe false
    }

    "after a null the motion starts from rest, wherever rest is" {
        val clock = HandClock()
        val source = StreamSource<Double?>(1.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, rest = 0.25, clock = clock)

        smooth.subscribeToStream {}
        source(null)
        source(1.0)

        smooth() shouldBe 0.25
    }

    "with two listeners, the first one leaving keeps the stream moving for the other" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        val first = smooth.subscribeToStream {}
        smooth.subscribeToStream {}

        first()
        source(1.0)

        clock.waiting shouldBe true

        repeat(200) { clock.frame() }

        smooth() shouldBe 1.0
    }

    "listened to again after everyone left, it moves again" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        smooth.subscribeToStream {}()
        smooth.subscribeToStream {}

        source(1.0)

        clock.waiting shouldBe true

        repeat(200) { clock.frame() }

        smooth() shouldBe 1.0
    }

    "a share of 1 or more counts as the slowest there is: it still moves towards the value, never away" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 1.5, clock = clock)

        smooth.subscribeToStream {}
        source(1.0)
        clock.frame()

        smooth()!! shouldBeGreaterThan 0.0
        smooth()!! shouldBeLessThan 1.0
    }

    "listened to again after everyone left, a new value is taken as it is: no glide from where it stopped" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        val first = smooth.subscribeToStream {}

        source(1.0)
        clock.frame()
        first()
        source(0.4)

        smooth.subscribeToStream {}

        smooth() shouldBe 0.4
        clock.waiting shouldBe false
    }

    "a listener leaving while it is told a value stops the frames" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        lateinit var leave: () -> Unit

        leave = smooth.subscribeToStream { v -> if (v != null && v > 0.0 && v < 1.0) leave() }

        source(1.0)
        clock.frame()

        clock.waiting shouldBe false
    }

    "a listener that writes a new value back into the source while it is told one starts no second chain of frames" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)
        var wroteBack = false

        smooth.subscribeToStream { v ->
            if (!wroteBack && v != null && v > 0.0 && v < 1.0) {
                wroteBack = true
                source(2.0)
            }
        }

        source(1.0)
        clock.frame()

        clock.requests shouldBe 1
    }

    "read while nobody listens, it gives the source's value as it is now" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)

        val only = smooth.subscribeToStream {}

        source(1.0)
        clock.frame()
        only()

        smooth() shouldBe 1.0
    }

    "a Spring swings past the value, comes back and lands exactly on it" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(Smoothing.Spring(stiffness = 200.0, damping = 10.0), clock = clock)
        val seen = mutableListOf<Double>()

        smooth.subscribeToStream { it?.let(seen::add) }
        source(1.0)

        repeat(1000) { clock.frame() }

        seen.max() shouldBeGreaterThan 1.05
        smooth() shouldBe 1.0
        clock.waiting shouldBe false
    }

    "a Spring keeps its speed when the goal moves on the way" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(Smoothing.Spring(stiffness = 200.0, damping = 10.0), clock = clock)

        smooth.subscribeToStream {}
        source(1.0)
        repeat(3) { clock.frame() }

        val before = smooth()!!

        // Heading back to where it started: it still moves up on the next frame, carried by its speed
        source(0.0)
        clock.frame()

        smooth()!! shouldBeGreaterThan before
    }

    "Linear moves at its speed and lands exactly on the value" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(Smoothing.Linear(unitsPerSecond = 6.0), clock = clock)

        smooth.subscribeToStream {}
        source(1.0)
        clock.frame(10.0)
        clock.frame(10.0)

        // The first frame counts as a usual frame (1/60 s), the second as its 10 ms
        smooth()!! shouldBe ((6.0 / 60.0 + 0.06) plusOrMinus 1e-12)

        repeat(100) { clock.frame() }

        smooth() shouldBe 1.0
        clock.waiting shouldBe false
    }

    "a law whose parameters make no sense lands on its first frame" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(Smoothing.Spring(stiffness = 100.0, damping = 0.0), clock = clock)

        smooth.subscribeToStream {}
        source(1.0)
        clock.frame()

        smooth() shouldBe 1.0
        clock.waiting shouldBe false
    }

    "a goal far beyond the default near's floating-point step lands and stops asking for frames" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.9, clock = clock)

        smooth.subscribeToStream {}
        source(1e15)

        repeat(2000) { if (clock.waiting) clock.frame() }

        smooth() shouldBe 1e15
        clock.waiting shouldBe false
    }

    "a listener that sets a new value just as the stream lands: the motion goes on to it" {
        val clock = HandClock()
        val source = StreamSource<Double?>(0.0)
        val smooth = source.smooth(sharePer10Ms = 0.5, clock = clock)
        var next = false

        smooth.subscribeToStream { v ->
            if (!next && v == 1.0) {
                next = true
                source(2.0)
            }
        }

        source(1.0)
        repeat(400) { if (clock.waiting) clock.frame() }

        smooth() shouldBe 2.0
        clock.waiting shouldBe false
    }
})
