package impl

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.CollectorData
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.rest.InsightsLevel
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

    private val filename: String = "records-$date/$dateTime.json"

    override fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? = collectors.getOrNull(cls)

    override suspend fun finish(call: ApplicationCall, level: InsightsLevel) {
        val endedNs = System.nanoTime()

        if (level == InsightsLevel.OFF) {
            return
        }

        // Read off the call NOW: the write below happens on another dispatcher after the response
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val status = call.response.status()?.value

        // BRIEF records the headline only, so the collectors are never even run
        val entries = when (level) {
            InsightsLevel.FULL -> collectors.all().map { it.finish(call) }
            else -> emptyList()
        }

        supervisorScope {
            launch(Dispatchers.IO) {
                delay(1)

                val data = InsightsData(
                    ts = dateTime,
                    date = dateTime.toString(),
                    startedNs = startedNs,
                    endedNs = endedNs,
                    method = method,
                    uri = uri,
                    status = status,
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
