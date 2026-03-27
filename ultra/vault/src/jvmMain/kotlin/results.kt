package io.peekandpoke.ultra.vault

/**
 * Represents the outcome of a document removal operation.
 *
 * @property count the number of documents that were removed.
 * @property query the [TypedQuery] that was used for the removal, or `null` if not applicable.
 */
data class RemoveResult(val count: Long, val query: TypedQuery<*>?) {
    companion object {
        /** An empty result indicating no documents were removed. */
        val empty = RemoveResult(count = 0, query = null)
    }
}
