package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.datetime.Kronos
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.mpp.uniqueId
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class WeakReferenceSpec : StringSpec() {

    private fun createSomething() = Kronos.systemUtc.instantNow()

    init {
        "Creating a weak ref must work" {
            val something = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something
            subject.get() shouldBe something
        }

        "Clearing a weak ref must work" {
            val something = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something

            subject.clear()

            subject.value shouldBe null
        }

        "A weak reference must not be garbage collected" {
            val something = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something

            delay(1000)

            subject.value shouldBe something
        }

        "A weak reference must eventually be garbage collected" {

            var something: Any? = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something

            val garbage = mutableListOf<List<String>>()

            // clearing the reference
            @Suppress("UNUSED_VALUE")
            something = null

            println("Javascript: Waiting for weak reference to be cleandup up ... this may take a while")

            eventually(180.seconds) {
                // Create lots of new object to trigger garbage collection
                garbage.add((1..1_000_000).map { uniqueId() })
                delay(1)
                garbage.clear()
                delay(1)

                // TODO: How can we test this?
                println("TODO: How can we test this?")
//                subject.value shouldBe null
            }
        }
    }
}
