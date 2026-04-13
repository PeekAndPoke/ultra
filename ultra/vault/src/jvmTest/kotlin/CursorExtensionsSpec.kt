package io.peekandpoke.ultra.vault

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class CursorExtensionsSpec : StringSpec({

    // Helper: Cursor backed by a specific EntityCache ///////////////////////////////////////////

    fun <T> cursorOf(items: List<T>, cache: EntityCache): Cursor<T> = object : Cursor<T> {
        override val entityCache: EntityCache = cache
        override val query: TypedQuery<T> get() = error("not needed in tests")
        override val count: Long = items.size.toLong()
        override val fullCount: Long? = null
        override val timeMs: Double = 1.0
        override fun asFlow(): Flow<T> = items.asFlow()
    }

    // map ///////////////////////////////////////////////////////////////////////////////////////

    "map transforms each item" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.map { it * 10 } shouldBe listOf(10, 20, 30)
    }

    "map on empty cursor returns empty list" {
        val cursor = Cursor.of(emptyList<Int>())

        cursor.map { it * 10 }.shouldBeEmpty()
    }

    // mapNotNull ////////////////////////////////////////////////////////////////////////////////

    "mapNotNull discards null results" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        cursor.mapNotNull { if (it % 2 == 0) it else null } shouldBe listOf(2, 4)
    }

    "mapNotNull on all-null returns empty list" {
        val cursor = Cursor.of(listOf("a", "b"))

        cursor.mapNotNull { null }.shouldBeEmpty()
    }

    // mapIndexed ////////////////////////////////////////////////////////////////////////////////

    "mapIndexed provides correct indices" {
        val cursor = Cursor.of(listOf("a", "b", "c"))

        cursor.mapIndexed { idx, v -> "$idx:$v" } shouldBe listOf("0:a", "1:b", "2:c")
    }

    // flatMap ///////////////////////////////////////////////////////////////////////////////////

    "flatMap flattens results" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.flatMap { listOf(it, it * 10) } shouldBe listOf(1, 10, 2, 20, 3, 30)
    }

    "flatMap with empty inner lists returns empty" {
        val cursor = Cursor.of(listOf(1, 2))

        cursor.flatMap { emptyList<Int>() }.shouldBeEmpty()
    }

    // filter ////////////////////////////////////////////////////////////////////////////////////

    "filter keeps matching items" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        cursor.filter { it > 3 } shouldBe listOf(4, 5)
    }

    "filter with no matches returns empty" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.filter { it > 100 }.shouldBeEmpty()
    }

    // filterNot /////////////////////////////////////////////////////////////////////////////////

    "filterNot removes matching items" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        cursor.filterNot { it % 2 == 0 } shouldBe listOf(1, 3, 5)
    }

    // filterIsInstance //////////////////////////////////////////////////////////////////////////

    "filterIsInstance keeps items of the target type" {
        val cursor = Cursor.of(listOf<Any>(1, "two", 3, "four", 5))

        cursor.filterIsInstance<String>() shouldBe listOf("two", "four")
    }

    "filterIsInstance on empty cursor returns empty" {
        val cursor = Cursor.of(emptyList<Any>())

        cursor.filterIsInstance<String>().shouldBeEmpty()
    }

    // firstOrNull (no predicate) ////////////////////////////////////////////////////////////////

    "firstOrNull returns first item" {
        val cursor = Cursor.of(listOf("x", "y", "z"))

        cursor.firstOrNull() shouldBe "x"
    }

    "firstOrNull on empty cursor returns null" {
        val cursor = Cursor.of(emptyList<String>())

        cursor.firstOrNull() shouldBe null
    }

    // firstOrNull (with predicate) //////////////////////////////////////////////////////////////

    "firstOrNull with predicate returns first match" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4))

        cursor.firstOrNull { it > 2 } shouldBe 3
    }

    "firstOrNull with predicate returns null when none match" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.firstOrNull { it > 100 } shouldBe null
    }

    // first (no predicate) //////////////////////////////////////////////////////////////////////

    "first returns first item" {
        val cursor = Cursor.of(listOf(10, 20))

        cursor.first() shouldBe 10
    }

    "first on empty cursor throws" {
        val cursor = Cursor.of(emptyList<Int>())

        shouldThrow<NoSuchElementException> {
            cursor.first()
        }
    }

    // first (with predicate) ////////////////////////////////////////////////////////////////////

    "first with predicate returns first match" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4))

        cursor.first { it % 2 == 0 } shouldBe 2
    }

    "first with predicate throws when none match" {
        val cursor = Cursor.of(listOf(1, 3, 5))

        shouldThrow<NoSuchElementException> {
            cursor.first { it % 2 == 0 }
        }
    }

    // find //////////////////////////////////////////////////////////////////////////////////////

    "find returns first matching item" {
        val cursor = Cursor.of(listOf("apple", "banana", "avocado"))

        cursor.find { it.startsWith("b") } shouldBe "banana"
    }

    "find returns null when nothing matches" {
        val cursor = Cursor.of(listOf("apple", "banana"))

        cursor.find { it.startsWith("z") } shouldBe null
    }

    // lastOrNull ////////////////////////////////////////////////////////////////////////////////

    "lastOrNull returns last item" {
        val cursor = Cursor.of(listOf("a", "b", "c"))

        cursor.lastOrNull() shouldBe "c"
    }

    "lastOrNull on empty cursor returns null" {
        val cursor = Cursor.of(emptyList<String>())

        cursor.lastOrNull() shouldBe null
    }

    // forEach ///////////////////////////////////////////////////////////////////////////////////

    "forEach visits every item in order" {
        val cursor = Cursor.of(listOf(1, 2, 3))
        val collected = mutableListOf<Int>()

        cursor.forEach { collected.add(it) }

        collected shouldBe listOf(1, 2, 3)
    }

    // forEachIndexed ////////////////////////////////////////////////////////////////////////////

    "forEachIndexed provides correct indices" {
        val cursor = Cursor.of(listOf("a", "b", "c"))
        val collected = mutableListOf<String>()

        cursor.forEachIndexed { idx, v -> collected.add("$idx=$v") }

        collected shouldBe listOf("0=a", "1=b", "2=c")
    }

    // any ///////////////////////////////////////////////////////////////////////////////////////

    "any returns true when at least one item matches" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.any { it == 2 } shouldBe true
    }

    "any returns false when no items match" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.any { it == 99 } shouldBe false
    }

    "any on empty cursor returns false" {
        val cursor = Cursor.of(emptyList<Int>())

        cursor.any { true } shouldBe false
    }

    // none //////////////////////////////////////////////////////////////////////////////////////

    "none returns true when no items match" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.none { it > 10 } shouldBe true
    }

    "none returns false when at least one matches" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.none { it == 2 } shouldBe false
    }

    // all ///////////////////////////////////////////////////////////////////////////////////////

    "all returns true when every item matches" {
        val cursor = Cursor.of(listOf(2, 4, 6))

        cursor.all { it % 2 == 0 } shouldBe true
    }

    "all returns false when at least one does not match" {
        val cursor = Cursor.of(listOf(2, 3, 6))

        cursor.all { it % 2 == 0 } shouldBe false
    }

    "all on empty cursor returns true" {
        val cursor = Cursor.of(emptyList<Int>())

        cursor.all { false } shouldBe true
    }

    // fold //////////////////////////////////////////////////////////////////////////////////////

    "fold accumulates a result" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4))

        cursor.fold(0) { acc, v -> acc + v } shouldBe 10
    }

    "fold on empty cursor returns initial value" {
        val cursor = Cursor.of(emptyList<Int>())

        cursor.fold(42) { acc, v -> acc + v } shouldBe 42
    }

    // groupBy ///////////////////////////////////////////////////////////////////////////////////

    "groupBy groups items by key" {
        val cursor = Cursor.of(listOf("ant", "apple", "bat", "ball"))

        val result = cursor.groupBy { it.first() }

        result shouldBe mapOf(
            'a' to listOf("ant", "apple"),
            'b' to listOf("bat", "ball"),
        )
    }

    // associateBy ///////////////////////////////////////////////////////////////////////////////

    "associateBy creates map keyed by selector" {
        val cursor = Cursor.of(listOf("one", "two", "six"))

        val result = cursor.associateBy { it.length }

        // "one", "two", "six" all have length 3; last wins
        result shouldBe mapOf(3 to "six")
    }

    "associateBy with unique keys preserves all entries" {
        val cursor = Cursor.of(listOf("a", "bb", "ccc"))

        val result = cursor.associateBy { it.length }

        result shouldBe mapOf(1 to "a", 2 to "bb", 3 to "ccc")
    }

    // associate /////////////////////////////////////////////////////////////////////////////////

    "associate creates map from key-value pairs" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        val result = cursor.associate { it to it * it }

        result shouldBe mapOf(1 to 1, 2 to 4, 3 to 9)
    }

    // partition /////////////////////////////////////////////////////////////////////////////////

    "partition splits into matching and non-matching" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        val (evens, odds) = cursor.partition { it % 2 == 0 }

        evens shouldBe listOf(2, 4)
        odds shouldBe listOf(1, 3, 5)
    }

    "partition on empty cursor returns two empty lists" {
        val cursor = Cursor.of(emptyList<Int>())

        val (a, b) = cursor.partition { true }

        a.shouldBeEmpty()
        b.shouldBeEmpty()
    }

    // sortedBy //////////////////////////////////////////////////////////////////////////////////

    "sortedBy sorts in ascending order" {
        val cursor = Cursor.of(listOf("banana", "apple", "cherry"))

        cursor.sortedBy { it } shouldBe listOf("apple", "banana", "cherry")
    }

    "sortedBy with numeric selector" {
        val cursor = Cursor.of(listOf("bb", "a", "ccc"))

        cursor.sortedBy { it.length } shouldBe listOf("a", "bb", "ccc")
    }

    // sortedByDescending ////////////////////////////////////////////////////////////////////////

    "sortedByDescending sorts in descending order" {
        val cursor = Cursor.of(listOf(1, 3, 2))

        cursor.sortedByDescending { it } shouldBe listOf(3, 2, 1)
    }

    // distinct //////////////////////////////////////////////////////////////////////////////////

    "distinct removes duplicate items" {
        val cursor = Cursor.of(listOf(1, 2, 2, 3, 1))

        cursor.distinct() shouldBe listOf(1, 2, 3)
    }

    "distinct on already-unique list is unchanged" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.distinct() shouldBe listOf(1, 2, 3)
    }

    // distinctBy ////////////////////////////////////////////////////////////////////////////////

    "distinctBy removes duplicates by selector" {
        val cursor = Cursor.of(listOf("apple", "avocado", "banana", "blueberry"))

        cursor.distinctBy { it.first() } shouldBe listOf("apple", "banana")
    }

    // take //////////////////////////////////////////////////////////////////////////////////////

    "take returns first n items" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        cursor.take(3) shouldBe listOf(1, 2, 3)
    }

    "take more than available returns all" {
        val cursor = Cursor.of(listOf(1, 2))

        cursor.take(10) shouldBe listOf(1, 2)
    }

    "take zero returns empty" {
        val cursor = Cursor.of(listOf(1, 2, 3))

        cursor.take(0).shouldBeEmpty()
    }

    // drop //////////////////////////////////////////////////////////////////////////////////////

    "drop skips first n items" {
        val cursor = Cursor.of(listOf(1, 2, 3, 4, 5))

        cursor.drop(2) shouldBe listOf(3, 4, 5)
    }

    "drop more than available returns empty" {
        val cursor = Cursor.of(listOf(1, 2))

        cursor.drop(10).shouldBeEmpty()
    }

    // cache (Cursor<Stored<T>>) /////////////////////////////////////////////////////////////////

    "cache puts all items into the entity cache and returns them" {
        val cache = DefaultEntityCache()

        val stored1 = Stored(value = "alpha", _id = "col/1")
        val stored2 = Stored(value = "beta", _id = "col/2")
        val stored3 = Stored(value = "gamma", _id = "col/3")

        val cursor = cursorOf(listOf(stored1, stored2, stored3), cache)

        val result = cursor.cache()

        // Returns all items
        result shouldContainExactly listOf(stored1, stored2, stored3)

        // Items are now in the cache
        cache.getOrPut<Stored<String>>("col/1") { null } shouldBe stored1
        cache.getOrPut<Stored<String>>("col/2") { null } shouldBe stored2
        cache.getOrPut<Stored<String>>("col/3") { null } shouldBe stored3
    }

    "cache on empty cursor returns empty list and caches nothing" {
        val cache = DefaultEntityCache()

        val cursor = cursorOf(emptyList<Stored<String>>(), cache)

        val result = cursor.cache()

        result.shouldBeEmpty()

        // Provider is called (nothing was cached), returning the sentinel
        cache.getOrPut("col/1") { "sentinel" } shouldBe "sentinel"
    }
})
