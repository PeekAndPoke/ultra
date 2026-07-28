package io.peekandpoke.ultra.vault

/**
 * Extension interface for repositories that support inserting multiple documents in a single operation.
 *
 * Implementations should execute a single database round-trip for all values, making batch inserts
 * significantly more efficient than inserting documents one by one.
 *
 * @param T the entity type managed by this repository.
 */
interface BatchInsertRepository<T> {

    /**
     * Inserts multiple elements with a single query.
     *
     * The result is a list of the inserted values.
     *
     * Implementations MUST return the results in the same order as [values] — callers zip the two
     * lists by index, so an out-of-order implementation mis-assigns results silently.
     */
    suspend fun <X : T> batchInsert(values: List<New<X>>): List<Stored<X>>

    /**
     * Inserts multiple element with a single query.
     *
     * The result is a list of the inserted values, in the same order as [values].
     */
    suspend fun <X : T> batchInsertValues(values: List<X>): List<Stored<X>> {
        val mapped = values.map { New(_value = it) }

        return batchInsert(mapped)
    }

    /**
     * Inserts multiple element with a single query.
     *
     * The [values] are pair of KEY to VALUE, where the KEY will be used as the _id / _key of the document.
     *
     * The result is a list of the inserted values, in the same order as [values].
     */
    suspend fun <X : T> batchInsertPairs(values: List<Pair<String, X>>): List<Stored<X>> {
        val mapped = values.map { New(_key = it.first.ensureKey, _value = it.second) }

        return batchInsert(mapped)
    }
}
