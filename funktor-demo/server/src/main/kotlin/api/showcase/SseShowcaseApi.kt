package io.peekandpoke.funktor.demo.server.api.showcase

import io.ktor.sse.*
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.demo.common.showcase.SseClockEvent
import io.peekandpoke.funktor.demo.common.showcase.SseMetricsEvent
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.format.DateTimeFormatter

class SseShowcaseApi(converter: OutgoingConverter) : ApiRoutes("showcase-sse", converter) {

    val sseClock = ShowcaseApiClient.SseClock.mount(Unit::class) {
        docs {
            name = "SSE clock stream"
        }.codeGen {
            funcName = "sseClock"
        }.authorize {
            public()
        }.handle {
            while (true) {
                val now = Instant.now()
                val event = SseClockEvent(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(now),
                    epochMs = now.toEpochMilli(),
                )
                send(ServerSentEvent(data = Json.encodeToString(event), event = "clock"))
                delay(1000)
            }
        }
    }

    val sseMetrics = ShowcaseApiClient.SseMetrics.mount(Unit::class) {
        docs {
            name = "SSE JVM metrics stream"
        }.codeGen {
            funcName = "sseMetrics"
        }.authorize {
            public()
        }.handle {
            val runtime = Runtime.getRuntime()
            val mxBean = ManagementFactory.getRuntimeMXBean()

            while (true) {
                val event = SseMetricsEvent(
                    heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
                    heapMaxMb = runtime.maxMemory() / (1024 * 1024),
                    threadCount = Thread.activeCount(),
                    uptimeSeconds = mxBean.uptime / 1000,
                )
                send(ServerSentEvent(data = Json.encodeToString(event), event = "metrics"))
                delay(2000)
            }
        }
    }
}
