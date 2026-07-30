package io.peekandpoke.funktor.insights.gui

import io.ktor.http.*
import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.collectors.RequestCollector
import io.peekandpoke.funktor.insights.collectors.ResponseCollector
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

data class InsightsGuiData(
    val ts: LocalDateTime,
    val date: String,
    val startedNs: Long,
    val endedNs: Long,
    val collectors: List<InsightsCollectorData>,
    val nextFile: DepotItem?,
    val previousFile: DepotItem?,
) {
    val statusCode: HttpStatusCode? by lazy {
        use(ResponseCollector.Data::class) { status }
    }

    val requestUrl: String by lazy {
        use(RequestCollector.Data::class) { fullUrl } ?: "???"
    }

    val requestMethod: String by lazy {
        use(RequestCollector.Data::class) { method.value } ?: "???"
    }

    val responseDuration: Duration by lazy {
        (endedNs - startedNs).nanoseconds
    }

    val responseTimeMs: String by lazy {
        "%.2f ms".format(responseDuration.inWholeNanoseconds / 1_000_000.0)
    }

    fun <T : InsightsCollectorData, R> use(cls: KClass<T>, block: T.() -> R?): R? {

        @Suppress("UNCHECKED_CAST")
        val collector = collectors.firstOrNull { it::class == cls } as T?

        return collector?.block()
    }
}
