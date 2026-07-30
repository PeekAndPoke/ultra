package io.peekandpoke.ultra.common

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/** JVM-only: weak collection cannot be forced on the other platforms. */
class WeakSetJvmSpec : StringSpec({

    "an element is dropped once no strong reference to it remains" {
        // a data class, so membership can be probed with an equal-but-distinct instance —
        // holding the original would pin it and defeat the test
        data class Holder(val id: Int)

        val subject = WeakSet<Holder>()

        var strong: Holder? = Holder(42)
        subject.add(strong!!)

        subject.contains(Holder(42)) shouldBe true

        strong = null

        eventually(10.seconds) {
            System.gc()
            // allocate to make a collection more likely
            ByteArray(1024 * 50)

            subject.contains(Holder(42)) shouldBe false
        }
    }
})
