package impl

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.CollectorData
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.api.InsightsApi
import io.peekandpoke.funktor.insights.InsightsData
import io.peekandpoke.funktor.insights.InsightsMapper
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.ultra.common.Lookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal class InsightsFull(
    override val config: InsightsConfig,
    private val collectors: Lookup<InsightsCollector>,
    private val repository: InsightsRepository,
    private val mapper: InsightsMapper,
) : Insights.Base() {
    companion object {
        /**
         * Uris that are never recorded.
         *
         * The insights entries matter: recording a call to the insights API would write a record whose
         * request and response headers describe the superuser who was *reading* insights — and every
         * such read would append another record, so a browsing session inflates the depot with
         * observations of itself. Extracted from the instance so it can be tested without booting an
         * application.
         *
         * Matched against [InsightsApi.base] rather than a loose `"/insights"`, which would also have
         * swallowed an application's own routes — `/api/insights-dashboard` contains it.
         */
        // TODO: make injectable
        fun isExcluded(uri: String): Boolean =
            uri.contains("favicon.ico") || uri.contains(InsightsApi.base)
    }

    private val date = LocalDate.now()
    private val dateTime = LocalDateTime.now()

    private val filename: String = "records-$date/$dateTime.json"

    override fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? = collectors.getOrNull(cls)

    override suspend fun finish(call: ApplicationCall) {
        val endedNs = System.nanoTime()

        // do not record if the uri is excluded
        if (isExcluded(call.request.uri)) {
            return
        }

        // finish all collectors
        val entries = collectors.all().map { it.finish(call) }

        supervisorScope {
            launch(Dispatchers.IO) {
                delay(1)

                val data = InsightsData(
                    ts = dateTime,
                    date = dateTime.toString(),
                    startedNs = startedNs,
                    endedNs = endedNs,
                    collectors = entries.map {
                        CollectorData(it.key, mapper.convertValue(it))
                    }
                )

                val content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)

                repository.putFile(path = filename, content = content)
            }
        }
    }
}
