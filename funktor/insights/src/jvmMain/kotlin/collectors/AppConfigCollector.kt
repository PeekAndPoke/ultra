package io.peekandpoke.funktor.insights.collectors

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsMapper

class AppConfigCollector(
    mapper: InsightsMapper,
    appConfig: AppConfig? = null,
    appInfo: AppInfo? = null,
) : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "app-config"
    }

    val static = Data(
        info = when {
            appInfo != null -> mapper.convertValue<Map<*, *>>(appInfo)
            else -> "not available"
        },
        config = when {
            appConfig != null -> mapper.convertValue<Map<*, *>>(appConfig)
            else -> "not available"
        }
    )

    /** VUE-REF: `reference/collectors/AppConfigCollector.kt` */
    data class Data(val info: Any, val config: Any) : InsightsCollectorData {
    }

    override fun finish(call: ApplicationCall): Data = static
}
