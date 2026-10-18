package io.peekandpoke.ultra.streams.ops

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe

private class Player(val diagnostics: Stream<Int>)

/** Records subscribe/unsubscribe events to prove subscription ordering */
private class TrackingStream<T>(
    private val wrapped: Stream<T>,
    private val events: MutableList<String>,
    private val name: String,
) : Stream<T> {
    override fun invoke(): T = wrapped()

    override fun subscribeToStream(sub: (T) -> Unit): Unsubscribe {
        events.add("subscribe:$name")

        val unsubscribe = wrapped.subscribeToStream(sub)

        return {
            events.add("unsubscribe:$name")
            unsubscribe()
        }
    }
}

class SwitchMapSpec : StringSpec({

    "switchMap publishes the current inner value exactly once on first subscribe" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        outer.subscriptions.size shouldBe 0
        inner1.subscriptions.size shouldBe 0

        val unsubscribe = stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        outer.subscriptions.size shouldBe 1
        inner1.subscriptions.size shouldBe 1
        inner2.subscriptions.size shouldBe 0

        unsubscribe()
    }

    "switchMap follows emissions of the current inner stream" {

        val inner1 = StreamSource(10)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        stream.subscribeToStream {
            received.add(it)
        }

        inner1(11)
        inner1(12)

        received shouldBe listOf(10, 11, 12)
    }

    "switchMap switches inner streams when the outer value changes" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        outer(inner2)

        received shouldBe listOf(10, 20)
        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 1

        inner1(99)
        received shouldBe listOf(10, 20)

        inner2(21)
        received shouldBe listOf(10, 20, 21)
    }

    "switchMap unsubscribes the old inner before subscribing the new one" {

        val events = mutableListOf<String>()

        val inner1 = TrackingStream(StreamSource(10), events, "inner1")
        val inner2 = TrackingStream(StreamSource(20), events, "inner2")
        val outer = StreamSource<Stream<Int>>(inner1)

        val stream = outer.switchMap { it }

        stream.subscribeToStream { }

        events shouldBe listOf("subscribe:inner1")

        outer(inner2)

        events shouldBe listOf("subscribe:inner1", "unsubscribe:inner1", "subscribe:inner2")
    }

    "switchMap keeps the subscription and stays silent when the same inner instance is selected again" {

        val events = mutableListOf<String>()

        val diag = StreamSource(5)
        val trackedDiag = TrackingStream(diag, events, "diag")

        val playerA = Player(diagnostics = trackedDiag)
        val playerB = Player(diagnostics = trackedDiag)

        val outer = StreamSource(playerA)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it.diagnostics }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(5)

        outer(playerB)

        received shouldBe listOf(5)
        events shouldBe listOf("subscribe:diag")
        diag.subscriptions.size shouldBe 1
    }

    "switchMap invoke computes fresh values while nobody is subscribed" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val stream = outer.switchMap { it }

        stream() shouldBe 10

        inner1(11)
        stream() shouldBe 11

        outer(inner2)
        stream() shouldBe 20

        outer.subscriptions.size shouldBe 0
        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 0
    }

    "switchMap invoke returns the latest value while subscribed" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val stream = outer.switchMap { it }

        stream.subscribeToStream { }

        stream() shouldBe 10

        inner1(11)
        stream() shouldBe 11

        outer(inner2)
        stream() shouldBe 20
    }

    "switchMap releases outer and inner streams when the last subscriber leaves" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        val unsubscribe = stream.subscribeToStream {
            received.add(it)
        }

        outer.subscriptions.size shouldBe 1
        inner1.subscriptions.size shouldBe 1
        received shouldBe listOf(10)

        unsubscribe()

        outer.subscriptions.size shouldBe 0
        inner1.subscriptions.size shouldBe 0

        inner1(99)
        outer(inner2)

        received shouldBe listOf(10)
        inner2.subscriptions.size shouldBe 0
    }

    "switchMap rebuilds from the current outer value on re-subscribe" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val stream = outer.switchMap { it }

        val received1 = mutableListOf<Int>()
        val unsubscribe = stream.subscribeToStream {
            received1.add(it)
        }

        received1 shouldBe listOf(10)

        unsubscribe()

        // Everything changes while nobody is subscribed
        inner1(11)
        outer(inner2)
        inner2(21)

        val received2 = mutableListOf<Int>()
        stream.subscribeToStream {
            received2.add(it)
        }

        received2 shouldBe listOf(21)

        outer.subscriptions.size shouldBe 1
        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 1
    }

    "switchMap does not run the selector at construction and propagates its exceptions from invoke" {

        val outer = StreamSource(1)

        var calls = 0

        val stream = outer.switchMap<Int, Int> {
            calls++
            error("boom")
        }

        calls shouldBe 0

        shouldThrow<IllegalStateException> {
            stream()
        }

        calls shouldBe 1
    }

    "switchMap propagates selector exceptions from an unsubscribed invoke" {

        val inner = StreamSource(10)
        val outer = StreamSource(1)

        val stream = outer.switchMap { value ->
            if (value < 0) error("boom") else inner
        }

        stream() shouldBe 10

        outer(-1)

        shouldThrow<IllegalStateException> {
            stream()
        }
    }

    "switchMap keeps the previous inner active when the selector throws during an outer emission" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource(1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { value ->
            when {
                value < 0 -> error("boom")
                value < 100 -> inner1
                else -> inner2
            }
        }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        // The selector throws inside the outer stream's notifyHandlers, which contains the error
        outer(-1)

        received shouldBe listOf(10)
        inner1.subscriptions.size shouldBe 1

        // The cached value is returned - computing it fresh would throw the selector's exception
        stream() shouldBe 10

        inner1(11)
        received shouldBe listOf(10, 11)
        stream() shouldBe 11

        outer(100)

        received shouldBe listOf(10, 11, 20)
        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 1
    }

    "switchMapNotNull publishes null while the outer value is null" {

        val diag = StreamSource(5)
        val outer = StreamSource<Player?>(null)

        val received = mutableListOf<Int?>()

        val stream = outer.switchMapNotNull { it.diagnostics }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(null)
        diag.subscriptions.size shouldBe 0
        stream() shouldBe null
    }

    "switchMapNotNull switches null to player to null" {

        val diag = StreamSource(5)
        val player = Player(diagnostics = diag)
        val outer = StreamSource<Player?>(null)

        val received = mutableListOf<Int?>()

        val stream = outer.switchMapNotNull { it.diagnostics }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(null)

        outer(player)

        received shouldBe listOf(null, 5)
        diag.subscriptions.size shouldBe 1

        diag(6)
        received shouldBe listOf(null, 5, 6)

        outer(null)

        received shouldBe listOf(null, 5, 6, null)
        diag.subscriptions.size shouldBe 0
        stream() shouldBe null
    }

    "switchMapNotNull stays silent when null follows null" {

        val outer = StreamSource<Player?>(null)

        val received = mutableListOf<Int?>()

        val stream = outer.switchMapNotNull { it.diagnostics }

        stream.subscribeToStream {
            received.add(it)
        }

        outer(null)

        received shouldBe listOf(null)
    }

    "switchMapNotNull keeps the inner subscription when the player changes but the diagnostics stream stays the same" {

        val diag = StreamSource(5)
        val playerA = Player(diagnostics = diag)
        val playerB = Player(diagnostics = diag)

        val outer = StreamSource<Player?>(playerA)

        val received = mutableListOf<Int?>()

        val stream = outer.switchMapNotNull { it.diagnostics }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(5)

        outer(playerB)

        received shouldBe listOf(5)
        diag.subscriptions.size shouldBe 1
    }

    "switchMapNotNull invoke computes fresh values while nobody is subscribed" {

        val diag = StreamSource(5)
        val player = Player(diagnostics = diag)
        val outer = StreamSource<Player?>(null)

        val stream = outer.switchMapNotNull { it.diagnostics }

        stream() shouldBe null

        outer(player)
        stream() shouldBe 5

        diag(6)
        stream() shouldBe 6

        outer.subscriptions.size shouldBe 0
        diag.subscriptions.size shouldBe 0
    }

    "switchMapNotNull composes with fallbackTo for a non-null result" {

        val diag = StreamSource(5)
        val player = Player(diagnostics = diag)
        val outer = StreamSource<Player?>(null)

        val received = mutableListOf<Int>()

        val stream = outer.switchMapNotNull { it.diagnostics }.fallbackTo(-1)

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(-1)

        outer(player)
        received shouldBe listOf(-1, 5)

        diag(6)
        received shouldBe listOf(-1, 5, 6)

        outer(null)
        received shouldBe listOf(-1, 5, 6, -1)
    }

    "A failing subscriber must not prevent other subscribers from being notified" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        // As everywhere in the library, the immediate call to a new subscriber is not protected
        shouldThrow<IllegalStateException> {
            stream.subscribeToStream {
                error("boom")
            }
        }

        stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        inner1(11)
        received shouldBe listOf(10, 11)

        outer(inner2)
        received shouldBe listOf(10, 11, 20)
    }

    "switchMap does not leak the outer subscription when the selector throws while subscribing" {

        val inner = StreamSource(10)
        val outer = StreamSource(1)

        val stream = outer.switchMap { value ->
            if (value < 0) error("boom") else inner
        }

        outer(-1)

        shouldThrow<IllegalStateException> {
            stream.subscribeToStream { }
        }

        outer.subscriptions.size shouldBe 0
        inner.subscriptions.size shouldBe 0

        // The operator recovers once the selector works again
        outer(1)

        val received = mutableListOf<Int>()

        val unsubscribe = stream.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)
        outer.subscriptions.size shouldBe 1

        unsubscribe()

        outer.subscriptions.size shouldBe 0
        inner.subscriptions.size shouldBe 0
    }

    "switchMap releases the new inner stream when the last subscriber leaves during a switch" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        var unsubscribe: Unsubscribe? = null

        unsubscribe = stream.subscribeToStream { value ->
            received.add(value)
            if (value == 20) {
                unsubscribe?.invoke()
            }
        }

        outer(inner2)

        received shouldBe listOf(10, 20)

        outer.subscriptions.size shouldBe 0
        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 0

        inner2(21)
        received shouldBe listOf(10, 20)
    }

    "switchMap releases every inner stream when a subscriber switches the outer during a switch" {

        val inner1 = StreamSource(10)
        val inner2 = StreamSource(20)
        val inner3 = StreamSource(30)
        val outer = StreamSource<Stream<Int>>(inner1)

        val received = mutableListOf<Int>()

        val stream = outer.switchMap { it }

        stream.subscribeToStream { value ->
            received.add(value)
            if (value == 20) {
                outer(inner3)
            }
        }

        outer(inner2)

        received shouldBe listOf(10, 20, 30)

        inner1.subscriptions.size shouldBe 0
        inner2.subscriptions.size shouldBe 0
        inner3.subscriptions.size shouldBe 1

        // The de-selected stream must not reach the subscribers any more
        inner2(999)
        received shouldBe listOf(10, 20, 30)
    }

    "switchMap subscribes the outer once when a subscriber subscribes again on its first value" {

        val inner = StreamSource(10)
        val outer = StreamSource(1)

        val stream = outer.switchMap { inner }

        val secondReceived = mutableListOf<Int>()

        stream.subscribeToStream {
            if (secondReceived.isEmpty()) {
                stream.subscribeToStream { value -> secondReceived.add(value) }
            }
        }

        outer.subscriptions.size shouldBe 1
        inner.subscriptions.size shouldBe 1
        secondReceived shouldBe listOf(10)
    }

    "switchMap starts once when subscribing the outer stream reenters the switchMap stream" {

        val inner = StreamSource(10)
        val outerSource = StreamSource(1)

        var stream: Stream<Int>? = null
        var reentered = false

        val received = mutableListOf<Int>()

        val outer = object : Stream<Int> {
            override fun invoke(): Int = outerSource()

            override fun subscribeToStream(sub: (Int) -> Unit): Unsubscribe {
                val unsubscribe = outerSource.subscribeToStream(sub)

                // Reenters while the operator is still establishing this very subscription
                if (!reentered) {
                    reentered = true
                    stream?.subscribeToStream { received.add(it) }
                }

                return unsubscribe
            }
        }

        val switched = outer.switchMap { inner }
        stream = switched

        switched.subscribeToStream { }

        // The operator must not start twice and orphan the first outer subscription
        outerSource.subscriptions.size shouldBe 1
        inner.subscriptions.size shouldBe 1

        // Accepted cost of reentering: a subscriber added while start() is still running is
        // already in the set when the first switch publishes, so it sees the value again
        received shouldBe listOf(10, 10)
    }

    "switchMap subscribes the outer once when the same handler subscribes twice" {

        val inner = StreamSource(10)
        val outer = StreamSource(1)

        val received = mutableListOf<Int>()
        val handler: (Int) -> Unit = { received.add(it) }

        val stream = outer.switchMap { inner }

        val unsubscribe1 = stream.subscribeToStream(handler)
        val unsubscribe2 = stream.subscribeToStream(handler)

        outer.subscriptions.size shouldBe 1
        inner.subscriptions.size shouldBe 1
        received shouldBe listOf(10, 10)

        unsubscribe1()
        unsubscribe2()

        outer.subscriptions.size shouldBe 0
        inner.subscriptions.size shouldBe 0
    }

    "switchMap keeps running for the remaining subscribers when one of them unsubscribes" {

        val inner = StreamSource(10)
        val outer = StreamSource(1)

        val received1 = mutableListOf<Int>()
        val received2 = mutableListOf<Int>()

        val stream = outer.switchMap { inner }

        val unsubscribe1 = stream.subscribeToStream {
            received1.add(it)
        }

        stream.subscribeToStream {
            received2.add(it)
        }

        unsubscribe1()

        outer.subscriptions.size shouldBe 1
        inner.subscriptions.size shouldBe 1

        inner(11)

        received1 shouldBe listOf(10)
        received2 shouldBe listOf(10, 11)
    }

    "switchMap can switch again after subscribing to an inner stream failed" {

        var attempts = 0

        val healthy = StreamSource(10)

        val failing = object : Stream<Int> {
            override fun invoke(): Int = 99

            override fun subscribeToStream(sub: (Int) -> Unit): Unsubscribe {
                attempts++
                error("cannot subscribe")
            }
        }

        val outer = StreamSource<Stream<Int>>(healthy)

        val stream = outer.switchMap { it }

        stream.subscribeToStream { }

        outer(failing)
        attempts shouldBe 1

        outer(failing)
        attempts shouldBe 2
    }
})
