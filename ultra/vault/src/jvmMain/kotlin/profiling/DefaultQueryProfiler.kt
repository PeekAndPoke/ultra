package io.peekandpoke.ultra.vault.profiling

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
