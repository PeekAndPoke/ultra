package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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
