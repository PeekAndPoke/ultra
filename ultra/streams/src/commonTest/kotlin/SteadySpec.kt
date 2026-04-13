package io.peekandpoke.ultra.streams

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SteadySpec : StringSpec({

    "steady() must return the value on invoke()" {
        val stream = steady(42)

        stream() shouldBe 42
    }

    "steady() must immediately call subscribers with the value" {
        val stream = steady("hello")

        var received: String? = null
        stream.subscribeToStream { received = it }

        received shouldBe "hello"
    }

    "steady() unsubscribe must be a no-op" {
        val stream = steady(1)

        val unsub = stream.subscribeToStream {}
        unsub()
        unsub()

        stream() shouldBe 1
    }

    "steady() must support nullable values" {
        val stream = steady<String?>(null)

        stream() shouldBe null

        var received: String? = "not-null"
        stream.subscribeToStream { received = it }
        received shouldBe null
    }
})
