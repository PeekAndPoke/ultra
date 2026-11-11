package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** `debounce` on a hand-driven timer (ULTRA-00271, ULTRA-00302, ULTRA-00309). */
class DebounceStreamSpec : StringSpec({

    class HandTimer {
        val pending = mutableListOf<Pair<Duration, () -> Unit>>()

        val after: (Duration, () -> Unit) -> () -> Unit = { wait, block ->
            val entry = wait to block
            pending.add(entry)
            val cancel: () -> Unit = { pending.remove(entry) }
            cancel
        }

        fun fire() {
            val run = pending.toList()
            pending.clear()
            run.forEach { it.second() }
        }
    }

    "a new subscriber is told the value once, and no timer runs for it" {
        val timer = HandTimer()
        val source = StreamSource(10)
        val debounced = source.debounce(delay = StreamSource(50.milliseconds), after = timer.after)
        val seen = mutableListOf<Int>()

        debounced.subscribeToStream { seen.add(it) }

        timer.pending.size shouldBe 0
        seen shouldBe listOf(10)
    }

    "only the last value within the window is published; a read gives the last value passed on" {
        val timer = HandTimer()
        val source = StreamSource(0)
        val debounced = source.debounce(delay = StreamSource(50.milliseconds), after = timer.after)
        val seen = mutableListOf<Int>()

        debounced.subscribeToStream { seen.add(it) }
        source(1)
        source(2)

        debounced() shouldBe 0
        timer.pending.size shouldBe 1

        timer.fire()

        seen shouldBe listOf(0, 2)
        debounced() shouldBe 2
    }

    "a new delay applies to the next value, a pending one keeps its own" {
        val timer = HandTimer()
        val source = StreamSource(0)
        val delay = StreamSource(50.milliseconds)
        val debounced = source.debounce(delay = delay, after = timer.after)

        debounced.subscribeToStream { }
        source(1)
        delay(200.milliseconds)

        timer.pending.single().first shouldBe 50.milliseconds

        source(2)
        timer.pending.single().first shouldBe 200.milliseconds
    }

    "a delay of zero or less publishes at once" {
        val timer = HandTimer()
        val source = StreamSource(0)
        val debounced = source.debounce(delay = StreamSource(Duration.ZERO), after = timer.after)
        val seen = mutableListOf<Int>()

        debounced.subscribeToStream { seen.add(it) }
        source(1)

        seen shouldBe listOf(0, 1)
    }

    "the source and the delay are listened to only while someone listens; the pending value is dropped" {
        val timer = HandTimer()
        val source = StreamSource(0)
        val delay = StreamSource(50.milliseconds)
        val debounced = source.debounce(delay = delay, after = timer.after)

        val unsubscribe = debounced.subscribeToStream { }
        source(1)
        delay.subscriptions.size shouldBe 1

        unsubscribe()

        delay.subscriptions.size shouldBe 0
        source.subscriptions.size shouldBe 0
        timer.pending.size shouldBe 0
        debounced() shouldBe 1
    }
})
