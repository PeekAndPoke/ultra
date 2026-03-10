package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.StreamSource
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class CutoffTickerSpec : StringSpec({

    "cutoffWhen with ticker stops producing values after unsubscribe" {

        val predicate = StreamSource(false)
        val received = mutableListOf<Long>()

        // We keep track of the ticker values, so we can see if the ticker keeps producing values or not
        var lastTickerValue = 0L
        val cutoff = ticker(20)
            .onEach { lastTickerValue = it }
            .cutoffWhen(predicate)

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        delay(90)

        val tickerValWhileSubscribed = lastTickerValue
        val receivedWhileSubscribed = received.size

        withClue("The cutoff stream should have produced values while subscribed") {
            receivedWhileSubscribed shouldBe received.size
        }

        unsubscribe()

        withClue("The predicate stream should have no subscriptions after unsubscribe") {
            predicate.subscriptions.size shouldBe 0
        }

        val receivedAtUnsubscribe = received.size

        delay(90)

        withClue("The cutoff stream must not emit more values after unsubscribe") {
            received.size shouldBe receivedAtUnsubscribe
        }

        withClue("The ticker must stop producing values after unsubscribe") {
            tickerValWhileSubscribed shouldBe lastTickerValue
        }
    }

    "cutoffWhen with ticker stops the ticker while predicate is true and resumes when predicate becomes false again" {

        val predicate = StreamSource(false)
        val received = mutableListOf<Long>()

        var lastTickerValue = 0L
        val cutoff = ticker(20)
            .onEach { lastTickerValue = it }
            .cutoffWhen(predicate)

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        delay(90)

        val lastReceivedBeforeCutoff = received.last()
        val tickerValueBeforeCutoff = lastTickerValue

        predicate(true)

        delay(90)

        withClue("The cutoff stream must keep publishing the last value while predicate is true") {
            received.last() shouldBe lastReceivedBeforeCutoff
        }

        withClue("The ticker must stop advancing while predicate is true") {
            lastTickerValue shouldBe tickerValueBeforeCutoff
        }

        predicate(false)

        delay(90)

        withClue("The cutoff stream must resume and publish the current ticker value when predicate becomes false again") {
            received.last() shouldBe lastTickerValue
        }

        unsubscribe()
    }

    "cutoffWhenNot with ticker stops producing values after unsubscribe" {

        val predicate = StreamSource(true)
        val received = mutableListOf<Long>()

        val cutoff = ticker(20).cutoffWhenNot(predicate)

        val unsubscribe = cutoff.subscribeToStream {
            received.add(it)
        }

        delay(90)

        val receivedWhileSubscribed = received.size

        withClue("The cutoffWhenNot stream should have produced values while subscribed") {
            receivedWhileSubscribed shouldBe received.size
        }

        unsubscribe()

        withClue("The predicate stream should have no subscriptions after unsubscribe") {
            predicate.subscriptions.size shouldBe 0
        }

        val receivedAtUnsubscribe = received.size

        delay(90)

        withClue("The cutoffWhenNot stream must not emit more values after unsubscribe") {
            received.size shouldBe receivedAtUnsubscribe
        }
    }
})
