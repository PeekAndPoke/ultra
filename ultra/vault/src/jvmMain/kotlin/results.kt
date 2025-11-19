package de.peekandpoke.ultra.vault

/**
 * Remove result
 */
data class RemoveResult(val count: Long, val query: TypedQuery<*>?) {
    companion object {
        val empty = RemoveResult(count = 0, query = null)
    }
}
