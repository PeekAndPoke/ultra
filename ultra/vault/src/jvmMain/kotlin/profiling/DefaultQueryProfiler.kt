package io.peekandpoke.ultra.vault.profiling

/**
 * Profiler that records a [QueryProfiler.Entry] for every query.
 *
 * Installed by the `Ultra_Vault` module when `VaultConfig.profile` is on. It is a dynamic service,
 * so there is one profiler per request — [entries] keep growing for its whole lifetime and are
 * never pruned.
 *
 * @param explainQueries Also ask the database to EXPLAIN each query, at the cost of one extra
 *   round trip per query.
 */
class DefaultQueryProfiler(
    override val explainQueries: Boolean,
) : QueryProfiler {
    private val lock = Any()

    override var entries: List<QueryProfiler.Entry> = emptyList()
        private set

    override suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (QueryProfiler.Entry) -> R,
    ): R {
        val entry = QueryProfiler.Entry.Impl(
            connection = connection,
            queryLanguage = queryLanguage,
            query = query
        )

        synchronized(lock) {
            entries = entries.plus(entry)
        }

        return block(entry)
    }
}
