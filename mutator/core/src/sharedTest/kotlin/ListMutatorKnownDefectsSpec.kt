package io.peekandpoke.mutator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.AddressBook
import io.peekandpoke.mutator.domain.addresses
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.street

/**
 * KNOWN DEFECTS found in review round 2 of ULTRA-00234, pinned as they behave TODAY, so the suite stays green.
 *
 * Each test names the defect and the expected behaviour. A fix flips the test: rewrite it to assert the expected
 * behaviour then. The maintainer decides how to go on.
 */
class ListMutatorKnownDefectsSpec : StringSpec() {

    private val e = Address("E", "C", "Z")
    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val z = Address("Z", "C", "Z")

    init {
        "KNOWN DEFECT A: a held child whose row another child replaced writes to another row holding the same instance (expected: dropped)" {
            val m = listOf(e, e).mutator()
            val held = m[0]
            m[0].street = "S"
            held.city = "Y"

            // expected: listOf(e.copy(street = "S"), e)
            m.get() shouldBe listOf(e.copy(street = "S"), e.copy(city = "Y"))
        }

        "KNOWN DEFECT B: after removeAt(0) a change goes to the first row holding the same instance (expected: the held row)" {
            val m = listOf(e, e, e).mutator()
            val row3 = m[2]
            m.removeAt(0)
            row3.street = "X"

            // expected: listOf(e, e.copy(street = "X"))
            m.get() shouldBe listOf(e.copy(street = "X"), e)
        }

        "KNOWN DEFECT C: after add(0, z) a change goes to the first row holding the same instance (expected: the held row)" {
            val m = listOf(e, b, e).mutator()
            val c = m[2]
            m.add(0, z)
            c.street = "X"

            // expected: listOf(z, e, b, e.copy(street = "X"))
            m.get() shouldBe listOf(z, e.copy(street = "X"), b, e)
        }

        "KNOWN DEFECT D: a change through a child whose row was removed writes to another row (expected: dropped)" {
            val m = listOf(a, b).mutator()
            val c0 = m[0]
            m[1]
            c0.set(b)
            m.removeAt(0)
            c0.street = "X"

            // expected: listOf(b)
            m.get() shouldBe listOf(b.copy(street = "X"))
        }

        "KNOWN DEFECT E: strings: after add(0) a change goes to the first equal-identity string (expected: the held row)" {
            val m = listOf("", "", "z").mutator { ObjectMutator(this) }
            val held = m[1]
            m.add(0, m.getChildMutator("new"))
            held.set("typed")

            // expected: listOf("new", "", "typed", "z")
            m.get() shouldBe listOf("new", "typed", "", "z")
        }

        "KNOWN DEFECT F: nullable elements: a change after removeAt(0) throws IndexOutOfBoundsException (expected: it lands)" {
            val m = listOf<String?>("x", null).mutator { ObjectMutator(this) }
            val held = m[1]
            m.removeAt(0)

            // expected: m.get() == listOf("y")
            shouldThrow<IndexOutOfBoundsException> { held.set("y") }
        }

        "KNOWN DEFECT G: a write through the same child from inside a parent observer is dropped (expected: it lands)" {
            val m = listOf(a).mutator()
            val held = m[0]
            var first = true
            m.onChange {
                if (first) {
                    first = false
                    held.city = "Y"
                }
            }
            held.street = "S"

            // expected: listOf(Address("S", "Y", "Z"))
            m.get() shouldBe listOf(Address("S", "C", "Z"))
        }

        "KNOWN DEFECT H: after commit(), an in-place change through a held list mutator changes the baseline (expected: modified)" {
            val book = AddressBook(listOf(a)).mutator()
            val pm = book.addresses
            pm.add(b)
            book.commit()
            pm.add(z)

            // expected: isModified() == true, getInitialValue() == AddressBook(listOf(a, b))
            book.isModified() shouldBe false
            book.getInitialValue() shouldBe AddressBook(listOf(a, b, z))
        }

        "KNOWN DEFECT I: the list iterator throws IndexOutOfBoundsException past its ends (expected: NoSuchElementException)" {
            val m = listOf(a).mutator()
            val it = m.listIterator()
            it.next()

            shouldThrow<IndexOutOfBoundsException> { it.next() }
            shouldThrow<IndexOutOfBoundsException> { m.listIterator().previous() }
        }

        "KNOWN DEFECT I: listIterator(index) accepts an index out of range (expected: IndexOutOfBoundsException)" {
            val m = listOf(a).mutator()

            m.listIterator(-1).hasPrevious() shouldBe false
            m.listIterator(2).hasNext() shouldBe false
        }
    }
}
