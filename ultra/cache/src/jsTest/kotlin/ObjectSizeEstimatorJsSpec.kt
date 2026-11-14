package io.peekandpoke.ultra.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** JS specifics of the default estimator. */
class ObjectSizeEstimatorJsSpec : StringSpec({

    timeout = TEST_TIMEOUT_MS

    "every JS number is charged 8 bytes, a double's size" {
        val estimator = ObjectSizeEstimator()

        estimator.estimate(1) shouldBe 8L
        estimator.estimate(1.5) shouldBe 8L
        estimator.estimate(1.toByte()) shouldBe 8L
    }

    "a property whose getter throws is charged flat, the estimate does not throw" {
        val obj: dynamic = js("({})")
        js("Object.defineProperty(obj, 'bad', { enumerable: true, get: function() { throw new Error('getter failed'); } })")
        obj.good = "fine"

        // header and two references; the failing property charged flat (header and two references);
        // "fine" as a string - not the flat charge of a walk that gave up on the whole object
        ObjectSizeEstimator().estimate(obj) shouldBe (16L + 2 * 8L) + (16L + 2 * 8L) + (16L + 8L + 24L + 2 * 4L)
    }
})
