package de.peekandpoke.ultra.reflection

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ChildFinderSpec : StringSpec() {

    data class Named(val name: String)
    data class Container(val child: Named)
    data class NestedContainer(val inner: Container)
    data class ListContainer(val items: List<Named>)
    data class MapContainer(val items: Map<String, Named>)

    init {

        "find direct child by class" {
            val target = Container(Named("Alice"))
            val results = ChildFinder.find(Named::class, target)

            results shouldHaveSize 1
            results[0].item shouldBe Named("Alice")
            results[0].path shouldBe "root.child"
        }

        "find deeply nested child" {
            val target = NestedContainer(Container(Named("Bob")))
            val results = ChildFinder.find(Named::class, target)

            results shouldHaveSize 1
            results[0].item shouldBe Named("Bob")
        }

        "find children in list" {
            val target = ListContainer(listOf(Named("A"), Named("B"), Named("C")))
            val results = ChildFinder.find(Named::class, target)

            results shouldHaveSize 3
            results.map { it.item.name }.toSet() shouldBe setOf("A", "B", "C")
        }

        "find children in map values" {
            val target = MapContainer(mapOf("x" to Named("X"), "y" to Named("Y")))
            val results = ChildFinder.find(Named::class, target)

            results shouldHaveSize 2
            results.map { it.item.name }.toSet() shouldBe setOf("X", "Y")
        }

        "find with predicate filters results" {
            val target = ListContainer(listOf(Named("keep"), Named("skip"), Named("keep2")))
            val results = ChildFinder.find(Named::class, target) { it.name.startsWith("keep") }

            results shouldHaveSize 2
            results.map { it.item.name }.toSet() shouldBe setOf("keep", "keep2")
        }

        "find returns empty when no match" {
            data class Unrelated(val x: Int)

            val target = Container(Named("Alice"))
            val results = ChildFinder.find(Unrelated::class, target)

            results.shouldBeEmpty()
        }

        "find tracks parents" {
            val named = Named("Deep")
            val container = Container(named)
            val target = NestedContainer(container)

            val results = ChildFinder.find(Named::class, target)

            results shouldHaveSize 1
            results[0].parent(0) shouldBe container
            results[0].parent(1) shouldBe target
        }

        "Found.parent returns null for out-of-range index" {
            val target = Container(Named("X"))
            val results = ChildFinder.find(Named::class, target)

            results[0].parent(999) shouldBe null
        }
    }
}
