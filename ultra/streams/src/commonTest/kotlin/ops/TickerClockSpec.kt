package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** `ticker(clock)` (ULTRA-00242) on a hand-driven clock: frames, times and the clock's lifetime. */
class TickerClockSpec : StringSpec({

    "a frame of the clock is a TickerFrame with its time and the time since the last" {
        val clock = HandClock()
        val tick = ticker(clock)
        val seen = mutableListOf<TickerFrame>()

        tick.subscribeToStream { seen.add(it) }

        clock.frame(10.0)
        clock.frame(16.5)

        seen shouldBe listOf(
            TickerFrame(count = 0, currentTime = 0.0, deltaTime = 0.0),
            TickerFrame(count = 1, currentTime = 10.0, deltaTime = 0.0),
            TickerFrame(count = 2, currentTime = 26.5, deltaTime = 16.5),
        )
        clock.requests shouldBe 1
    }

    "the last listener leaving stops the frames; listened to again, the time since the last starts anew" {
        val clock = HandClock()
        val tick = ticker(clock)
        val seen = mutableListOf<TickerFrame>()

        val first = tick.subscribeToStream {}
        clock.frame(10.0)
        first()

        clock.waiting shouldBe false

        tick.subscribeToStream { seen.add(it) }
        clock.frame(50.0)

        seen.last() shouldBe TickerFrame(count = 2, currentTime = 60.0, deltaTime = 0.0)
        clock.requests shouldBe 1
    }

    "a subscribe while a frame is told asks for no second frame" {
        val clock = HandClock()
        val tick = ticker(clock)
        var joined = false

        tick.subscribeToStream {
            if (!joined && it.count == 1) {
                joined = true
                tick.subscribeToStream {}
            }
        }

        clock.frame()

        clock.requests shouldBe 1
    }

    "the only listener leaving while a frame is told stops the frames" {
        val clock = HandClock()
        val tick = ticker(clock)

        lateinit var leave: () -> Unit
        leave = tick.subscribeToStream { if (it.count == 1) leave() }

        clock.frame()

        clock.waiting shouldBe false
    }

    "the only listener leaving and a new one coming while a frame is told: one chain of frames" {
        val clock = HandClock()
        val tick = ticker(clock)

        lateinit var leave: () -> Unit
        leave = tick.subscribeToStream {
            if (it.count == 1) {
                leave()
                tick.subscribeToStream {}
            }
        }

        clock.frame()

        clock.requests shouldBe 1
    }
})
