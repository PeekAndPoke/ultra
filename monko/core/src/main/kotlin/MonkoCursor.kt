package de.peekandpoke.monko

import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.TypedQuery

class MonkoCursor<T>(
    val entries: List<T>,
    override val query: TypedQuery<T>,
    override val entityCache: EntityCache,
) : Cursor<T> {

    override fun iterator(): Iterator<T> {
        return entries.iterator()
    }

    override val count: Long
        get() = entries.size.toLong()

    override val fullCount: Long?
        get() = null // TODO

    override val timeMs: Double
        get() = 0.0 // TODO
}
