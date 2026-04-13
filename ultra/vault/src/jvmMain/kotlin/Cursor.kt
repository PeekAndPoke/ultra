package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

interface Cursor<T> {

    companion object {
        inline fun <reified T> empty() = empty(kType<T>())

        fun <T> empty(type: TypeRef<T>): Cursor<T> = EmptyCursor(type)

        inline fun <reified T> of(items: List<T>) =
            of(type = kType(), items = items)

        fun <T> of(type: TypeRef<T>, items: List<T>): Cursor<T> =
            Simple(items = items, query = TypedQuery.of(returns = type))
    }

    private class Simple<T>(
        val items: List<T>,
        override val query: TypedQuery<T>,
    ) : Cursor<T> {
        override fun asFlow(): Flow<T> = items.asFlow()

        override val entityCache: EntityCache = NullEntityCache
        override val count: Long get() = items.size.toLong()
        override val fullCount: Long? get() = null
        override val timeMs: Double get() = 1.0
    }

    private class EmptyCursor<T>(type: TypeRef<T>) : Cursor<T> {
        override fun asFlow(): Flow<T> = emptyFlow()

        override val entityCache: EntityCache = NullEntityCache
        override val query: TypedQuery<T> = TypedQuery.of(returns = type)
        override val count: Long = 0
        override val fullCount: Long = 0
        override val timeMs: Double = 1.0
    }

    /** The Entity Cache */
    val entityCache: EntityCache

    /** The executed query */
    val query: TypedQuery<T>

    /** The number of results that where returned */
    val count: Long

    /** The total number of results that would match the query */
    val fullCount: Long?

    /** The time the query took */
    val timeMs: Double

    /** Async iteration via Flow. For KarangoCursor, fetches chunks lazily on demand. */
    fun asFlow(): Flow<T>

    /** Collect all items. Convenience for asFlow().toList(). */
    suspend fun toList(): List<T> = asFlow().toList()
}

// Convenience extensions — suspend equivalents of stdlib collection operations /////////////////////
//
// These make Cursor feel like a regular collection. All are suspend-safe.

// Transform ///////////////////////////////////////////////////////////////////////////////////////////

/** Maps cursor items. */
suspend fun <T, R> Cursor<T>.map(transform: suspend (T) -> R): List<R> =
    asFlow().map { transform(it) }.toList()

/** Maps cursor items, discarding nulls. */
suspend fun <T, R : Any> Cursor<T>.mapNotNull(transform: suspend (T) -> R?): List<R> {
    val result = mutableListOf<R>()
    asFlow().collect { transform(it)?.let { r -> result.add(r) } }
    return result
}

/** Maps cursor items with their index. */
suspend fun <T, R> Cursor<T>.mapIndexed(transform: suspend (index: Int, T) -> R): List<R> {
    val result = mutableListOf<R>()
    var idx = 0
    asFlow().collect { result.add(transform(idx++, it)) }
    return result
}

/** Flat-maps cursor items. */
suspend fun <T, R> Cursor<T>.flatMap(transform: suspend (T) -> Iterable<R>): List<R> {
    val result = mutableListOf<R>()
    asFlow().collect { result.addAll(transform(it)) }
    return result
}

// Filter //////////////////////////////////////////////////////////////////////////////////////////////

/** Filters cursor items. */
suspend fun <T> Cursor<T>.filter(predicate: suspend (T) -> Boolean): List<T> =
    asFlow().filter { predicate(it) }.toList()

/** Filters cursor items, keeping those that do NOT match. */
suspend fun <T> Cursor<T>.filterNot(predicate: suspend (T) -> Boolean): List<T> =
    asFlow().filter { !predicate(it) }.toList()

/** Filters cursor items by type. */
suspend inline fun <reified R> Cursor<*>.filterIsInstance(): List<R> {
    val result = mutableListOf<R>()
    asFlow().collect { if (it is R) result.add(it) }
    return result
}

// Element access //////////////////////////////////////////////////////////////////////////////////////

/** Returns the first item, or null if the cursor is empty. */
suspend fun <T> Cursor<T>.firstOrNull(): T? =
    asFlow().firstOrNull()

/** Returns the first item matching [predicate], or null. */
suspend fun <T> Cursor<T>.firstOrNull(predicate: suspend (T) -> Boolean): T? =
    asFlow().filter { predicate(it) }.firstOrNull()

/** Returns the first item, or throws if the cursor is empty. */
suspend fun <T> Cursor<T>.first(): T =
    asFlow().first()

/** Returns the first item matching [predicate], or throws. */
suspend fun <T> Cursor<T>.first(predicate: suspend (T) -> Boolean): T =
    asFlow().filter { predicate(it) }.first()

/** Alias for [firstOrNull] with a predicate — matches stdlib find(). */
suspend fun <T> Cursor<T>.find(predicate: suspend (T) -> Boolean): T? =
    firstOrNull(predicate)

/** Returns the last item, or null if the cursor is empty. */
suspend fun <T> Cursor<T>.lastOrNull(): T? =
    toList().lastOrNull()

// Iteration ///////////////////////////////////////////////////////////////////////////////////////////

/** Iterates cursor items. */
suspend fun <T> Cursor<T>.forEach(action: suspend (T) -> Unit) {
    asFlow().collect { action(it) }
}

/** Iterates cursor items with their index. */
suspend fun <T> Cursor<T>.forEachIndexed(action: suspend (index: Int, T) -> Unit) {
    var idx = 0
    asFlow().collect { action(idx++, it) }
}

// Predicates //////////////////////////////////////////////////////////////////////////////////////////

/** Returns true if any item matches [predicate]. */
suspend fun <T> Cursor<T>.any(predicate: suspend (T) -> Boolean): Boolean =
    firstOrNull(predicate) != null

/** Returns true if no item matches [predicate]. */
suspend fun <T> Cursor<T>.none(predicate: suspend (T) -> Boolean): Boolean =
    !any(predicate)

/** Returns true if all items match [predicate]. */
suspend fun <T> Cursor<T>.all(predicate: suspend (T) -> Boolean): Boolean =
    !any { !predicate(it) }

// Aggregation /////////////////////////////////////////////////////////////////////////////////////////

/** Folds cursor items into a single result. */
suspend fun <T, R> Cursor<T>.fold(initial: R, operation: suspend (acc: R, T) -> R): R {
    var acc = initial
    asFlow().collect { acc = operation(acc, it) }
    return acc
}

/** Groups cursor items by a key. */
suspend fun <T, K> Cursor<T>.groupBy(keySelector: suspend (T) -> K): Map<K, List<T>> {
    val result = mutableMapOf<K, MutableList<T>>()
    asFlow().collect { result.getOrPut(keySelector(it)) { mutableListOf() }.add(it) }
    return result
}

/** Associates cursor items by a key. Last value wins for duplicate keys. */
suspend fun <T, K> Cursor<T>.associateBy(keySelector: suspend (T) -> K): Map<K, T> {
    val result = mutableMapOf<K, T>()
    asFlow().collect { result[keySelector(it)] = it }
    return result
}

/** Associates cursor items as key-value pairs. */
suspend fun <T, K, V> Cursor<T>.associate(transform: suspend (T) -> Pair<K, V>): Map<K, V> {
    val result = mutableMapOf<K, V>()
    asFlow().collect { val (k, v) = transform(it); result[k] = v }
    return result
}

/** Partitions cursor items into matching and non-matching lists. */
suspend fun <T> Cursor<T>.partition(predicate: suspend (T) -> Boolean): Pair<List<T>, List<T>> {
    val matching = mutableListOf<T>()
    val nonMatching = mutableListOf<T>()
    asFlow().collect { if (predicate(it)) matching.add(it) else nonMatching.add(it) }
    return matching to nonMatching
}

// Sorting /////////////////////////////////////////////////////////////////////////////////////////////

/** Returns cursor items sorted by [selector]. */
suspend fun <T, R : Comparable<R>> Cursor<T>.sortedBy(selector: (T) -> R?): List<T> =
    toList().sortedBy(selector)

/** Returns cursor items sorted by [selector] in descending order. */
suspend fun <T, R : Comparable<R>> Cursor<T>.sortedByDescending(selector: (T) -> R?): List<T> =
    toList().sortedByDescending(selector)

// Distinct ////////////////////////////////////////////////////////////////////////////////////////////

/** Returns distinct cursor items. */
suspend fun <T> Cursor<T>.distinct(): List<T> =
    toList().distinct()

/** Returns cursor items distinct by [selector]. */
suspend fun <T, K> Cursor<T>.distinctBy(selector: (T) -> K): List<T> =
    toList().distinctBy(selector)

// Take / Drop /////////////////////////////////////////////////////////////////////////////////////////

/** Returns the first [n] items. */
suspend fun <T> Cursor<T>.take(n: Int): List<T> =
    asFlow().toList().take(n)

/** Returns all items except the first [n]. */
suspend fun <T> Cursor<T>.drop(n: Int): List<T> =
    asFlow().toList().drop(n)

// Vault-specific //////////////////////////////////////////////////////////////////////////////////////

/** Converts the cursor into a list and puts all entries into the EntityCache. */
suspend fun <T> Cursor<Stored<T>>.cache(): List<Stored<T>> =
    map { entityCache.put(it._id, it) }
