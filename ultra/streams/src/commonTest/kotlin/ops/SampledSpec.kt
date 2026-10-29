package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** `sampled(...)` (ULTRA-00267, from Klang): the latest value at every tick of a clock. */
class SampledSpec : StringSpec({

    "one value per tick, whatever the source's rate, repeats and null included" {
        val clock = HandClock()
        val source = StreamSource<Int?>(1)
        val seen = mutableListOf<Int?>()

        source.sampled(clock).subscribeToStream { seen.add(it) }
        seen.clear()

        // Many values between two ticks: only the latest is taken
        source(2)
        source(3)
        seen shouldBe emptyList()

        clock.frame()
        clock.frame()
        source(null)
        clock.frame()

        seen shouldBe listOf(3, 3, null)
    }

    "with history: one entry per tick" {
        val clock = HandClock()
        val source = StreamSource(0)
        val history = source.sampled(clock).history(capacity = 3)
        var last: List<Int> = emptyList()

        history.subscribeToStream { last = it }

        repeat(5) { n ->
            repeat(10) { source(n * 10 + it) }
            clock.frame()
        }

        last shouldBe listOf(29, 39, 49)
    }

    "a subscriber is told the value as it is when it starts listening" {
        val source = StreamSource(1)
        val sampled = source.sampled(HandClock())

        source(7)

        var first: Int? = null
        sampled.subscribeToStream { if (first == null) first = it }

        first shouldBe 7
    }

    "source and clock are held only while someone listens" {
        val clock = HandClock()
        val source = StreamSource(1)
        val sampled = source.sampled(clock)

        source.subscriptions.size shouldBe 0
        clock.waiting shouldBe false

        val unsubscribe = sampled.subscribeToStream { }
        source.subscriptions.size shouldBe 1
        clock.requests shouldBe 1

        unsubscribe()
        source.subscriptions.size shouldBe 0
        clock.waiting shouldBe false
    }

    "a new interval re-times the ticks on a clock of its own; an infinite one stops them" {
        val clocks = mutableMapOf<Duration, HandClock>()
        val every = StreamSource(100.milliseconds)
        val source = StreamSource(1)
        val seen = mutableListOf<Int>()

        source.sampled(every = every, clockFor = { clocks.getOrPut(it) { HandClock() } })
            .subscribeToStream { seen.add(it) }
        seen.clear()

        clocks.getValue(100.milliseconds).frame()
        seen shouldBe listOf(1)

        every(250.milliseconds)
        clocks.getValue(100.milliseconds).waiting shouldBe false
        clocks.getValue(250.milliseconds).requests shouldBe 1

        source(2)
        clocks.getValue(250.milliseconds).frame()
        seen shouldBe listOf(1, 2)

        every(Duration.INFINITE)
        clocks.getValue(250.milliseconds).waiting shouldBe false
        seen shouldBe listOf(1, 2)
    }

    "a config stream repeating the same interval keeps the tick that is due, on the same clock" {
        val clocks = mutableListOf<HandClock>()
        val every = StreamSource(250.milliseconds)
        val source = StreamSource(1)
        val seen = mutableListOf<Int>()

        source.sampled(every = every, clockFor = { HandClock().also { clocks.add(it) } })
            .subscribeToStream { seen.add(it) }
        seen.clear()

        repeat(5) { every(250.milliseconds) }
        clocks.size shouldBe 1

        clocks[0].frame()
        seen shouldBe listOf(1)
    }

    "an interval longer than Int.MAX_VALUE ms is clamped, not wrapped to a negative one" {
        30.days.inWholeIntervalMs() shouldBe Int.MAX_VALUE
        (-5).milliseconds.inWholeIntervalMs() shouldBe 0
    }

    "an interval of zero or less counts as 1 ms" {
        val asked = mutableListOf<Duration>()

        StreamSource(1).sampled(every = StreamSource(Duration.ZERO), clockFor = { asked.add(it); HandClock() })
            .subscribeToStream { }

        asked shouldBe listOf(1.milliseconds)
    }
})
