package impl

import com.fasterxml.jackson.module.kotlin.convertValue
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.insights.CollectorData
import de.peekandpoke.funktor.insights.Insights
import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsData
import de.peekandpoke.funktor.insights.InsightsMapper
import de.peekandpoke.funktor.insights.InsightsRepository
import de.peekandpoke.ultra.common.Lookup
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        { it.request.uri.startsWith("/_/") }
    )

    private val filename: String = "records-${date}/$dateTime.json"

    override fun getRequestDetailsUri(): String = filename

    override fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? = collectors.getOrNull(cls)

    override suspend fun finish(call: ApplicationCall) {
        stopWatch.end()

        // do not record if any of the filters match
        if (filter.any { it(call) }) {
            return
        }

        // finish all collectors
        val entries = collectors.all().map { it.finish(call) }

        coroutineScope {
            launch(Dispatchers.IO) {
                delay(1)

                val data = InsightsData(
                    dateTime,
                    dateTime.toString(),
                    stopWatch,
                    entries.map {
                        CollectorData(it::class.jvmName, mapper.convertValue(it))
                    }
                )

                val content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)

                repository.putFile(path = filename, content = content)
            }
        }
    }
}
