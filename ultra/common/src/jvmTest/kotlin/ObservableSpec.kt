package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ObservableSpec : StringSpec() {

    class Obs : Observable<Int> {
        val subs = Observable.Subscriptions<Int>()

        private var counter = 0

        fun emit() = subs.emit(counter++)

        override fun observe(block: OnChange<Int>): Unsubscribe = subs.observe(block)
    }

    init {
        "Subscribing to an Observable must work" {
            val obs = Obs()
            val received = mutableListOf<Int>()
            val unsub = obs.observe { received.add(it) }

            obs.emit()

            received shouldBe listOf(0)

            unsub()
        }

        "After unsubscribing no new values must be received" {
            val obs = Obs()
            val received = mutableListOf<Int>()
            val unsub = obs.observe { received.add(it) }

            obs.emit()
            obs.emit()
            unsub()
            obs.emit()
            obs.emit()

            received shouldBe listOf(0, 1)
        }

        "Unsubscribing twice is harmless" {
            val obs = Obs()
            val received = mutableListOf<Int>()
            val unsub = obs.observe { received.add(it) }

            unsub()
            unsub()
            obs.emit()

            received shouldBe emptyList()
            obs.subs.numSubscriptions() shouldBe 0
        }

        "A subscription survives until it is cancelled" {
            val obs = Obs()
            val received = mutableListOf<Int>()

            // Nothing owns this subscription any more, yet it keeps firing: the callback is held
            // strongly and only Unsubscribe removes it. That is the contract.
            run { obs.observe { received.add(it) } }

            System.gc()
            obs.emit()

            received shouldBe listOf(0)
        }

        // Mutating subscriptions from inside a callback ////////////////////////////////////////

        "A callback may unsubscribe itself while a value is being dispatched" {
            val obs = Obs()
            val seen = mutableListOf<String>()

            var unsubA: Unsubscribe? = null
            unsubA = obs.observe { seen.add("a"); unsubA?.invoke() }
            obs.observe { seen.add("b") }

            // needs at least two subscriptions: the iterator only checks for concurrent
            // modification on next(), so a single-subscriber round never noticed
            obs.emit()

            seen shouldBe listOf("a", "b")

            seen.clear()
            obs.emit()
            seen shouldBe listOf("b")
        }

        "A callback may subscribe a new observer while a value is being dispatched" {
            val obs = Obs()
            val seen = mutableListOf<String>()

            obs.observe { seen.add("a"); obs.observe { seen.add("late") } }
            obs.observe { seen.add("b") }

            obs.emit()

            // the new subscription is not notified in the round it was added
            seen shouldBe listOf("a", "b")

            seen.clear()
            obs.emit()
            seen shouldBe listOf("a", "b", "late")
        }

        // unsubscribeAll / hasSubscriptions ////////////////////////////////////////////////////

        "hasSubscriptions reports whether anyone is listening" {
            val obs = Obs()

            obs.subs.hasSubscriptions() shouldBe false

            val unsub = obs.observe { }
            obs.subs.hasSubscriptions() shouldBe true

            unsub()
            obs.subs.hasSubscriptions() shouldBe false
        }

        "unsubscribeAll drops every subscription" {
            val obs = Obs()
            val seen = mutableListOf<Int>()

            obs.observe { seen.add(it) }
            obs.observe { seen.add(it) }

            obs.subs.numSubscriptions() shouldBe 2

            obs.subs.unsubscribeAll()

            obs.subs.numSubscriptions() shouldBe 0
            obs.subs.hasSubscriptions() shouldBe false

            obs.emit()
            seen shouldBe emptyList()
        }

        "Unsubscribing after unsubscribeAll is harmless" {
            val obs = Obs()
            val unsub = obs.observe { }

            obs.subs.unsubscribeAll()
            unsub()

            obs.subs.numSubscriptions() shouldBe 0
        }

        "Subscriptions are notified in subscription order" {
            val obs = Obs()
            val order = mutableListOf<String>()

            obs.observe { order.add("first") }
            obs.observe { order.add("second") }
            obs.observe { order.add("third") }

            obs.emit()

            order shouldBe listOf("first", "second", "third")
        }
    }
}
