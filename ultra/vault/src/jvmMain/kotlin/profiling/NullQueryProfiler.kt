package io.peekandpoke.ultra.vault.profiling

/**
 * Profiler that records nothing. This is the default, as `VaultConfig.profile` is off.
 *
 * Every query gets [QueryProfiler.Entry.Null], so all stop watches stay at zero. The Karango driver
 * derives `Cursor.timeMs` from those timings, which therefore reads a constant `0.0` unless
 * profiling was switched on — do not build alerting on it.
 */
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
