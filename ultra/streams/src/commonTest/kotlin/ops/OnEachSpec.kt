package io.peekandpoke.ultra.streams.ops

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.StreamSource

class OnEachSpec : StringSpec({

    "onEach block is called for each value" {

        val source = StreamSource(10)
        val sideEffects = mutableListOf<Int>()

        val stream = source.onEach { sideEffects.add(it) }

        val unsub = stream.subscribeToStream { }

        source(20)
        source(30)

        sideEffects shouldBe listOf(10, 20, 30)

        unsub()
    }

    "onEach does not modify the value (pass-through)" {

        val source = StreamSource(10)

        val stream = source.onEach { /* side effect only */ }

        val received = mutableListOf<Int>()

        val unsub = stream.subscribeToStream { received.add(it) }

        source(20)

        received shouldBe listOf(10, 20)

        unsub()
    }

    "multiple chained onEach blocks execute in order" {

        val source = StreamSource(1)
        val log = mutableListOf<String>()

        val stream = source
            .onEach { log.add("first:$it") }
            .onEach { log.add("second:$it") }

        val unsub = stream.subscribeToStream { }

        source(2)

        log shouldBe listOf("first:1", "second:1", "first:2", "second:2")

        unsub()
    }
})
