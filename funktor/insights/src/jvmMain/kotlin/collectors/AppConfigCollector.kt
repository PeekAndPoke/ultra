package de.peekandpoke.funktor.insights.collectors

import com.fasterxml.jackson.module.kotlin.convertValue
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.model.AppInfo
import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.InsightsMapper
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.server.application.*

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
