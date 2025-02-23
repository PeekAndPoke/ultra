package de.peekandpoke.ultra.vault.profiling

class DefaultQueryProfiler(
    override val explainQueries: Boolean,
) : QueryProfiler {
    override var entries: List<QueryProfiler.Entry> = emptyList()

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

        entries = entries.plus(entry)

        return block(entry)
    }
}
