package de.peekandpoke.ultra.vault

/**
 * Defines a repository that supports batch inserts
 */
interface BatchInsertRepository<T> {

    /**
     * Inserts multiple elements with a single query.
     *
     * The result is a list of the inserted values.
     * The order of the [values] must match the order of the results.
     */
    suspend fun <X : T> batchInsert(values: List<New<X>>): List<Stored<X>>

    /**
     * Inserts multiple element with a single query.
     *
     * The result is a list of the inserted values.
     * The order of the [values] must match the order of the results.
     */
    suspend fun <X : T> batchInsertValues(values: List<X>): List<Stored<X>> {
        val mapped = values.map { New(value = it) }

        return batchInsert(mapped)
    }

    /**
     * Inserts multiple element with a single query.
     *
     * The [values] are pair of KEY to VALUE, where the KEY will be used as the _id / _key of the document.
     *
     * The result is a list of the inserted values.
     * The order of the [values] must match the order of the results.
     */
    suspend fun <X : T> batchInsertPairs(values: List<Pair<String, X>>): List<Stored<X>> {
        val mapped = values.map { New(_key = it.first.ensureKey, value = it.second) }

        return batchInsert(mapped)
    }
}
