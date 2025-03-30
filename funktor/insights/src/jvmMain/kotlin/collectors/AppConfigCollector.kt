package de.peekandpoke.ktorfx.insights.collectors

import com.fasterxml.jackson.module.kotlin.convertValue
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.model.AppInfo
import de.peekandpoke.ktorfx.insights.InsightsCollector
import de.peekandpoke.ktorfx.insights.InsightsCollectorData
import de.peekandpoke.ktorfx.insights.InsightsMapper
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
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
