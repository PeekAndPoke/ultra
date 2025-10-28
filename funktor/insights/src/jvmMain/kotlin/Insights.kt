package de.peekandpoke.funktor.insights

import de.peekandpoke.funktor.core.metrics.RequestMetricsProvider
import de.peekandpoke.funktor.core.model.InsightsConfig
import io.ktor.server.application.*
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

interface Insights : RequestMetricsProvider {

    abstract class Base : Insights {
        val startedNs: Long = System.nanoTime()

        override fun getRequestDuration(): Duration = (System.nanoTime() - startedNs).nanoseconds
    }

    val config: InsightsConfig

    override fun getRequestDuration(): Duration

    suspend fun start(call: ApplicationCall) {}

    suspend fun finish(call: ApplicationCall)

    fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? {
        return null
    }

    fun <T : InsightsCollector, R> use(cls: KClass<T>, block: T.() -> R?): R? {
        return getOrNull(cls)?.block()
    }
}


