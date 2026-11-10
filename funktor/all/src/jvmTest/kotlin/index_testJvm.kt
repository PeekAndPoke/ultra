package io.peekandpoke.funktor

import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.InsightsFileRepository
import io.peekandpoke.funktor.insights.InsightsRepository
import io.peekandpoke.funktor.testing.AppSpecAware
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.io.path.createTempDirectory

data class FunktorAllTestConfig(
    override val ktor: KtorConfig,
    override val funktor: FunktorConfig = FunktorConfig(),
    override val keys: Map<String, Redacted<String>> = emptyMap(),
) : AppConfig

fun createBlueprint(config: FunktorAllTestConfig) = kontainer {
    funktor(
        config = config,
        rest = {
            jwt()
        },
        logging = {
            useKarango()
        },
        cluster = {
            useKarango()
        },
        messaging = {
            useKarango()
        },
        saas = {
            useKarango()
            ensureOrganisation(slug = "system-default", name = "System Default")
        },
        auth = {
            useKarango()
        },
    )

    karango(config = ArangoDbConfig.forUnitTests)

    // Insights writes to `./tmp/depot/insights` by default — a RELATIVE path, so a suite run on a
    // machine where the demo has run would read the demo's leftovers and behave differently per
    // machine. Rooting it in a fresh temp directory makes "the depot holds exactly what this run
    // recorded" a fact the tests can assert on.
    dynamic(InsightsRepository::class) { InsightsFileRepository(insightsDepotDir) }

    module(TestUserModule)
}

/** Throw-away depot root for the insights records this suite records. One per JVM run. */
val insightsDepotDir: String by lazy {
    createTempDirectory("funktor-all-insights").toFile()
        // FULL records are ~270 KB; without this every run leaves them in /tmp forever
        .also { it.deleteOnExit(); Runtime.getRuntime().addShutdownHook(Thread { it.deleteRecursively() }) }
        .absolutePath
}

val testApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config)
        )
    },
)

/**
 * The same app with its insights switch OFF (`funktor.insights.enabled = false`, ULTRA-00301): the insights API
 * answers 404 on every host, and nothing records. The config file turns it on for the suite, so the blueprint
 * overrides it; `instrumentWithInsights(app)` and the API read the blueprint's config, not the file's.
 */
val insightsOffTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config.copy(funktor = config.funktor.copy(insights = InsightsConfig.Disabled)))
        )
    },
)

inline fun <C : AppConfig> AppSpecAware<C>.apiApp(block: AppUnderTest<C>.() -> Unit) =
    testApp(host = "api.funktor.local").block()

/**
 * The same API surface as [apiApp], mounted a second time with insights **recording**.
 *
 * The app's insights switch is on for the whole suite, so the insights API answers on both hosts (ULTRA-00301);
 * only this host INSTRUMENTS its routes, because recording is expensive — a FULL record serialises the whole
 * kontainer and app config, ~270 KB per request measured on real data — and every spec in this module would pay
 * it for coverage only one spec wants. The routes, auth chain and handlers are the very same ones.
 *
 * **The record-writing half is what this isolates, not all of the cost.** `instrumentWithInsights`
 * calls `registerTracer()`, which walks to the application's root `Routing` and installs a `trace { }`
 * there — so every request on `api.*` builds a resolve trace too. That is application-global by
 * construction and a second host cannot contain it.
 */
inline fun <C : AppConfig> AppSpecAware<C>.insightsApp(block: AppUnderTest<C>.() -> Unit) =
    testApp(host = "insights.funktor.local").block()
