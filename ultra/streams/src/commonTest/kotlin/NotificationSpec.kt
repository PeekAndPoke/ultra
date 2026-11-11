package io.peekandpoke.ultra.streams

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.ops.HandClock
import io.peekandpoke.ultra.streams.ops.combinedWith
import io.peekandpoke.ultra.streams.ops.distinct
import io.peekandpoke.ultra.streams.ops.map
import io.peekandpoke.ultra.streams.ops.onEach
import io.peekandpoke.ultra.streams.ops.sampled
import io.peekandpoke.ultra.streams.ops.switchMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Values published during a notification, handlers removed during one (ULTRA-00307). */
class NotificationSpec : StringSpec({

    "a value set by a handler is not overtaken by the older one: a source" {
        val source = StreamSource(0)
        source.subscribeToStream { if (it < 0) source(0) }
        var shown = 99
        source.subscribeToStream { shown = it }

        source(-5)

        shown shouldBe 0
    }

    "a value set by a handler is not overtaken by the older one: a map and a distinct after it" {
        val source = StreamSource(0)
        source.subscribeToStream { if (it < 0) source(0) }
        val mapped = source.map { it }
        val distinct = mapped.distinct()
        var shownMapped = 99
        var shownDistinct = 99
        mapped.subscribeToStream { shownMapped = it }
        distinct.subscribeToStream { shownDistinct = it }

        source(-5)

        mapped() shouldBe 0
        shownMapped shouldBe 0
        shownDistinct shouldBe 0
    }

    "a value set by a handler is not overtaken by the older one: a combinator" {
        val source = StreamSource(0)
        val combined = source.combinedWith(StreamSource(1)) { a, b -> a + b }
        combined.subscribeToStream { if (it < 0) source(0) }
        var shown = 99
        combined.subscribeToStream { shown = it }

        source(-5)

        shown shouldBe 1
    }

    "a handler removed during a notification is not called with that value" {
        val source = StreamSource(0)
        lateinit var second: Unsubscribe
        val seen = mutableListOf<Int>()
        source.subscribeToStream { if (it == 1) second() }
        second = source.subscribeToStream { seen.add(it) }

        source(1)

        seen shouldBe listOf(0)
    }

    "sampled keeps ticking after its last subscriber left during a notification of its interval" {
        val clocks = mutableMapOf<Duration, HandClock>()
        val every = StreamSource(100.milliseconds)
        val sampled = StreamSource(1).sampled(every = every, clockFor = { clocks.getOrPut(it) { HandClock() } })

        lateinit var leave: Unsubscribe
        every.subscribeToStream { if (it == 200.milliseconds) leave() }
        leave = sampled.subscribeToStream { }
        every(200.milliseconds)

        sampled.subscribeToStream { }

        clocks.getValue(200.milliseconds).requests shouldBe 1
    }

    "switchMap reads fresh after its last subscriber left during a notification of the inner stream" {
        val inner = StreamSource("a")
        val switched = StreamSource(0).switchMap { inner }

        lateinit var leave: Unsubscribe
        inner.subscribeToStream { if (it == "b") leave() }
        leave = switched.subscribeToStream { }
        inner("b")
        inner("c")

        switched() shouldBe "c"
    }

    "an onEach block that writes upstream does not leave its subscribers on the older value" {
        val source = StreamSource(0)
        val clamped = source.onEach { if (it < 0) source(0) }
        var shown = 99
        clamped.subscribeToStream { shown = it }

        source(-5)

        shown shouldBe 0
        clamped() shouldBe 0
    }
})
