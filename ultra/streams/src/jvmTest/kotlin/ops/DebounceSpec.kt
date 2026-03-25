package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

// Debounce tests require multithreading (the debounce coroutine and the test's delay
// must run concurrently). This doesn't work on JS's single-threaded event loop.
class DebounceSpec : StringSpec({

    "debounce emits a single value after the delay" {

        val source = StreamSource(10)
        val debounced = source.debounce(50)

        val received = mutableListOf<Int>()

        val unsub = debounced.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(10)

        source(20)
        delay(100)

        received shouldBe listOf(10, 20)

        unsub()
    }

    "debounce only emits the last value when multiple arrive within the delay window" {

        val source = StreamSource(0)
        val debounced = source.debounce(50)

        val received = mutableListOf<Int>()

        val unsub = debounced.subscribeToStream {
            received.add(it)
        }

        received shouldBe listOf(0)

        source(1)
        source(2)
        source(3)

        delay(100)

        received shouldBe listOf(0, 3)

        unsub()
    }

    "debounce resets timer on each new value" {

        val source = StreamSource(0)
        val debounced = source.debounce(100)

        val received = mutableListOf<Int>()

        val unsub = debounced.subscribeToStream {
            received.add(it)
        }

        source(1)
        delay(60)
        source(2)
        delay(60)
        source(3)
        delay(150)

        received shouldBe listOf(0, 3)

        unsub()
    }
})
