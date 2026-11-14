package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.AddressBook
import io.peekandpoke.mutator.domain.addresses
import io.peekandpoke.mutator.domain.mutator

/**
 * KNOWN DEFECTS, pinned as they behave TODAY so the suite stays green. Each test names the defect, its ticket and the
 * expected behaviour; the fix flips the test, which is then rewritten to assert the expected behaviour.
 */
class ListMutatorKnownDefectsSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val z = Address("Z", "C", "Z")

    init {
        "KNOWN DEFECT H (ULTRA-00320): after commit(), a held list mutator's in-place change alters the baseline" {
            val book = AddressBook(listOf(a)).mutator()
            val pm = book.addresses
            pm.add(b)
            book.commit()
            pm.add(z)

            // expected: isModified() == true, getInitialValue() == AddressBook(listOf(a, b))
            book.isModified() shouldBe false
            book.getInitialValue() shouldBe AddressBook(listOf(a, b, z))
        }
    }
}
