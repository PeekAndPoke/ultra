package de.peekandpoke.ultra.streams.ops

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class TickerSpec : StringSpec({

    "ticker emits TickerFrames with incrementing count" {

        val received = mutableListOf<TickerFrame>()

        val unsubscribe = ticker(20).subscribeToStream {
            received.add(it)
        }

        delay(90)

        withClue("Ticker should have emitted multiple values") {
            received.size shouldBeGreaterThan 1
        }

        withClue("First frame should have count 0 and deltaTime 0") {
            received.first().count shouldBe 0
            received.first().deltaTime shouldBe 0.0
        }

        withClue("Ticker count should be incrementing") {
            received.zipWithNext().forEach { (a, b) ->
                b.count shouldBeGreaterThan a.count
            }
        }

        unsubscribe()
    }

    "ticker stops emitting after unsubscribe" {

        val received = mutableListOf<TickerFrame>()

        val unsubscribe = ticker(20).subscribeToStream {
            received.add(it)
        }

        delay(90)

        unsubscribe()

        val countAtUnsubscribe = received.size

        delay(90)

        withClue("Ticker must not emit after unsubscribe") {
            received.size shouldBe countAtUnsubscribe
        }
    }

    "animTicker emits TickerFrame with deltaTime 0 on first frame" {

        val received = mutableListOf<TickerFrame>()

        val unsubscribe = animTicker().subscribeToStream {
            received.add(it)
        }

        // First emission is the initial value (before any rAF fires)
        withClue("Initial frame should have count 0 and deltaTime 0") {
            received.first().count shouldBe 0
            received.first().deltaTime shouldBe 0.0
        }

        delay(100)

        withClue("AnimTicker should have emitted frames") {
            received.size shouldBeGreaterThan 1
        }

        unsubscribe()
    }

    "animTicker deltaTime is positive after first frame" {

        val received = mutableListOf<TickerFrame>()

        val unsubscribe = animTicker().subscribeToStream {
            received.add(it)
        }

        delay(100)

        // Skip initial value (index 0) and the first rAF frame (index 1, deltaTime may be 0 since previousTime starts null)
        // From index 2 onward, deltaTime should be positive
        val framesWithDelta = received.drop(2)

        withClue("Frames after the first should have positive deltaTime") {
            framesWithDelta.forEach {
                it.deltaTime shouldBeGreaterThan 0.0
            }
        }

        unsubscribe()
    }

    "animTicker stops emitting after unsubscribe" {

        val received = mutableListOf<TickerFrame>()

        val unsubscribe = animTicker().subscribeToStream {
            received.add(it)
        }

        delay(100)

        unsubscribe()

        val countAtUnsubscribe = received.size

        delay(100)

        withClue("AnimTicker must not emit after unsubscribe") {
            received.size shouldBe countAtUnsubscribe
        }
    }
})
