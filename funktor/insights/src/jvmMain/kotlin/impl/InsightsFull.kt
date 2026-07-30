package impl

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.CollectorData
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.InsightsCollector
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
    private val date = LocalDate.now()
    private val dateTime = LocalDateTime.now()

    // TODO: make injectable
    private val filter = listOf<(ApplicationCall) -> Boolean>(
        { it.request.uri.contains("favicon.ico") },
        // The insights API must not observe itself: recording a call to it writes a record whose
        // own request/response headers describe the superuser reading insights.
        { it.request.uri.contains("/insights") }
    )

    private val filename: String = "records-$date/$dateTime.json"

    override fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? = collectors.getOrNull(cls)

    override suspend fun finish(call: ApplicationCall) {
        val endedNs = System.nanoTime()

        // do not record if any of the filters match
        if (filter.any { it(call) }) {
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
