package io.peekandpoke.ultra.streams

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.ops.combinedWith
import io.peekandpoke.ultra.streams.ops.cutoffWhen
import io.peekandpoke.ultra.streams.ops.fold
import io.peekandpoke.ultra.streams.ops.HandClock
import io.peekandpoke.ultra.streams.ops.map
import io.peekandpoke.ultra.streams.ops.sampled
import io.peekandpoke.ultra.streams.ops.smooth
import io.peekandpoke.ultra.streams.ops.switchMap

class HandlerResilienceSpec : StringSpec({

    "One handler given twice is two subscriptions: the first to leave does not end the other (ULTRA-00274)" {
        val noop: (Int) -> Unit = {}
        val subscribeTwice: List<Pair<String, (Stream<Int>) -> Pair<Unsubscribe, Unsubscribe>>> = listOf(
            "the same handler" to { s -> s.subscribeToStream(noop) to s.subscribeToStream(noop) },
            "the default handler" to { s -> s.subscribeToStream() to s.subscribeToStream() },
        )

        subscribeTwice.forEach { (name, twice) ->
            val base = StreamSource(0)
            // Computed only while listened to: once nobody listens, it stops summing
            val sum = base.fold(0) { acc, n -> acc + n }
            val mapped = sum.map { it }

            val (first, second) = twice(mapped)
            first()
            base(5)

            withClue(name) { mapped() shouldBe 5 }
            second()
        }
    }

    "One handler given twice keeps every operator up until both leave" {
        val noop: (Int) -> Unit = {}
        val operators: List<Pair<String, (Stream<Int>) -> Stream<Int>>> = listOf(
            "map" to { s -> s.map { it } },
            "combinedWith" to { s -> s.combinedWith(StreamSource(0)) { a, b -> a + b } },
            "switchMap" to { s -> s.switchMap { StreamSource(it) } },
            "cutoffWhen" to { s -> s.cutoffWhen(StreamSource(false)) },
            "sampled" to { s -> s.sampled(HandClock()) },
        )

        // smooth is a Stream<Double?>: the same check, with a handler of that type
        val smoothSource = StreamSource(1)
        val smooth = smoothSource.map<Int, Double?> { it.toDouble() }.smooth(smoothing = StreamSource(null), clock = HandClock())
        val noopDouble: (Double?) -> Unit = {}
        val firstSmooth = smooth.subscribeToStream(noopDouble)
        val secondSmooth = smooth.subscribeToStream(noopDouble)
        firstSmooth()
        withClue("smooth after the first left") { smoothSource.subscriptions.size shouldBe 1 }
        secondSmooth()
        withClue("smooth after both left") { smoothSource.subscriptions.size shouldBe 0 }

        operators.forEach { (name, op) ->
            val source = StreamSource(1)
            val stream = op(source)

            val first = stream.subscribeToStream(noop)
            val second = stream.subscribeToStream(noop)
            first()

            withClue("$name after the first left") { source.subscriptions.size shouldBe 1 }
            second()
            withClue("$name after both left") { source.subscriptions.size shouldBe 0 }
        }
    }

    "A handler that throws on its first value is not kept: neither by a source nor by a wrapper, nor upstream" {
        val source = StreamSource(10)
        var called = 0

        runCatching { source.subscribeToStream { called++; error("I fail at once") } }.isFailure shouldBe true
        source(20)
        called shouldBe 1
        source.subscriptions.size shouldBe 0

        val mapped = source.map { it * 2 }
        var mappedCalls = 0
        runCatching { mapped.subscribeToStream { mappedCalls++; error("I fail at once") } }.isFailure shouldBe true
        source(30)
        mappedCalls shouldBe 1
        // The wrapper let go of its source too: nobody listens to it any more
        source.subscriptions.size shouldBe 0
    }

    "A handler that throws on its first value is not kept by combinedWith, cutoffWhen or switchMap either" {
        val operators: List<Pair<String, (Stream<Int>) -> Stream<Int>>> = listOf(
            "combinedWith" to { s -> s.combinedWith(StreamSource(0)) { a, b -> a + b } },
            "cutoffWhen" to { s -> s.cutoffWhen(StreamSource(false)) },
            "switchMap" to { s -> s.switchMap { StreamSource(it) } },
        )

        operators.forEach { (name, op) ->
            val source = StreamSource(1)
            var calls = 0

            runCatching { op(source).subscribeToStream { calls++; error("I fail at once") } }.isFailure shouldBe true
            source(2)

            withClue(name) {
                calls shouldBe 1
                source.subscriptions.size shouldBe 0
            }
        }
    }

    "A failing handler on StreamSource must not prevent other handlers from being notified" {

        val source = StreamSource(10)
        val received = mutableListOf<Int>()

        // This handler throws, but only on values after the initial subscribe
        var throwOnNext = false
        source.subscribeToStream {
            if (throwOnNext) throw RuntimeException("I fail")
        }

        source.subscribeToStream {
            received.add(it)
        }

        throwOnNext = true
        source(20)
        source(30)

        received shouldBe listOf(10, 20, 30)
    }

    "A failing handler on a mapped stream must not prevent other handlers from being notified" {

        val source = StreamSource(10)
        val mapped = source.map { it * 2 }
        val received = mutableListOf<Int>()

        var throwOnNext = false
        mapped.subscribeToStream {
            if (throwOnNext) throw RuntimeException("I fail")
        }

        mapped.subscribeToStream {
            received.add(it)
        }

        throwOnNext = true
        source(20)
        source(30)

        received shouldBe listOf(20, 40, 60)
    }

    "Multiple failing handlers still allow healthy handlers to receive values" {

        val source = StreamSource(1)
        val received = mutableListOf<Int>()

        var throwOnNext = false
        source.subscribeToStream { if (throwOnNext) throw RuntimeException("fail 1") }
        source.subscribeToStream { received.add(it) }
        source.subscribeToStream { if (throwOnNext) throw RuntimeException("fail 2") }
        source.subscribeToStream { received.add(it * 10) }

        throwOnNext = true
        source(2)

        received shouldBe listOf(1, 10, 2, 20)
    }
})
