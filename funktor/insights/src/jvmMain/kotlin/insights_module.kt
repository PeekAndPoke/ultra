package de.peekandpoke.funktor.insights

import de.peekandpoke.funktor.core.kontainerOrNull
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.insights.collectors.AppConfigCollector
import de.peekandpoke.funktor.insights.collectors.KontainerCollector
import de.peekandpoke.funktor.insights.collectors.LogCollector
import de.peekandpoke.funktor.insights.collectors.RequestCollector
import de.peekandpoke.funktor.insights.collectors.ResponseCollector
import de.peekandpoke.funktor.insights.collectors.RoutingCollector
import de.peekandpoke.funktor.insights.collectors.RuntimeCollector
import de.peekandpoke.funktor.insights.collectors.TemplateInsightsCollector
import de.peekandpoke.funktor.insights.collectors.UserCollector
import de.peekandpoke.funktor.insights.collectors.VaultCollector
import de.peekandpoke.funktor.insights.gui.InsightsBarWebResources
import de.peekandpoke.funktor.insights.gui.InsightsGui
import de.peekandpoke.funktor.insights.gui.InsightsGuiRoutes
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiWebResources
import de.peekandpoke.funktor.insights.gui.InsightsRenderer
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import impl.InsightsFull
import impl.InsightsSlim
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.funktorInsights() = module(Funktor_Insights)

val KontainerAware.funktorInsights get() = kontainer.getOrNull(Insights::class)
val ApplicationCall.funktorInsights get() = kontainerOrNull?.funktorInsights
val RoutingContext.funktorInsights get() = call.funktorInsights

val Funktor_Insights = module {
    dynamic(InsightsConfig::class) { InsightsConfig.Disabled }

    dynamic(Insights::class) { config: InsightsConfig, kontainer: Kontainer ->
        when (config.enabled) {
            false -> InsightsSlim(config)

            true -> {
                val collectors = kontainer.getLookup(InsightsCollector::class)

                val repository = kontainer.getOrNull(InsightsRepository::class)
                    ?: return@dynamic InsightsSlim(config)

                val mapper = kontainer.getOrNull(InsightsMapper::class)
                    ?: return@dynamic InsightsSlim(config)

                InsightsFull(
                    config = config,
                    collectors = collectors,
                    repository = repository,
                    mapper = mapper,
                )
            }
        }
    }

    dynamic(InsightsDataLoader::class)
    instance(InsightsMapper())
    dynamic(InsightsRepository::class) { InsightsFileRepository() }

    // Default collectors
    dynamic(UserCollector::class)
    dynamic(RequestCollector::class)
    dynamic(ResponseCollector::class)
    dynamic(RoutingCollector::class)
    dynamic(AppConfigCollector::class)
    dynamic(KontainerCollector::class)
    dynamic(RuntimeCollector::class)
    dynamic(LogCollector::class)
    dynamic(LogCollector.Appender::class)
    dynamic(VaultCollector::class)
    dynamic(TemplateInsightsCollector::class)

    // Insights Bar
    dynamic(InsightsRenderer::class)
    singleton(InsightsBarWebResources::class)

    // Insights Gui
    singleton(InsightsGuiWebResources::class)
    singleton(InsightsGuiRoutes::class)
    singleton(InsightsGui::class)
    prototype(InsightsGuiTemplate::class)
}
