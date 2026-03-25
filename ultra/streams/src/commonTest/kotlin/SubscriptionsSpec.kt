package de.peekandpoke.ultra.streams

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SubscriptionsSpec : StringSpec({

    "subscribe adds a managed subscription" {

        val source = StreamSource(10)
        val subs = Subscriptions()

        val received = mutableListOf<Int>()
        subs.subscribe(source) { received.add(it) }

        subs.size shouldBe 1
        source.subscriptions.size shouldBe 1

        source(20)
        received shouldBe listOf(10, 20)
    }

    "unsubscribeAll cleans up all subscriptions" {

        val source1 = StreamSource(1)
        val source2 = StreamSource(2)
        val subs = Subscriptions()

        subs.subscribe(source1) { }
        subs.subscribe(source2) { }

        subs.size shouldBe 2
        source1.subscriptions.size shouldBe 1
        source2.subscriptions.size shouldBe 1

        subs.unsubscribeAll()

        subs.size shouldBe 0
        source1.subscriptions.size shouldBe 0
        source2.subscriptions.size shouldBe 0
    }

    "plusAssign operator works" {

        val source = StreamSource(10)
        val subs = Subscriptions()

        subs += source.subscribeToStream { }

        subs.size shouldBe 1
        source.subscriptions.size shouldBe 1

        subs.unsubscribeAll()

        subs.size shouldBe 0
        source.subscriptions.size shouldBe 0
    }

    "unsubscribeAll can be called multiple times safely" {

        val source = StreamSource(10)
        val subs = Subscriptions()

        subs.subscribe(source) { }

        subs.unsubscribeAll()
        subs.unsubscribeAll()

        subs.size shouldBe 0
        source.subscriptions.size shouldBe 0
    }
})
