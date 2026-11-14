package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.AddressBook
import io.peekandpoke.mutator.domain.addresses
import io.peekandpoke.mutator.domain.mutator

/**
 * KNOWN DEFECTS and KNOWN BEHAVIOUR awaiting a decision, pinned as they behave TODAY so the suite stays green. Each
 * test names the case, its ticket and the expected behaviour; a fix flips the test, which is then rewritten.
 */
class ListMutatorKnownDefectsSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")
    private val x = Address("X", "C", "Z")
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

        // The list iterator's own remove() and set() address rows by index: no fix (ULTRA-00324) ////////////////

        "KNOWN BEHAVIOUR (ULTRA-00324): iterator set() after removeAt(0) before it writes the row after its element" {
            val m = listOf(a, b, c).mutator()
            val iterator = m.listIterator()
            iterator.next()
            iterator.next()
            m.removeAt(0)
            iterator.set(m.getChildMutator(x))

            // expected, by row: listOf(x, c)
            m.get() shouldBe listOf(b, x)
        }

        "KNOWN BEHAVIOUR (ULTRA-00324): iterator remove() after an earlier removeAt(0) removes the next row" {
            val m = listOf(a, b, c).mutator()
            val iterator = m.listIterator()
            iterator.next()
            iterator.next()
            m.removeAt(0)
            iterator.remove()

            // expected, by row: listOf(c)
            m.get() shouldBe listOf(b)
        }

        "KNOWN BEHAVIOUR (ULTRA-00324): iterator remove() after add(0, z) removes the inserted row" {
            val m = listOf(a, b).mutator()
            val iterator = m.listIterator()
            iterator.next()
            m.add(0, z)
            iterator.remove()

            // expected, by row: listOf(z, b)
            m.get() shouldBe listOf(a, b)
        }
    }
}
