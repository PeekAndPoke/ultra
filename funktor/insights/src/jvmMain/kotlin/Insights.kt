package de.peekandpoke.ktorfx.insights

import de.peekandpoke.ktorfx.core.metrics.RequestMetricsProvider
import de.peekandpoke.ktorfx.core.model.InsightsConfig
import io.ktor.server.application.*
import kotlin.reflect.KClass
import kotlin.time.Duration

interface Insights : RequestMetricsProvider {

    abstract class Base : Insights {
        val stopWatch: StopWatch = StopWatch()

        override fun getRequestDuration() = stopWatch.totalDuration()
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


