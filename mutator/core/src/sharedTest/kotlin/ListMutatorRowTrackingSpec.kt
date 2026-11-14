package io.peekandpoke.mutator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.mutator.domain.Address
import io.peekandpoke.mutator.domain.city
import io.peekandpoke.mutator.domain.mutator
import io.peekandpoke.mutator.domain.street
import io.peekandpoke.mutator.domain.zip
import kotlin.random.Random

/**
 * A held element mutator of a [ListMutator] writes to its own row, wherever structural changes moved that row, and
 * its write is dropped silently when the row is gone or something else wrote the row since (ULTRA-00234, round 2).
 *
 * `E` is one instance, held in several rows on purpose.
 */
class ListMutatorRowTrackingSpec : StringSpec() {

    private val e = Address("E", "C", "Z")
    private val a = Address("A", "C", "Z")
    private val b = Address("B", "C", "Z")
    private val c = Address("C", "C", "Z")
    private val z = Address("Z", "C", "Z")

    init {
        // The round-2 claims A to G ////////////////////////////////////////////////////////////////////////////////

        "A: a write through a mutator whose row another mutator wrote since is dropped" {
            val m = listOf(e, e).mutator()
            val held = m[0]
            m[0].street = "S"
            held.city = "Y"

            m.get() shouldBe listOf(e.copy(street = "S"), e)
        }

        "B: duplicate instances: after removeAt(0) a write goes to the held row" {
            val m = listOf(e, e, e).mutator()
            val row3 = m[2]
            m.removeAt(0)
            row3.street = "X"

            m.get() shouldBe listOf(e, e.copy(street = "X"))
        }

        "C: duplicate instances: after add(0, z) a write goes to the held row" {
            val m = listOf(e, b, e).mutator()
            val held = m[2]
            m.add(0, z)
            held.street = "X"

            m.get() shouldBe listOf(z, e, b, e.copy(street = "X"))
        }

        "D: a write through a mutator whose row was removed is dropped, even when another row holds its value" {
            val m = listOf(a, b).mutator()
            val c0 = m[0]
            c0.set(b)
            m.removeAt(0)
            var notified = 0
            m.onChange { notified++ }
            c0.street = "X"

            m.get() shouldBe listOf(b)
            notified shouldBe 0
        }

        "E: strings: after add(0) a write goes to the held row" {
            val m = listOf("", "", "z").mutator { ObjectMutator(this) }
            val held = m[1]
            m.add(0, m.getChildMutator("new"))
            held.set("typed")

            m.get() shouldBe listOf("new", "", "typed", "z")
        }

        "F: nullable elements: after removeAt(0) a write lands on the held row" {
            val m = listOf<String?>("x", null, null).mutator { ObjectMutator(this) }
            val held = m[2]
            m.removeAt(0)
            held.set("y")

            m.get() shouldBe listOf(null, "y")
        }

        "G: a write through the same mutator from inside a parent observer lands" {
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

            m.get() shouldBe listOf(Address("S", "Y", "Z"))
        }

        // Identity, not equality /////////////////////////////////////////////////////////////////////////////////

        "a row holding an equal but different instance than the mutator last saw counts as written by someone else" {
            val m = listOf(a).mutator()
            val held = m[0]
            m[0].street = "S"
            m[0].street = "A"
            // the row now holds an instance equal to a, but not a itself

            held.city = "Y"

            m.get() shouldBe listOf(a)
            m.get()[0].city shouldBe "C"
        }

        // Structural changes ///////////////////////////////////////////////////////////////////////////////////////

        "clear(): a write through a mutator held across clear() is dropped, also when the same instance comes back" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m.clear()
            m.add(a)
            held.street = "X"

            m.get() shouldBe listOf(a)
        }

        "replacing the whole value: a write through a mutator held across set() is dropped" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m.set(mutableListOf(a, b))
            held.street = "X"

            m.get() shouldBe listOf(a, b)
        }

        "reset(): a write through a mutator held across reset() is dropped (same row count before and after)" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m[1].street = "S"
            m.reset()
            held.street = "X"

            m.get() shouldBe listOf(a, b)
        }

        "sortBy(): a write through a mutator held across a reorder is dropped, never lands on another element" {
            val m = listOf(c, a, b).mutator()
            val held = m[0]
            m.sortBy { it.get().street }
            held.street = "X"

            m.get() shouldBe listOf(a, b, c)
        }

        "reverse(): a write through a mutator held across a reorder is dropped, never lands on another element" {
            val m = listOf(a, b, c).mutator()
            val held = m[0]
            m.reverse()
            held.street = "X"

            m.get() shouldBe listOf(c, b, a)
        }

        "reverse(): a held mutator whose row still holds its element after the reorder writes to it" {
            val m = listOf(a, b, c).mutator()
            val held = m[1]
            m.reverse()
            held.street = "X"

            m.get() shouldBe listOf(c, b.copy(street = "X"), a)
        }

        "sortBy(): a held mutator whose row still holds its element after the reorder writes to it" {
            val m = listOf(a, c, b).mutator()
            val held = m[0]
            m.sortBy { it.get().street }
            held.street = "X"

            m.get() shouldBe listOf(a.copy(street = "X"), b, c)
        }

        "reverse() of 20: a write through a mutator held across the JVM's iterator-based reverse is dropped" {
            val list = (1..20).map { Address("S$it", "C", "Z") }
            val m = list.mutator()
            val held = m[0]
            m.reverse()
            held.street = "X"

            m.get() shouldBe list.reversed()
        }

        "shuffle(): a held mutator writes to its own element if it stayed in its row, else the write is dropped" {
            val list = (1..10).map { Address("S$it", "C", "Z") }

            repeat(20) { seed ->
                val m = list.mutator()
                val held = m[3]
                m.shuffle(Random(seed))
                val shuffled = m.get().toList()
                shuffled shouldBe list.toMutableList().apply { shuffle(Random(seed)) }
                held.street = "X"

                val expected = if (shuffled[3] === list[3]) {
                    shuffled.toMutableList().also { it[3] = list[3].copy(street = "X") }
                } else {
                    shuffled
                }
                m.get() shouldBe expected
            }
        }

        "removeAll(): a write through a held mutator after rows before it were removed lands on its row" {
            val m = listOf(e, b, e).mutator()
            val held = m[2]
            m.removeAll(listOf(m.getChildMutator(b)))
            held.street = "X"

            m.get() shouldBe listOf(e, e.copy(street = "X"))
        }

        "removeAll(): a write through a held mutator whose row was removed is dropped" {
            val m = listOf(a, b, c).mutator()
            val held = m[1]
            m.removeAll(listOf(m.getChildMutator(b)))
            held.street = "X"

            m.get() shouldBe listOf(a, c)
        }

        "retainAll(): a write through a held mutator after rows before it were removed lands on its row" {
            val m = listOf(a, b, e, e).mutator()
            val held = m[3]
            m.retainAll(listOf(m.getChildMutator(e)))
            held.street = "X"

            m.get() shouldBe listOf(e, e.copy(street = "X"))
        }

        "remove(element): a write through a held mutator after an equal row before it was removed lands on its row" {
            val m = listOf(e, b, e).mutator()
            val held = m[2]
            m.remove(m.getChildMutator(e))
            held.street = "X"

            m.get() shouldBe listOf(b, e.copy(street = "X"))
        }

        "addAll(index): a write through a held mutator after rows were inserted before it lands on its row" {
            val m = listOf(e, e).mutator()
            val held = m[1]
            m.addAll(0, listOf(m.getChildMutator(e), m.getChildMutator(z)))
            held.street = "X"

            m.get() shouldBe listOf(e, z, e, e.copy(street = "X"))
        }

        "set(index): a write through a mutator whose row was replaced is dropped" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m[0] = m.getChildMutator(z)
            held.street = "X"

            m.get() shouldBe listOf(z, b)
        }

        "iterator remove(): a write through a held mutator after a row before it was removed lands on its row" {
            val m = listOf(e, e, e).mutator()
            val held = m[2]
            val iterator = m.listIterator()
            iterator.next()
            iterator.remove()
            held.street = "X"

            m.get() shouldBe listOf(e, e.copy(street = "X"))
        }

        "iterator add(): a write through a held mutator after a row was inserted before it lands on its row" {
            val m = listOf(e, e).mutator()
            val held = m[1]
            m.listIterator().add(m.getChildMutator(z))
            held.street = "X"

            m.get() shouldBe listOf(z, e, e.copy(street = "X"))
        }

        "iterator set(): a write through a mutator whose row the iterator replaced is dropped" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            val iterator = m.listIterator()
            iterator.next()
            iterator.set(m.getChildMutator(z))
            held.street = "X"

            m.get() shouldBe listOf(z, b)
        }

        "subList(): a snapshot element writes to its row after rows before it were removed" {
            val m = listOf(e, e, e).mutator()
            val snapshot = m.subList(2, 3)
            m.removeAt(0)
            snapshot[0].street = "X"

            m.get() shouldBe listOf(e, e.copy(street = "X"))
        }

        "subList(): a snapshot element whose row was removed drops its write" {
            val m = listOf(a, b, c).mutator()
            val snapshot = m.subList(1, 2)
            m.removeAt(1)
            snapshot[0].street = "X"

            m.get() shouldBe listOf(a, c)
        }

        "a write after the list was changed in place through get() is dropped, not written to a wrong row" {
            val m = listOf(e, e).mutator()
            val held = m[1]
            m.get().add(0, z)
            held.street = "X"

            m.get() shouldBe listOf(z, e, e)
        }

        // addAll() with the list itself or a view of it ////////////////////////////////////////////////////////////

        "addAll(itself): held mutators keep their rows" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m.addAll(m)
            held.street = "X"

            m.get() shouldBe listOf(a.copy(street = "X"), b, a, b)
        }

        "addAll(index, itself): held mutators keep their rows" {
            val m = listOf(a, b).mutator()
            val first = m[0]
            val last = m[1]
            m.addAll(1, m)
            first.street = "X"
            last.street = "Y"

            m.get() shouldBe listOf(a.copy(street = "X"), a, b, b.copy(street = "Y"))
        }

        "addAll(asReversed()): held mutators keep their rows" {
            val m = listOf(a, b).mutator()
            val held = m[0]
            m.addAll(m.asReversed())
            held.street = "X"

            m.get() shouldBe listOf(a.copy(street = "X"), b, b, a)
        }

        // Rows on both sides of a change in the middle //////////////////////////////////////////////////////////////

        "removeAt(middle): the rows before and after keep their mutators" {
            val m = listOf(a, b, c).mutator()
            val first = m[0]
            val last = m[2]
            m.removeAt(1)
            first.street = "X"
            last.street = "Y"

            m.get() shouldBe listOf(a.copy(street = "X"), c.copy(street = "Y"))
        }

        "add(middle): the rows before and after keep their mutators" {
            val m = listOf(a, b).mutator()
            val first = m[0]
            val last = m[1]
            m.add(1, z)
            first.street = "X"
            last.street = "Y"

            m.get() shouldBe listOf(a.copy(street = "X"), z, b.copy(street = "Y"))
        }

        "addAll(middle): the rows before and after keep their mutators" {
            val m = listOf(a, b).mutator()
            val first = m[0]
            val last = m[1]
            m.addAll(1, listOf(m.getChildMutator(z)))
            first.street = "X"
            last.street = "Y"

            m.get() shouldBe listOf(a.copy(street = "X"), z, b.copy(street = "Y"))
        }

        "removeAll(middle): the rows before and after keep their mutators" {
            val m = listOf(a, b, c).mutator()
            val first = m[0]
            val last = m[2]
            m.removeAll(listOf(m.getChildMutator(b)))
            first.street = "X"
            last.street = "Y"

            m.get() shouldBe listOf(a.copy(street = "X"), c.copy(street = "Y"))
        }

        // NaN ////////////////////////////////////////////////////////////////////////////////////////////////////

        "a row holding Double.NaN can be written (on JS NaN === NaN is false)" {
            val m = listOf(Double.NaN, 1.0).mutator { ObjectMutator(this) }
            m[0].set(2.0)

            m.get() shouldBe listOf(2.0, 1.0)
        }

        "a row holding Float.NaN can be written" {
            val m = listOf(Float.NaN, 1.0f).mutator { ObjectMutator(this) }
            m[0].set(2.0f)

            m.get() shouldBe listOf(2.0f, 1.0f)
        }

        // Current behaviour, pinned //////////////////////////////////////////////////////////////////////////////

        "a dropped write is kept by the mutator: after a second reverse() its next write carries it" {
            val m = listOf(a, b, c).mutator()
            val held = m[0]
            m.reverse()
            held.street = "X"
            m.get() shouldBe listOf(c, b, a)

            m.reverse()
            held.city = "Y"

            m.get() shouldBe listOf(Address("X", "Y", "Z"), b, c)
        }

        "a dropped write is kept by the mutator: once another mutator restores the row, the next write carries it" {
            val m = listOf(a).mutator()
            val held = m[0]
            val other = m[0]
            other.street = "S"
            held.city = "Y"
            m.get() shouldBe listOf(a.copy(street = "S"))

            // back to its baseline: the other mutator writes the very instance a again
            other.street = "A"
            m.get()[0] shouldBe a
            held.zip = "Q"

            m.get() shouldBe listOf(Address("A", "Y", "Q"))
        }

        "remove(element) removes the first equal row, not the row of the mutator passed in" {
            val m = listOf(e, b, e).mutator()
            val first = m[0]
            val last = m[2]
            m.remove(last)
            first.street = "X"
            last.city = "Y"

            m.get() shouldBe listOf(b, e.copy(city = "Y"))
        }
    }
}
