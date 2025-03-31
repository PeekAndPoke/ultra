package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CombineSpec : StringSpec({

    "combinedWith" {

        val first = StreamSource(10)
        val second = StreamSource("a")

        val received = mutableListOf<Pair<Int, String>>()

        val combined = first.combinedWith(second) { p1, p2 -> Pair(p1, p2) }

        val unsubscribe = combined.subscribeToStream { received.add(it) }

        received shouldBe listOf(
            Pair(10, "a")
        )

        first(20)

        received shouldBe listOf(
            Pair(10, "a"),
            Pair(20, "a"),
        )

        second("b")

        received shouldBe listOf(
            Pair(10, "a"),
            Pair(20, "a"),
            Pair(20, "b"),
        )

        unsubscribe()
    }

    "pairedWith" {

        val first = StreamSource(10)
        val second = StreamSource("a")

        val received = mutableListOf<Pair<Int, String>>()

        val combined = first.pairedWith(second)

        val unsubscribe = combined.subscribeToStream { received.add(it) }

        received shouldBe listOf(
            Pair(10, "a")
        )

        first(20)

        received shouldBe listOf(
            Pair(10, "a"),
            Pair(20, "a"),
        )

        second("b")

        received shouldBe listOf(
            Pair(10, "a"),
            Pair(20, "a"),
            Pair(20, "b"),
        )

        unsubscribe()
    }
})
