package io.peekandpoke.ultra.common

/**
 * A read-only list view of [size] elements that computes each element from its index on demand; nothing is stored.
 *
 * What it means for a consumer:
 * - Each read computes the element again: two reads of one index give equal, but not the same, instances.
 * - `size`, `get`, `first`, `last`, iteration and `subList` (a view as well) are cheap, whatever the size.
 * - `toList()`, `map`, `filter`, `sorted` and the like build a real list of every element: expensive for a large view.
 * - `equals`, `hashCode`, `contains` and `indexOf` read the elements one by one, as for any list.
 * - It is read-only and stateless, so safe to share between threads as long as [compute] is pure.
 * - A negative [size] is an empty view; an index out of range throws [IndexOutOfBoundsException] (the [List] contract).
 *
 * It is [RandomAccess], so the cache's size estimator samples it by index instead of walking it. Such an estimate
 * charges the sampled elements as if they were stored, i.e. it over-counts a view, which is the safe direction.
 */
class ComputedList<T>(
    size: Int,
    private val compute: (index: Int) -> T,
) : AbstractList<T>(), RandomAccess {

    override val size: Int = size.coerceAtLeast(0)

    override fun get(index: Int): T {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index is out of bounds for size $size")

        return compute(index)
    }
}
