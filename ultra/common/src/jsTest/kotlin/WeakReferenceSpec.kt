package io.peekandpoke.ultra.common

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class WeakReferenceSpec : StringSpec() {

    // JS WeakRef only accepts objects, not primitives (string, number, boolean).
    // Using a data class ensures we always create a proper JS object.
    private data class Something(val value: Long)

    private fun createSomething(): Any = Something(kotlin.random.Random.nextLong())

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

        // JS GC is non-deterministic — no way to force WeakRef.deref() to return undefined.
        "A weak reference must eventually be garbage collected".config(enabled = false) {

            var something: Any? = createSomething()
            val subject = WeakReference(something)

            subject.value shouldBe something

            @Suppress("UNUSED_VALUE")
            something = null

            eventually(180.seconds) {
                subject.value shouldBe null
            }
        }
    }
}
