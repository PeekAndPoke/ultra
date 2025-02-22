package de.peekandpoke.ultra.vault.profiling

object NullQueryProfiler : QueryProfiler {

    override val explainQueries: Boolean = false

    override val entries: List<QueryProfiler.Entry> get() = emptyList()

    override suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (QueryProfiler.Entry) -> R,
    ): R {
        return block(QueryProfiler.Entry.Null)
    }
}
