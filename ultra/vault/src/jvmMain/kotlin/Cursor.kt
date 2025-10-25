package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

interface Cursor<T> : Iterable<T> {

    companion object {
        inline fun <reified T> empty() = empty(kType<T>())

        fun <T> empty(type: TypeRef<T>): Cursor<T> = EmptyCursor(type)

        inline fun <reified T> of(items: Iterable<T>) =
            of(type = kType(), items = items)

        fun <T> of(type: TypeRef<T>, items: Iterable<T>): Cursor<T> =
            Simple(
                items = items,
                query = TypedQuery.of(type),
            )
    }

    private class Simple<T>(
        val items: Iterable<T>,
        override val query: TypedQuery<T>,
    ) : Cursor<T> {
        override fun iterator(): Iterator<T> {
            return items.iterator()
        }

        override val entityCache: EntityCache = NullEntityCache
        override val count: Long get() = items.count().toLong()
        override val fullCount: Long? get() = null
        override val timeMs: Double get() = 1.0
    }

    private class EmptyCursor<T>(type: TypeRef<T>) : Cursor<T> {
        override fun iterator(): Iterator<T> {
            return emptyList<T>().listIterator()
        }

        override val entityCache: EntityCache = NullEntityCache
        override val query: TypedQuery<T> = TypedQuery.of(type)
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
}

/**
 * Converts the cursor into a list a puts all entries into the EntityCache
 */
fun <T> Cursor<Stored<T>>.cache(): List<Stored<T>> {
    return map { entityCache.put(it._id, it) }
}
