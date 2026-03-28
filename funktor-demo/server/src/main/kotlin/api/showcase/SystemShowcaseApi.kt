package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.demo.common.showcase.AppLifecycleInfo
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.format.DateTimeFormatter

class SystemShowcaseApi(converter: OutgoingConverter) : ApiRoutes("showcase-system", converter) {

    val getAppLifecycle = ShowcaseApiClient.GetAppLifecycle.mount {
        docs {
            name = "Get app lifecycle info"
        }.codeGen {
            funcName = "getAppLifecycle"
        }.authorize {
            public()
        }.handle {
            val mxBean = ManagementFactory.getRuntimeMXBean()
            val startTime = Instant.ofEpochMilli(mxBean.startTime)

            ApiResponse.ok(
                AppLifecycleInfo(
                    startedAt = DateTimeFormatter.ISO_INSTANT.format(startTime),
                    uptimeSeconds = mxBean.uptime / 1000,
                    jvmVersion = System.getProperty("java.version") ?: "?",
                    kotlinVersion = KotlinVersion.CURRENT.toString(),
                )
            )
        }
    }
}
