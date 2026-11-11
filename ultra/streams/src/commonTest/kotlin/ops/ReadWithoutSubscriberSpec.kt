package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource

/** `invoke()` gives the current value also while nobody listens (ULTRA-00302, from Klang's KLANG-00422). */
class ReadWithoutSubscriberSpec : StringSpec({

    "distinct gives the source's value after it changed, never having been listened to" {
        val source = StreamSource(1)
        val stream = source.distinct()

        source(2)

        stream() shouldBe 2
    }

    "distinctStrict gives the source's value after it changed, never having been listened to" {
        val source = StreamSource("a")
        val stream = source.distinctStrict()

        source("b")

        stream() shouldBe "b"
    }

    "distinct gives the source's value after the last subscriber left and the source changed" {
        val source = StreamSource(1)
        val stream = source.distinct()

        val unsubscribe = stream.subscribeToStream { }
        source(2)
        unsubscribe()

        source(3)

        stream() shouldBe 3
    }

    "cutoffWhen gives the source's value while not cut off, never having been listened to" {
        val source = StreamSource(1)
        val predicate = StreamSource(false)
        val stream = source.cutoffWhen(predicate)

        source(2)

        stream() shouldBe 2
    }

    "cutoffWhen holds the value last read when the cut-off begins without a subscriber" {
        val source = StreamSource(1)
        val predicate = StreamSource(false)
        val stream = source.cutoffWhen(predicate)

        source(2)
        stream() shouldBe 2

        predicate(true)
        source(3)

        stream() shouldBe 2
    }

    "cutoffWhen gives a first subscriber arriving while cut off the value last read" {
        val source = StreamSource(1)
        val predicate = StreamSource(false)
        val stream = source.cutoffWhen(predicate)

        source(2)
        stream() shouldBe 2
        predicate(true)
        source(3)

        val received = mutableListOf<Int>()
        stream.subscribeToStream { received.add(it) }

        received shouldBe listOf(2)
    }

    "cutoffWhen gives the source's value after the last subscriber left and the source changed" {
        val source = StreamSource(1)
        val predicate = StreamSource(false)
        val stream = source.cutoffWhen(predicate)

        val unsubscribe = stream.subscribeToStream { }
        unsubscribe()

        source(2)

        stream() shouldBe 2
    }

    "cutoffWhenNot gives the source's value while not cut off, and holds it once cut off" {
        val source = StreamSource(1)
        val predicate = StreamSource(true)
        val stream = source.cutoffWhenNot(predicate)

        source(2)
        stream() shouldBe 2

        predicate(false)
        source(3)

        stream() shouldBe 2
    }

    "filter gives the source's value when it passes, never having been listened to" {
        val source = StreamSource(1)
        val even = source.filter { it % 2 == 0 }

        source(2)
        even() shouldBe 2

        source(3)
        even() shouldBe 2
    }

    "filter with an initial value gives the source's value when it passes, never having been listened to" {
        val source = StreamSource(1)
        val even = source.filter(0) { it % 2 == 0 }

        even() shouldBe 0

        source(4)
        even() shouldBe 4
    }

    "indexed counts nothing on a read while nobody listens" {
        val indexed = StreamSource("a").indexed()
        val seen = mutableListOf<Pair<Int, String>>()

        indexed() shouldBe Pair(0, "a")
        indexed() shouldBe Pair(0, "a")

        indexed.subscribeToStream { seen.add(it) }

        seen shouldBe listOf(Pair(0, "a"))
    }

    "filter on a nullable stream: a passing null is the value of a read without a subscriber" {
        val source = StreamSource<String?>("abcd")
        val filtered = source.filter { it == null || it.length > 3 }

        filtered() shouldBe "abcd"

        source(null)

        filtered() shouldBe null
    }
})
