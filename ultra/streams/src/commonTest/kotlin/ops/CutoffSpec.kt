package io.peekandpoke.ultra.streams.ops

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe

class CutoffSpec : StringSpec({

    "cutoffWhen publishes source values until predicate becomes true and keeps last value" {

        val source = StreamSource(10)
        val predicate = StreamSource(false)

        val received = mutableListOf<Int>()

        val cutoff = source.cutoffWhen(predicate)

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        source.subscriptions.size shouldBe 1
        predicate.subscriptions.size shouldBe 1

        received shouldBe listOf(10)

        source(20)
        received shouldBe listOf(10, 20)

        predicate(true)
        received shouldBe listOf(10, 20)

        source(30)
        received shouldBe listOf(10, 20)

        predicate(false)
        received shouldBe listOf(10, 20, 30)

        unsubscribe()

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        source(40)
        predicate(true)

        received shouldBe listOf(10, 20, 30)
    }

    "cutoffWhenNot publishes source values until predicate becomes false and keeps last value" {

        val source = StreamSource(10)
        val predicate = StreamSource(true)

        val received = mutableListOf<Int>()

        val cutoff = source.cutoffWhenNot(predicate)

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        source.subscriptions.size shouldBe 1
        predicate.subscriptions.size shouldBe 1

        received shouldBe listOf(10)

        source(20)
        received shouldBe listOf(10, 20)

        predicate(false)
        received shouldBe listOf(10, 20)

        source(30)
        received shouldBe listOf(10, 20)

        predicate(true)
        received shouldBe listOf(10, 20, 30)

        unsubscribe()

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        source(40)
        predicate(false)

        received shouldBe listOf(10, 20, 30)
    }

    "cutoffWhen unsubscribes from source and predicate when there are no subscribers" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val received = mutableListOf<Int>()

        val cutoff = source.cutoffWhen(predicate)

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        source.subscriptions.size shouldBe 1
        predicate.subscriptions.size shouldBe 1
        received shouldBe listOf(1)

        unsubscribe()

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        source(2)
        predicate(true)
        source(3)
        predicate(false)

        received shouldBe listOf(1)
    }

    "cutoffWhen publishes the current value to a first subscriber that arrives while cut off" {

        val source = StreamSource(7)
        val predicate = StreamSource(true)

        val cutoff = source.cutoffWhen(predicate)

        val received = mutableListOf<Int>()

        cutoff.subscribeToStream {
            received.add(it)
        }

        // The Stream contract requires the subscription to be called immediately
        received shouldBe listOf(7)
        source.subscriptions.size shouldBe 0

        val received2 = mutableListOf<Int>()

        cutoff.subscribeToStream {
            received2.add(it)
        }

        received2 shouldBe listOf(7)
    }

    "cutoffWhen does not publish a source value once the cut-off has taken effect" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val received = mutableListOf<Int>()

        // Subscribed to the source first, so it comes before the operator in the notify snapshot
        source.subscribeToStream { value ->
            if (value == 42) {
                predicate(true)
            }
        }

        val cutoff = source.cutoffWhen(predicate)

        cutoff.subscribeToStream {
            received.add(it)
        }

        source(42)

        received shouldBe listOf(1)
        cutoff() shouldBe 1
    }

    "cutoffWhen releases the source when a subscriber cuts off during the source's first value" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val received = mutableListOf<Int>()

        val cutoff = source.cutoffWhen(predicate)

        cutoff.subscribeToStream { value ->
            received.add(value)
            // The very first delivery cuts the stream off
            if (value == 1) {
                predicate(true)
            }
        }

        source.subscriptions.size shouldBe 0

        source(2)

        received shouldBe listOf(1)
    }

    "cutoffWhen releases source and predicate when the last subscriber leaves during a resume" {

        // Starts cut off, so the subscriber owns its unsubscribe handle before any source value
        val source = StreamSource(1)
        val predicate = StreamSource(true)

        var unsubscribe: Unsubscribe? = null

        val cutoff = source.cutoffWhen(predicate)

        unsubscribe = cutoff.subscribeToStream { value ->
            if (value == 2) {
                unsubscribe?.invoke()
            }
        }

        source(2)

        // Lifting the cut-off resubscribes the source, which publishes its current value at once
        predicate(false)

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0
    }

    "cutoffWhen subscribes the predicate once when a subscriber subscribes again on its first value" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val cutoff = source.cutoffWhen(predicate)

        val secondReceived = mutableListOf<Int>()

        cutoff.subscribeToStream {
            if (secondReceived.isEmpty()) {
                cutoff.subscribeToStream { value -> secondReceived.add(value) }
            }
        }

        predicate.subscriptions.size shouldBe 1
        source.subscriptions.size shouldBe 1
        secondReceived shouldBe listOf(1)
    }

    "cutoffWhen subscribes the predicate once when the same handler subscribes twice" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val received = mutableListOf<Int>()
        val handler: (Int) -> Unit = { received.add(it) }

        val cutoff = source.cutoffWhen(predicate)

        val unsubscribe1 = cutoff.subscribeToStream(handler)
        val unsubscribe2 = cutoff.subscribeToStream(handler)

        predicate.subscriptions.size shouldBe 1
        source.subscriptions.size shouldBe 1
        received shouldBe listOf(1, 1)

        // Subscribers are a set of handlers, not of handles: the same instance is ONE subscriber,
        // so it is notified once per value and the first release tears the operator down.
        source(2)
        received shouldBe listOf(1, 1, 2)

        unsubscribe1()

        predicate.subscriptions.size shouldBe 0
        source.subscriptions.size shouldBe 0

        unsubscribe2()

        predicate.subscriptions.size shouldBe 0
        source.subscriptions.size shouldBe 0
    }

    "cutoffWhen starts cut off when the predicate changed before the first subscriber" {

        val source = StreamSource(1)
        val predicate = StreamSource(false)

        val cutoff = source.cutoffWhen(predicate)

        // Flips before anyone subscribes, so the predicate's subscribe-time emission is the
        // only thing that could tell the operator - and it must not be what decides
        predicate(true)

        val received = mutableListOf<Int>()

        val secondReceived = mutableListOf<Int>()

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
            if (secondReceived.isEmpty()) {
                cutoff.subscribeToStream { value -> secondReceived.add(value) }
            }
        }

        predicate.subscriptions.size shouldBe 1
        source.subscriptions.size shouldBe 0
        received shouldBe listOf(1)
        secondReceived shouldBe listOf(1)

        unsubscribe()
    }

    "cutoffWhen subscribes the source once when it restarts after having been cut off" {

        val source = StreamSource(1)

        var subscribes = 0

        val counting = object : Stream<Int> {
            override fun invoke(): Int = source()

            override fun subscribeToStream(sub: (Int) -> Unit): Unsubscribe {
                subscribes++

                return source.subscribeToStream(sub)
            }
        }

        val predicate = StreamSource(false)

        val cutoff = counting.cutoffWhen(predicate)

        val unsubscribe = cutoff.subscribeToStream { }

        subscribes shouldBe 1

        predicate(true)
        unsubscribe()

        // Resumed while nobody is subscribed, so the operator restarts with a stale cut-off state
        predicate(false)

        val unsubscribe2 = cutoff.subscribeToStream { }

        // The predicate's subscribe-time emission must not drive a second subscription
        subscribes shouldBe 2
        source.subscriptions.size shouldBe 1

        unsubscribe2()

        source.subscriptions.size shouldBe 0
    }

    "cutoffWhen starts once when subscribing the predicate reenters the cutoff stream" {

        val source = StreamSource(1)
        val flag = StreamSource(false)

        var cutoff: Stream<Int>? = null
        var reentered = false

        val received = mutableListOf<Int>()

        val predicate = object : Stream<Boolean> {
            override fun invoke(): Boolean = flag()

            override fun subscribeToStream(sub: (Boolean) -> Unit): Unsubscribe {
                val unsubscribe = flag.subscribeToStream(sub)

                // Reenters while the operator is still establishing this very subscription
                if (!reentered) {
                    reentered = true
                    cutoff?.subscribeToStream { received.add(it) }
                }

                return unsubscribe
            }
        }

        val stream = source.cutoffWhen(predicate)
        cutoff = stream

        stream.subscribeToStream { }

        // The operator must not start twice and orphan the first predicate subscription
        flag.subscriptions.size shouldBe 1
        source.subscriptions.size shouldBe 1

        // Accepted cost of reentering: a subscriber added while start() is still running is
        // already in the set when start() publishes, so it sees the value a second time
        received shouldBe listOf(1, 1)
    }

    "cutoffWhen releases the predicate when the source cannot be subscribed" {

        val flag = StreamSource(false)

        val failing = object : Stream<Int> {
            override fun invoke(): Int = 1

            override fun subscribeToStream(sub: (Int) -> Unit): Unsubscribe = error("cannot subscribe")
        }

        val stream = failing.cutoffWhen(flag)

        shouldThrow<IllegalStateException> {
            stream.subscribeToStream { }
        }

        flag.subscriptions.size shouldBe 0
    }

    "cutoffWhen does not resubscribe the source when the predicate fires after teardown" {

        val source = StreamSource(1)
        val predicate = StreamSource(true)

        val cutoff = source.cutoffWhen(predicate)

        var unsubscribe: Unsubscribe? = null

        // Registered before the operator, so it runs earlier in the predicate's notify snapshot
        predicate.subscribeToStream { value ->
            if (!value) {
                unsubscribe?.invoke()
                unsubscribe = null
            }
        }

        unsubscribe = cutoff.subscribeToStream { }

        // Lifting the cut-off tears the operator down before its own predicate handler runs
        predicate(false)

        source.subscriptions.size shouldBe 0
        // Only the handler this test registered itself is left
        predicate.subscriptions.size shouldBe 1
    }

    "cutoffWhenNot unsubscribes from source and predicate when there are no subscribers" {

        val source = StreamSource(1)
        val predicate = StreamSource(true)

        val received = mutableListOf<Int>()

        val cutoff = source.cutoffWhenNot(predicate)

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        source.subscriptions.size shouldBe 1
        predicate.subscriptions.size shouldBe 1
        received shouldBe listOf(1)

        unsubscribe()

        source.subscriptions.size shouldBe 0
        predicate.subscriptions.size shouldBe 0

        source(2)
        predicate(false)
        source(3)
        predicate(true)

        received shouldBe listOf(1)
    }
})
