package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.datetime.Kronos
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class ObservableSpec : StringSpec() {

    class Obs : Observable<Int> {
        val subs = Observable.Subscriptions<Int>()

        private var counter = 0

        fun emit() = subs.emit(counter++)

        override fun subscribe(block: OnChange<Int>): Unsubscribe = subs.subscribe(block)
    }

    private fun createSomething() = Kronos.systemUtc.instantNow()

    init {
        "Subscribing to an Observable must work" {
            val obs = Obs()
            val received = mutableListOf<Int>()
            val unsub = obs.subscribe { received.add(it) }

            obs.emit()

            received shouldBe listOf(0)

            unsub()
        }

        "After unsubscribing no new values must be received" {
            val obs = Obs()
            val received = mutableListOf<Int>()
            val unsub = obs.subscribe { received.add(it) }

            obs.emit()
            obs.emit()
            unsub()
            obs.emit()
            obs.emit()

            received shouldBe listOf(0, 1)
        }

        "No longer referenced subscriptions must be clean up eventually" {
            val obs = Obs()
            val received = mutableListOf<Int>()

            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
            var unsub: Unsubscribe? = obs.subscribe { received.add(it) }

            obs.emit()

            eventually(3.seconds) {
                // clearing the reference
                unsub = null

                // Trigger garbage collection
                System.gc()

                obs.subs.numSubscriptions() shouldBe 0
            }

            obs.emit()

            received shouldBe listOf(0)
        }
    }
}
