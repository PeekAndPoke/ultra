package de.peekandpoke.ultra.vault.profiling

interface QueryProfiler {

    interface StopWatch {

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

        class Impl : StopWatch {

            private val sync = Any()
            override val entriesNs = mutableListOf<Long>()

            override val totalNs get() = entriesNs.sum()

            override val count get() = entriesNs.size

            override operator fun <T> invoke(block: () -> T): T {
                val start = System.nanoTime()
                val result = block()

                synchronized(sync) {
                    entriesNs.add(System.nanoTime() - start)
                }

                return result
            }

            override suspend fun <T> async(block: suspend () -> T): T {
                val start = System.nanoTime()
                val result = block()

                synchronized(sync) {
                    entriesNs.add(System.nanoTime() - start)
                }

                return result
            }
        }

        @Suppress("MemberVisibilityCanBePrivate")
        val entriesNs: List<Long>

        val totalNs get() = entriesNs.sum()

        val count get() = entriesNs.size

        operator fun <T> invoke(block: () -> T): T

        suspend fun <T> async(block: suspend () -> T): T
    }

    interface Entry {

        object Null : Entry {
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

        class Impl(
            val connection: String,
            val queryLanguage: String,
            val query: String,
        ) : Entry {
            override var vars: Map<String, Any?>? = null

            /** The number of results returned */
            override var count: Long? = null

            /** The total number of results matching the query */
            override var totalCount: Long? = null

            /** The total number of results matching the query */
            override var queryExplained: String? = null

            override val measureQuery: StopWatch.Impl = StopWatch.Impl()
            override val measureIterator: StopWatch.Impl = StopWatch.Impl()
            override val measureSerializer: StopWatch.Impl = StopWatch.Impl()
            override val measureDeserializer: StopWatch.Impl = StopWatch.Impl()
            override val measureExplain: StopWatch.Impl = StopWatch.Impl()
        }

        var vars: Map<String, Any?>?

        /** The number of results returned */
        var count: Long?

        /** The total number of results matching the query */
        var totalCount: Long?

        /** The total number of results matching the query */
        var queryExplained: String?

        val totalNs: Long
            get() = measureQuery.totalNs +
                    measureIterator.totalNs +
                    measureSerializer.totalNs +
                    measureDeserializer.totalNs +
                    measureExplain.totalNs

        val measureQuery: StopWatch
        val measureIterator: StopWatch
        val measureSerializer: StopWatch
        val measureDeserializer: StopWatch
        val measureExplain: StopWatch
    }

    val explainQueries: Boolean

    val entries: List<Entry>

    suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (Entry) -> R,
    ): R
}
