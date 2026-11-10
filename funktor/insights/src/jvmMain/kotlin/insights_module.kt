package io.peekandpoke.funktor.insights

import impl.InsightsDisabled
import impl.InsightsFull
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainerOrNull
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
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
    dynamic(Insights::class) { appConfig: AppConfig, kontainer: Kontainer ->
        // One switch for the whole app (ULTRA-00301); the environment decides too (ULTRA-00225)
        val config = appConfig.funktor.insights

        when (config.isActiveIn(appConfig.ktor)) {
            false -> InsightsDisabled(config)

            true -> {
                val collectors = kontainer.getLookup(InsightsCollector::class)

                val repository = kontainer.getOrNull(InsightsRepository::class)
                    ?: return@dynamic InsightsDisabled(config)

                val codec = kontainer.getOrNull(InsightsCodec::class)
                    ?: return@dynamic InsightsDisabled(config)

                InsightsFull(
                    config = config,
                    collectors = collectors,
                    repository = repository,
                    codec = codec,
                )
            }
        }
    }

    instance(InsightsCodec())

    dynamic(InsightsDataLoader::class)
    // Shared by the request and response collectors. An app carrying credentials in its own header
    // replaces this instance with one that lists it.
    instance(HeaderLogging.defaults)
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

    // Fails the boot on duplicate collector keys — see the class for why it cannot wait
    singleton(CollectorKeyUniquenessCheck::class)
    // Fails the boot when the insights switch is on outside development, instrumented routes or not
    singleton(InsightsEnvironmentCheck::class)

    // The static graphs. A true singleton, so the kontainer graph is built once per blueprint
    singleton(InsightsGraphs::class)

    // The API — superuser-gated, and the only way to reach the recorded data
    singleton(InsightsApiFeature::class)
}
