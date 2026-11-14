package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * On JS numbers are not boxed, so for the row guard's identity check an equal number written by someone else is the
 * same instance, whatever its size.
 */
class ListMutatorIdentityJsSpec : StringSpec() {

    init {
        "JS: after a rewrite with an equal Int (1000), a held mutator's write lands" {
            val m = listOf(1000).mutator { ObjectMutator(this) }
            val held = m[0]
            val iterator = m.listIterator()
            iterator.next()
            iterator.set(m.getChildMutator(1000))

            held.set(1001)

            m.get() shouldBe listOf(1001)
        }
    }
}
