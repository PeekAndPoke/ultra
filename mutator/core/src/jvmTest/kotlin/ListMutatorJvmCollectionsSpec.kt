package io.peekandpoke.mutator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Collections

/**
 * `java.util.Collections` on a [ListMutator]: a documented limit, pinned (ULTRA-00234).
 */
class ListMutatorJvmCollectionsSpec : StringSpec() {

    init {
        "Collections.rotate() on 100 or more rows throws: it reverses subList() ranges, and the snapshot is read-only" {
            val list = (1..100).toList()
            val m = list.mutator { ObjectMutator(this) }

            shouldThrow<UnsupportedOperationException> { Collections.rotate(m, 1) }
        }

        "Collections.rotate() on fewer than 100 rows works" {
            val list = (1..99).toList()
            val m = list.mutator { ObjectMutator(this) }

            Collections.rotate(m, 1)

            m.get() shouldBe listOf(99) + (1..98).toList()
        }
    }
}
