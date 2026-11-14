package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator

/**
 * Setting a collection mutator back to a value equal to its baseline must not tie the value to the baseline.
 */
class MutatorBaselineSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")

    init {
        "list: an in-place change after set() back to the baseline leaves the baseline alone" {
            val mutator = listOf(a).mutator()
            mutator.add(b)

            mutator.set(mutableListOf(a))
            mutator.isModified() shouldBe false

            mutator.add(c)

            mutator.isModified() shouldBe true
            mutator.getInitialValue() shouldBe listOf(a)
            mutator.reset() shouldBe listOf(a)
        }

        "set: an in-place change after set() back to the baseline leaves the baseline alone" {
            val mutator = setOf(a).mutator()
            mutator.add(b)

            mutator.set(mutableSetOf(a))
            mutator.isModified() shouldBe false

            mutator.add(c)

            mutator.isModified() shouldBe true
            mutator.getInitialValue() shouldBe setOf(a)
            mutator.reset() shouldBe setOf(a)
        }

        "map: an in-place change after set() back to the baseline leaves the baseline alone" {
            val mutator = mapOf("a" to a).mutator()
            mutator.put("b", mutator.getChildMutator(b))

            mutator.set(mutableMapOf("a" to a))
            mutator.isModified() shouldBe false

            mutator.put("c", mutator.getChildMutator(c))

            mutator.isModified() shouldBe true
            mutator.getInitialValue() shouldBe mapOf("a" to a)
            mutator.reset() shouldBe mapOf("a" to a)
        }
    }
}
