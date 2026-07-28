package io.peekandpoke.ultra.vault

/**
 * Represents the outcome of a document removal operation.
 *
 * There is no success flag: a [count] of 0 says only that nothing was removed. It does not
 * distinguish "no document matched" from a driver that caught a removal error and reported it as 0.
 *
 * @property count the number of documents actually removed, not the number attempted.
 * @property query the [TypedQuery] that was used for the removal, or `null` if the driver removes
 *   through its own API instead of a query.
 */
data class RemoveResult(val count: Long, val query: TypedQuery<*>?) {
    companion object {
        /**
         * An empty result indicating no documents were removed.
         *
         * Also the stand-in returned by repositories that do not implement removal at all.
         */
        val empty = RemoveResult(count = 0, query = null)
    }
}
