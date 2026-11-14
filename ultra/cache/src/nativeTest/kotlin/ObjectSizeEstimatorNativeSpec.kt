package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private class NativeHolder(val text: String)

/** Native specifics of the default estimator. */
class ObjectSizeEstimatorNativeSpec : StringSpec({

    timeout = TEST_TIMEOUT_MS

    "a custom object is charged a flat header and two references, whatever it holds" {
        val estimator = ObjectSizeEstimator()

        estimator.estimate(NativeHolder("x".repeat(1_000))) shouldBe 16L + 2 * 8L
        // inside a list: the list (16 + 8), the object flat
        estimator.estimate(listOf(NativeHolder("y"))) shouldBe 24L + 32L
    }
})
