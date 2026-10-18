package impl

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.CollectorData
import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.InsightsOptions
import io.peekandpoke.funktor.insights.InsightsData
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.insights.InsightsCodec
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import io.peekandpoke.ultra.common.Lookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass

internal class InsightsFull(
    override val config: InsightsConfig,
    private val collectors: Lookup<InsightsCollector>,
    private val repository: InsightsRepository,
    private val codec: InsightsCodec,
) : Insights.Base() {
    private val json = Json { prettyPrint = true }

    private val date = LocalDate.now()
    private val dateTime = LocalDateTime.now()

    private val filename: String = "records-$date/$dateTime.json"

    override fun <T : InsightsCollector> getOrNull(cls: KClass<T>): T? = collectors.getOrNull(cls)

    override suspend fun finish(call: ApplicationCall, options: InsightsOptions) {
        val endedNs = System.nanoTime()

        if (options.level == InsightsLevel.OFF) {
            return
        }

        // Read off the call NOW: the write below happens on another dispatcher after the response
        val method = call.request.httpMethod.value
        val uri = call.request.path()
        val status = call.response.status()?.value

        // BRIEF records the headline only, so the collectors are never even run
        // The key comes from the COLLECTOR, so pair each with its slice rather than asking the data
        val entries: List<Pair<String, InsightsCollectorData>> = when (options.level) {
            InsightsLevel.FULL -> collectors.all().map { it.key to it.finish(call) }
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
                    collectors = entries.map { (key, slice) ->
                        // Slumber, not Jackson. Slumbering dispatches on the RUNTIME class, so the
                        // collectors' declared `Any` fields are not a blocker here even though they are
                        // one for awaking — and insights reads with kotlinx, never with Slumber.
                        CollectorData(key, codec.slumber(slice) as? Map<*, *> ?: emptyMap<String, Any?>())
                    }
                )

                val content = json.encodeToString(
                    JsonElement.serializer(),
                    codec.slumber(data).toJsonElement(),
                )

                repository.putFile(path = filename, content = content)
            }
        }
    }
}
