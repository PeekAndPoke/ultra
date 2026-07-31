package io.peekandpoke.funktor.insights

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.metrics.RequestMetricsProvider
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.rest.InsightsLevel
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** Per-request insights collector: gathers timing, profiling, and diagnostic data. */
interface Insights : RequestMetricsProvider {

    /** Base implementation providing wall-clock timing from construction. */
    abstract class Base : Insights {
        val startedNs: Long = System.nanoTime()

        override fun getRequestDuration(): Duration = (System.nanoTime() - startedNs).nanoseconds
    }

    val config: InsightsConfig

    override fun getRequestDuration(): Duration

    suspend fun start(call: ApplicationCall) {}

    /**
     * Records the request at [level].
     *
     * The level comes from the resolved route's attributes, not from the request uri — a uri is
     * client-controlled, and the substring test this replaced could be defeated by percent-encoding
     * the path or by putting the pattern in a query string.
     */
    suspend fun finish(call: ApplicationCall, level: InsightsLevel)

    fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? {
        return null
    }

    fun <T : InsightsCollector, R> use(cls: KClass<T>, block: T.() -> R?): R? {
        return getOrNull(cls)?.block()
    }
}
