package io.peekandpoke.ultra.vault.profiling

/**
 * Wraps [inner] so that the queries profiled through it keep their timings and text, but not their values.
 *
 * For a repository that holds credentials (password hashes, token hashes, mails with links): its driver is
 * built with this profiler, so every query of that repository is covered, with no per-call switch to forget.
 * Each entry is marked [QueryProfiler.Entry.valuesWithheld] before the driver reports into it, and no EXPLAIN
 * runs, because the database fills the plan with the values.
 */
class ValuesWithheldQueryProfiler(private val inner: QueryProfiler) : QueryProfiler {

    override val explainQueries: Boolean get() = false

    override val entries: List<QueryProfiler.Entry> get() = inner.entries

    override suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (QueryProfiler.Entry) -> R,
    ): R = inner.profile(connection = connection, queryLanguage = queryLanguage, query = query) { entry ->
        (entry as? QueryProfiler.Entry.Impl)?.valuesWithheld = true

        block(entry)
    }
}

/** This profiler with the values of its queries withheld; see [ValuesWithheldQueryProfiler]. */
fun QueryProfiler.withholdingValues(): QueryProfiler = when (this) {
    is ValuesWithheldQueryProfiler -> this
    else -> ValuesWithheldQueryProfiler(this)
}
