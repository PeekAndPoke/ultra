package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** `ticker` at an interval that is a stream (ULTRA-00271), on hand clocks, one per interval. */
class TickerIntervalStreamSpec : StringSpec({

    fun clocks(): Pair<MutableMap<Int, HandClock>, (Int) -> FrameClock> {
        val made = mutableMapOf<Int, HandClock>()
        return made to { ms: Int -> made.getOrPut(ms) { HandClock() } }
    }

    "the frame that is due keeps its clock, the next one is asked of the new interval's clock" {
        val (made, clockFor) = clocks()
        val interval = StreamSource(100.milliseconds)
        val tick = ticker(interval = interval, clockFor = clockFor)
        val seen = mutableListOf<Int>()

        tick.subscribeToStream { seen.add(it.count) }

        interval(10.milliseconds)
        made.getValue(100).requests shouldBe 1
        made.getValue(10).requests shouldBe 0

        made.getValue(100).frame()
        made.getValue(10).requests shouldBe 1
        made.getValue(100).requests shouldBe 0

        made.getValue(10).frame()
        seen shouldBe listOf(0, 1, 2)
    }

    "a repeat of the same interval keeps its clock; zero or less counts as 1 ms" {
        var created = 0
        val interval = StreamSource(Duration.ZERO)
        val clock = HandClock()
        val tick = ticker(interval = interval, clockFor = { ms -> created++; ms shouldBe 1; clock })

        tick.subscribeToStream { }
        interval((-5).milliseconds)
        interval(1.milliseconds)

        // One clock for all three: each counts as 1 ms
        created shouldBe 1
        clock.requests shouldBe 1
    }

    "the interval is listened to only while someone listens" {
        val (_, clockFor) = clocks()
        val interval = StreamSource(100.milliseconds)
        val tick = ticker(interval = interval, clockFor = clockFor)

        interval.subscriptions.size shouldBe 0

        val unsubscribe = tick.subscribeToStream { }
        interval.subscriptions.size shouldBe 1

        unsubscribe()
        interval.subscriptions.size shouldBe 0
    }
})
