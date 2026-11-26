package io.peekandpoke.ultra.common

/**
 * A read-only list view of [size] elements that computes each element from its index on demand; nothing is stored.
 *
 * What it means for a consumer:
 * - Each read computes the element again: two reads of one index give equal, but not the same, instances.
 * - `size`, `get`, `first`, `last`, iteration and `subList` (a view as well) are cheap, whatever the size.
 * - `toList()`, `map`, `filter`, `sorted` and the like build a real list of every element: expensive for a large view.
 * - `equals`, `hashCode`, `contains` and `indexOf` read the elements one by one, as for any list.
 * - `toString()` is bounded: the size and the first three elements, whatever the size.
 * - It is read-only and stateless, so safe to share between threads as long as [compute] is pure.
 * - A negative [size] is an empty view; an index out of range throws [IndexOutOfBoundsException] (the [List] contract).
 *
 * It is [RandomAccess]. A size estimator that walks or samples the list sees computed elements and charges them as if
 * they were stored, so it over-counts a view.
 */
class ComputedList<T>(
    size: Int,
    private val compute: (index: Int) -> T,
) : AbstractList<T>(), RandomAccess {

    private companion object {
        const val TO_STRING_ELEMENTS = 3
    }

    override val size: Int = size.coerceAtLeast(0)

    override fun get(index: Int): T {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index is out of bounds for size $size")

        return compute(index)
    }

    override fun toString(): String {
        val shown = (0..<minOf(size, TO_STRING_ELEMENTS)).map { compute(it) }
        val more = if (size > TO_STRING_ELEMENTS) ", ..." else ""

        return "ComputedList(size=$size, [${shown.joinToString(", ")}$more])"
    }
}
