// Kotlin
package de.peekandpoke.ultra.common.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@Suppress("ReplacePutWithAssignment")
class ValueSortedMapSpec : StringSpec() {
    data class Person(val name: String, val age: Int)

    init {
        "put should store and return previous value when replacing" {
            val m = ValueSortedMap<String, Int, Int> { it } // sort by value itself

            m.put("a", 10) shouldBe null
            m.put("a", 20) shouldBe 10

            m["a"] shouldBe 20
        }

        "ascending should iterate entries in ascending order of sort key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m["k1"] shouldBe null

            m.put("k1", 30)
            m.put("k2", 10)
            m.put("k3", 20)

            // ascending returns pairs (key to value)
            m.ascending().toList().map { it.second } shouldContainExactly listOf(10, 20, 30)
            m.ascending().toList().map { it.first } shouldContainExactly listOf("k2", "k3", "k1")
        }

        "descending should iterate entries in descending order of sort key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("k1", 5)
            m.put("k2", 15)
            m.put("k3", 10)

            m.descending().toList().map { it.second } shouldContainExactly listOf(15, 10, 5)
            m.descending().toList().map { it.first } shouldContainExactly listOf("k2", "k3", "k1")
        }

        "stable ordering for equal sort keys (tie-break by insertion order)" {
            // Use sort key projection that makes all values equal
            val m = ValueSortedMap<String, Int, Int> { 0 }

            // Insert keys in order
            m.put("a", 1) // id 0
            m.put("b", 2) // id 1
            m.put("c", 3) // id 2

            // Since all sort keys equal, ascending should preserve insertion order via id
            m.ascending().toList().map { it.first } shouldContainExactly listOf("a", "b", "c")
            m.descending().toList().map { it.first } shouldContainExactly listOf("c", "b", "a")
        }

        "put replacing value should update ordering according to new sort key" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("k1", 1)
            m.put("k2", 2)
            m.put("k3", 3)

            // Replace k2 with a bigger value so it moves to end
            m.put("k2", 10)

            m.ascending().toList().map { it.first } shouldContainExactly listOf("k1", "k3", "k2")
        }

        "remove should remove key and not appear in ascending/descending" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("a", 1)
            m.put("b", 2)
            m.put("c", 3)

            m.remove("b") shouldBe 2
            m.containsKey("b") shouldBe false

            m.ascending().toList().map { it.first } shouldContainExactly listOf("a", "c")
            m.descending().toList().map { it.first } shouldContainExactly listOf("c", "a")
        }

        "remove of missing key returns null and doesn't change map" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("x", 42)
            m.remove("nope").shouldBeNull()

            m.size shouldBe 1
            m["x"] shouldBe 42
        }

        "clear should empty the map" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.putAll(mapOf("a" to 1, "b" to 2))
            m.size shouldBe 2

            m.clear()
            m.size shouldBe 0

            m.ascending().toList().shouldBeEmpty()
            m.descending().toList().shouldBeEmpty()
        }

        "values, keys and entries reflect current state (values are sorted by projection)" {
            val m = ValueSortedMap<String, Int, Int> { it }

            m.put("one", 1)
            m.put("two", 2)
            m.put("zero", 0)

            // values returns collection in sorted order
            m.values.toList() shouldContainExactly listOf(0, 1, 2)

            // keys reflect map keys (as a set) - order is not guaranteed for key set
            m.keys shouldNotBe null
            m.keys.size shouldBe 3

            // entries should contain correct key->value pairs
            val entriesAsPairs = m.entries.map { it.key to it.value }.toSet()
            entriesAsPairs shouldBe setOf("zero" to 0, "one" to 1, "two" to 2)
        }

        "sort Person objects by age ascending" {
            val m = ValueSortedMap<String, Person, Int> { it.age }

            m.put("p1", Person("Alice", 30))
            m.put("p2", Person("Bob", 25))
            m.put("p3", Person("Charlie", 35))

            m.ascending().toList().map { it.second } shouldContainExactly listOf(
                Person("Bob", 25),
                Person("Alice", 30),
                Person("Charlie", 35),
            )
        }

        "sort Person objects by age descending" {
            val m = ValueSortedMap<String, Person, Int> { it.age }

            m.put("p1", Person("Alice", 30))
            m.put("p2", Person("Bob", 25))
            m.put("p3", Person("Charlie", 35))

            m.descending().toList().map { it.second } shouldContainExactly listOf(
                Person("Charlie", 35),
                Person("Alice", 30),
                Person("Bob", 25),
            )
        }

        "stable ordering for equal ages preserves insertion order" {
            val m = ValueSortedMap<String, Person, Int> { it.age }

            // all same age -> tie broken by insertion id
            m.put("a", Person("Anna", 20))   // id 0
            m.put("b", Person("Ben", 20))    // id 1
            m.put("c", Person("Cara", 20))   // id 2

            m.ascending().toList().map { it.first } shouldContainExactly listOf("a", "b", "c")
            m.descending().toList().map { it.first } shouldContainExactly listOf("c", "b", "a")
        }

        "replacing a Person with different age updates ordering" {
            val m = ValueSortedMap<String, Person, Int> { it.age }

            m.put("x", Person("Xavier", 50))
            m.put("y", Person("Yara", 40))
            m.put("z", Person("Zoe", 45))

            // Replace y with older age so it should move to the end in ascending
            m.put("y", Person("Yara", 60))

            m.ascending().toList().map { it.first } shouldContainExactly listOf("z", "x", "y")
        }

        "values reflect sorted Person objects" {
            val m = ValueSortedMap<String, Person, Int> { it.age }

            m.put("one", Person("One", 1))
            m.put("two", Person("Two", 2))
            m.put("zero", Person("Zero", 0))

            m.values.toList() shouldContainExactly listOf(
                Person("Zero", 0),
                Person("One", 1),
                Person("Two", 2),
            )
        }
    }
}
