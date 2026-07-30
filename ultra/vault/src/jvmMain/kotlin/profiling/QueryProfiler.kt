package io.peekandpoke.ultra.vault.profiling

/**
 * Collects timings and metadata for the queries executed through the vault drivers.
 *
 * Installed by the `Ultra_Vault` kontainer module as a dynamic service — one profiler per request —
 * and picked by `VaultConfig.profile`: [DefaultQueryProfiler] when it is on, [NullQueryProfiler]
 * (the default) when it is off. All timings are nanoseconds.
 */
interface QueryProfiler {

    /** Measures how long blocks take, keeping one timing per measured block. */
    interface StopWatch {

        /** Stop watch that runs the block but records nothing. */
        object Null : StopWatch {
            override val entriesNs: List<Long> = emptyList()

            override val totalNs: Long = 0

            override val count: Int = 0

            override operator fun <T> invoke(block: () -> T): T {
                return block()
            }

            override suspend fun <T> async(block: suspend () -> T): T {
                return block()
            }
        }

        /** Stop watch that records the duration of every measured block. */
        class Impl : StopWatch {

            private val sync = Any()
            override val entriesNs = mutableListOf<Long>()

            override val totalNs get() = entriesNs.sum()

            override val count get() = entriesNs.size

            override operator fun <T> invoke(block: () -> T): T {
                val start = System.nanoTime()

                // a query that fails is exactly the one worth timing, so record it either way
                try {
                    return block()
                } finally {
                    synchronized(sync) {
                        entriesNs.add(System.nanoTime() - start)
                    }
                }
            }

            override suspend fun <T> async(block: suspend () -> T): T {
                val start = System.nanoTime()

                try {
                    return block()
                } finally {
                    synchronized(sync) {
                        entriesNs.add(System.nanoTime() - start)
                    }
                }
            }
        }

        /** Duration of each measured block in nanoseconds, in the order the blocks finished. */
        @Suppress("MemberVisibilityCanBePrivate")
        val entriesNs: List<Long>

        /** Sum of [entriesNs] in nanoseconds. */
        val totalNs get() = entriesNs.sum()

        /** Number of blocks measured. */
        val count get() = entriesNs.size

        /** Measures [block] and returns its result. */
        operator fun <T> invoke(block: () -> T): T

        /** Measures the suspending [block] and returns its result. */
        suspend fun <T> async(block: suspend () -> T): T
    }

    /** Timings and metadata of a single query. */
    interface Entry {

        /** Entry that accepts every value and discards it. Handed out by [NullQueryProfiler]. */
        object Null : Entry {
            override var query: String? get() = null; set(_) {}

            override var vars: Map<String, Any?>? get() = null; set(_) {}

            override var count: Long? get() = null; set(_) {}

            override var totalCount: Long? get() = null; set(_) {}

            override var queryExplained: String? get() = null; set(_) {}

            override val measureQuery: StopWatch = StopWatch.Null
            override val measureIterator: StopWatch = StopWatch.Null
            override val measureSerializer: StopWatch = StopWatch.Null
            override val measureDeserializer: StopWatch = StopWatch.Null
            override val measureExplain: StopWatch = StopWatch.Null
        }

        /**
         * Entry that records everything the driver reports.
         *
         * @param connection Name of the database connection, e.g. `ArangoDB::my-db`.
         * @param queryLanguage Language of [query], e.g. `aql` or `json`.
         */
        class Impl(
            val connection: String,
            val queryLanguage: String,
            override var query: String? = null,
        ) : Entry {
            override var vars: Map<String, Any?>? = null

            /** The number of results returned */
            override var count: Long? = null

            /** The total number of results matching the query */
            override var totalCount: Long? = null

            /** The query plan as returned by EXPLAIN */
            override var queryExplained: String? = null

            override val measureQuery: StopWatch.Impl = StopWatch.Impl()
            override val measureIterator: StopWatch.Impl = StopWatch.Impl()
            override val measureSerializer: StopWatch.Impl = StopWatch.Impl()
            override val measureDeserializer: StopWatch.Impl = StopWatch.Impl()
            override val measureExplain: StopWatch.Impl = StopWatch.Impl()
        }

        /** The query as sent to the database */
        var query: String?

        /** The bind variables of [query] */
        var vars: Map<String, Any?>?

        /** The number of results returned */
        var count: Long?

        /** The total number of results matching the query */
        var totalCount: Long?

        /** The query plan as returned by EXPLAIN. Only filled when [QueryProfiler.explainQueries] is on. */
        var queryExplained: String?

        /** Sum of all stop watches below, in nanoseconds. */
        val totalNs: Long
            get() = measureQuery.totalNs +
                    measureIterator.totalNs +
                    measureSerializer.totalNs +
                    measureDeserializer.totalNs +
                    measureExplain.totalNs

        /** Time spent executing the query on the database. */
        val measureQuery: StopWatch

        /** Time spent iterating the results. No driver writes this one, so it stays at zero. */
        val measureIterator: StopWatch

        /** Time spent serialising the query. No driver writes this one, so it stays at zero. */
        val measureSerializer: StopWatch

        /** Time spent turning the raw results into entities. */
        val measureDeserializer: StopWatch

        /** Time spent on the EXPLAIN round trip. Zero unless [QueryProfiler.explainQueries] is on. */
        val measureExplain: StopWatch
    }

    /**
     * Whether drivers should also ask the database to EXPLAIN each query.
     *
     * Costs one extra round trip per query. Honoured by the Karango and Monko drivers.
     */
    val explainQueries: Boolean

    /** All entries recorded so far, in the order the queries were started. */
    val entries: List<Entry>

    /**
     * Runs [block] with the [Entry] that collects the timings of one query.
     *
     * @param connection Name of the database connection, e.g. `ArangoDB::my-db`.
     * @param queryLanguage Language of [query], e.g. `aql` or `json`.
     * @param query The query text. Drivers that only build it later pass an empty string here and
     *   set [Entry.query] instead.
     */
    suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (Entry) -> R,
    ): R
}
