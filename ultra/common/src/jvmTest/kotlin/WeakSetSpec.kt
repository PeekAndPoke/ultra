// language: kotlin
package de.peekandpoke.ultra.common

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds

class WeakSetSpec : StringSpec() {
    init {
        "entries should be removed after GC when no strong refs remain" {
            @Suppress("unused")
            class Holder(val id: Int)

            val set = WeakSet<Holder>()

            // create object and keep only a weak reference
            var strong: Holder? = Holder(42)
            val weakRef = WeakReference(strong)

            set.add(strong!!)
            set.contains(strong).shouldBeTrue()

            // drop strong reference
            strong = null

            // wait for GC to clear the weak reference and then verify the set no longer holds it
            eventually(10.seconds) {
                System.gc()
                // also allocate some garbage to make GC more likely to run
                ByteArray(1024 * 50)
                weakRef.get() == null
            }

            set.isEmpty().shouldBeTrue()
        }


        "add and contains should work" {
            val s = WeakSet<String>()

            s.size shouldBe 0
            s.contains("a") shouldBe false

            s.add("a")
            s.contains("a") shouldBe true
            s.size shouldBe 1

            // Adding same element again should not change size
            s.add("a")
            s.size shouldBe 1

            s.add("b")
            s.contains("b") shouldBe true
            s.size shouldBe 2
        }

        "toSet should return a snapshot of the current elements" {
            val s = WeakSet<String>()

            s.add("one")
            s.add("two")
            s.add("three")

            val snapshot = s.toSet()
            // snapshot should contain exactly the elements we added (order-independent)
            snapshot.shouldContainExactlyInAnyOrder(listOf("one", "two", "three"))
            // original set size is unchanged
            s.size shouldBe 3
        }

        "remove should remove an element and update size" {
            val s = WeakSet<Int>()

            s.add(1)
            s.add(2)
            s.add(3)

            s.size shouldBe 3
            s.contains(2) shouldBe true

            s.remove(2)
            s.contains(2) shouldBe false
            s.size shouldBe 2

            // removing something not present is a no-op
            s.remove(42)
            s.size shouldBe 2
        }

        "clear should remove all elements" {
            val s = WeakSet<String>()

            s.add("a")
            s.add("b")
            s.add("c")

            s.size shouldBe 3

            s.clear()

            s.size shouldBe 0
            // toSet snapshot after clear should be empty
            s.toSet().shouldBeEmpty()
            s.contains("a") shouldBe false
        }

        "mix of operations maintains consistent behavior" {
            val s = WeakSet<String>()

            s.add("x")
            s.add("y")
            s.size shouldBe 2

            s.remove("x")
            s.size shouldBe 1
            s.contains("y") shouldBe true

            val snap1 = s.toSet()
            snap1.shouldContainExactlyInAnyOrder(listOf("y"))

            s.add("z")
            s.size shouldBe 2

            val snap2 = s.toSet()
            snap2.shouldContainExactlyInAnyOrder(listOf("y", "z"))

            s.clear()
            s.size shouldBe 0
        }
    }
}

