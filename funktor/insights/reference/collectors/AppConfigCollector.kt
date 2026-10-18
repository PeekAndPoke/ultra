package io.peekandpoke.funktor.insights.collectors

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.server.application.*
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import io.peekandpoke.funktor.insights.InsightsMapper
import io.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui

class AppConfigCollector(
    mapper: InsightsMapper,
    appConfig: AppConfig? = null,
    appInfo: AppInfo? = null,
) : InsightsCollector {

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

    data class Data(val info: Any, val config: Any) : InsightsCollectorData {

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.cog()
                +"Config"
            }

            content {
                ui.header H4 { +"AppInfo" }
                json(info)

                ui.header H4 { +"Config" }
                json(config)
            }
        }
    }

    override fun finish(call: ApplicationCall): Data = static
}
