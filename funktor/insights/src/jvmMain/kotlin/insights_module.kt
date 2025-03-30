package de.peekandpoke.ktorfx.insights

import de.peekandpoke.ktorfx.core.kontainerOrNull
import de.peekandpoke.ktorfx.core.model.InsightsConfig
import de.peekandpoke.ktorfx.insights.collectors.AppConfigCollector
import de.peekandpoke.ktorfx.insights.collectors.KontainerCollector
import de.peekandpoke.ktorfx.insights.collectors.LogCollector
import de.peekandpoke.ktorfx.insights.collectors.RequestCollector
import de.peekandpoke.ktorfx.insights.collectors.ResponseCollector
import de.peekandpoke.ktorfx.insights.collectors.RoutingCollector
import de.peekandpoke.ktorfx.insights.collectors.RuntimeCollector
import de.peekandpoke.ktorfx.insights.collectors.TemplateInsightsCollector
import de.peekandpoke.ktorfx.insights.collectors.UserCollector
import de.peekandpoke.ktorfx.insights.collectors.VaultCollector
import de.peekandpoke.ktorfx.insights.gui.InsightsBarWebResources
import de.peekandpoke.ktorfx.insights.gui.InsightsGui
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiRoutes
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ktorfx.insights.gui.InsightsGuiWebResources
import de.peekandpoke.ktorfx.insights.gui.InsightsRenderer
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import impl.InsightsFull
import impl.InsightsSlim
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun KontainerBuilder.ktorFxInsights() = module(KtorFX_Insights)

val KontainerAware.ktorFxInsights get() = kontainer.getOrNull(Insights::class)
val ApplicationCall.ktorFxInsights get() = kontainerOrNull?.ktorFxInsights
val RoutingContext.ktorFxInsights get() = call.ktorFxInsights

val KtorFX_Insights = module {
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

