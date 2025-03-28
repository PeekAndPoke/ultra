package de.peekandpoke.ultra.common

import de.peekandpoke.ultra.common.datetime.Kronos
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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

            System.gc()
            delay(500)
            System.gc()

            subject.value shouldBe something
        }

        "A weak reference must eventually be garbage collected" {
            var something: Any? = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something

            // clearing the reference
            @Suppress("UNUSED_VALUE")
            something = null

            eventually(3.seconds) {
                // Trigger garbage collection
                System.gc()

                subject.value shouldBe null
            }
        }
    }
}
