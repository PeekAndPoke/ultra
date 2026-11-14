package io.peekandpoke.mutator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.street

/**
 * [ListMutator.subList] returns a read-only snapshot of child mutators that write back to the parent (ULTRA-00234).
 */
class ListMutatorSubListSpec : StringSpec() {

    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")
    private val x = Address("X", "C", "Z")

    init {
        "subList() holds the elements of the range" {
            val mutator = listOf(a, b, c).mutator()

            mutator.subList(0, 2).map { it.get() } shouldBe listOf(a, b)
            mutator.subList(1, 3).map { it.get() } shouldBe listOf(b, c)
            mutator.subList(0, 3).map { it.get() } shouldBe listOf(a, b, c)
        }

        "subList() of an empty range is empty" {
            val mutator = listOf(a, b, c).mutator()

            mutator.subList(0, 0).isEmpty() shouldBe true
            mutator.subList(3, 3).isEmpty() shouldBe true
            listOf<Address>().mutator().subList(0, 0).isEmpty() shouldBe true
        }

        "subList() checks its bounds like List.subList()" {
            val mutator = listOf(a, b, c).mutator()

            shouldThrow<IndexOutOfBoundsException> { mutator.subList(-1, 2) }
            shouldThrow<IndexOutOfBoundsException> { mutator.subList(0, 4) }
            shouldThrow<IllegalArgumentException> { mutator.subList(2, 1) }
        }

        "a change through the snapshot writes back to the parent and notifies its observers" {
            val mutator = listOf(a, b, c).mutator()
            var notified = 0
            var seen: List<Address>? = null
            mutator.onChange {
                notified++
                seen = it.toList()
            }

            val snapshot = mutator.subList(1, 3)
            snapshot[0].street = "B-changed"

            val expected = listOf(a, b.copy(street = "B-changed"), c)
            mutator.get() shouldBe expected
            notified shouldBe 1
            seen shouldBe expected
        }

        "a change through the snapshot finds its row after the row moved" {
            val mutator = listOf(a, b, c).mutator()
            val snapshot = mutator.subList(1, 3)

            // c moves from index 2 to 1, and index 2 now holds a different element
            mutator.removeAt(0)
            mutator.add(x)
            snapshot[1].street = "C-changed"

            mutator.get() shouldBe listOf(b, c.copy(street = "C-changed"), x)
        }

        "the snapshot's contains(), containsAll(), indexOf() and lastIndexOf() compare by value, as the list does" {
            val mutator = listOf(a, b, a).mutator()
            val snapshot = mutator.subList(0, 3)

            snapshot.indexOf(mutator[2]) shouldBe 0
            snapshot.indexOf(mutator[1]) shouldBe 1
            snapshot.lastIndexOf(mutator.getChildMutator(a)) shouldBe 2
            snapshot.contains(mutator.getChildMutator(b)) shouldBe true
            snapshot.containsAll(listOf(mutator.getChildMutator(a), mutator.getChildMutator(b))) shouldBe true

            snapshot.indexOf(mutator.getChildMutator(x)) shouldBe -1
            snapshot.lastIndexOf(mutator.getChildMutator(x)) shouldBe -1
            snapshot.contains(mutator.getChildMutator(x)) shouldBe false
            snapshot.containsAll(listOf(mutator.getChildMutator(a), mutator.getChildMutator(x))) shouldBe false

            // the same answers as the list itself
            mutator.indexOf(mutator[2]) shouldBe 0
            mutator.lastIndexOf(mutator.getChildMutator(a)) shouldBe 2
        }

        "the snapshot's toString() shows the values" {
            val mutator = listOf(a, b, c).mutator()

            mutator.subList(0, 2).toString() shouldBe listOf(a, b).toString()
        }

        "the snapshot is not a view: later changes to the parent's structure leave it as it was" {
            val mutator = listOf(a, b, c).mutator()
            val snapshot = mutator.subList(0, 2)

            mutator.add(0, x)
            mutator.removeAt(1)
            mutator.add(x)

            mutator.get() shouldBe listOf(x, b, c, x)
            snapshot.size shouldBe 2
            snapshot.map { it.get() } shouldBe listOf(a, b)
        }

        "the snapshot refuses every structural change" {
            val mutator = listOf(a, b, c).mutator()
            val snapshot = mutator.subList(0, 2)
            val child = mutator.getChildMutator(x)

            shouldThrow<UnsupportedOperationException> { snapshot.add(child) }
            shouldThrow<UnsupportedOperationException> { snapshot.add(0, child) }
            shouldThrow<UnsupportedOperationException> { snapshot.addAll(listOf(child)) }
            shouldThrow<UnsupportedOperationException> { snapshot.addAll(0, listOf(child)) }
            shouldThrow<UnsupportedOperationException> { snapshot.remove(snapshot[0]) }
            shouldThrow<UnsupportedOperationException> { snapshot.removeAt(0) }
            shouldThrow<UnsupportedOperationException> { snapshot.removeAll(listOf(snapshot[0])) }
            shouldThrow<UnsupportedOperationException> { snapshot.retainAll(listOf(snapshot[0])) }
            shouldThrow<UnsupportedOperationException> { snapshot.set(0, child) }
            shouldThrow<UnsupportedOperationException> { snapshot.clear() }

            snapshot.map { it.get() } shouldBe listOf(a, b)
            mutator.get() shouldBe listOf(a, b, c)
            mutator.isModified() shouldBe false
        }

        "the snapshot refuses structural changes even when they would change nothing" {
            val mutator = listOf(a, b, c).mutator()
            val empty = mutator.subList(1, 1)

            shouldThrow<UnsupportedOperationException> { empty.clear() }
            shouldThrow<UnsupportedOperationException> { empty.addAll(emptyList()) }
            shouldThrow<UnsupportedOperationException> { empty.removeAll(emptyList()) }
            shouldThrow<UnsupportedOperationException> { empty.retainAll(emptyList()) }
            shouldThrow<UnsupportedOperationException> { empty.remove(mutator.getChildMutator(x)) }
        }

        "the snapshot's iterators refuse structural changes" {
            val mutator = listOf(a, b, c).mutator()
            val snapshot = mutator.subList(0, 2)
            val child = mutator.getChildMutator(x)

            val iterator = snapshot.iterator()
            iterator.next()
            shouldThrow<UnsupportedOperationException> { iterator.remove() }

            val listIterator = snapshot.listIterator()
            listIterator.next()
            shouldThrow<UnsupportedOperationException> { listIterator.remove() }
            shouldThrow<UnsupportedOperationException> { listIterator.set(child) }
            shouldThrow<UnsupportedOperationException> { listIterator.add(child) }

            val fromIndex = snapshot.listIterator(1)
            fromIndex.next().get() shouldBe b
            shouldThrow<UnsupportedOperationException> { fromIndex.remove() }

            snapshot.map { it.get() } shouldBe listOf(a, b)
            mutator.get() shouldBe listOf(a, b, c)
        }

        "the snapshot's own subList() is a read-only snapshot too" {
            val mutator = listOf(a, b, c).mutator()
            val inner = mutator.subList(0, 3).subList(1, 2)

            inner.map { it.get() } shouldBe listOf(b)
            shouldThrow<UnsupportedOperationException> { inner.add(mutator.getChildMutator(x)) }
        }
    }
}
