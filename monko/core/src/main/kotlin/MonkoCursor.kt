package io.peekandpoke.monko

import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.TypedQuery

class MonkoCursor<T>(
    val entries: List<T>,
    override val query: TypedQuery<T>,
    override val entityCache: EntityCache,
    private val _fullCount: Long? = null,
    private val _timeMs: Double = 0.0,
) : Cursor<T> {

    override fun iterator(): Iterator<T> {
        return entries.iterator()
    }

    override val count: Long
        get() = entries.size.toLong()

    override val fullCount: Long?
        get() = _fullCount

    override val timeMs: Double
        get() = _timeMs
}
