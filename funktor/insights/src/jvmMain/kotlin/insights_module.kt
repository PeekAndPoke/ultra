package io.peekandpoke.funktor.insights

import impl.InsightsFull
import impl.InsightsSlim
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainerOrNull
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.collectors.AppConfigCollector
import io.peekandpoke.funktor.insights.collectors.KontainerCollector
import io.peekandpoke.funktor.insights.collectors.LogCollector
import io.peekandpoke.funktor.insights.collectors.RequestCollector
import io.peekandpoke.funktor.insights.collectors.ResponseCollector
import io.peekandpoke.funktor.insights.collectors.RoutingCollector
import io.peekandpoke.funktor.insights.collectors.RuntimeCollector
import io.peekandpoke.funktor.insights.collectors.TemplateInsightsCollector
import io.peekandpoke.funktor.insights.collectors.UserCollector
import io.peekandpoke.funktor.insights.collectors.VaultCollector
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

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
    // Shared by the request and response collectors. An app carrying credentials in its own header
    // replaces this instance with one that lists it.
    instance(HeaderLogging.defaults)
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
}
