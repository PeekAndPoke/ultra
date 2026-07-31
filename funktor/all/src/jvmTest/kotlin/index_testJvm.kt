package io.peekandpoke.funktor

import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.funktorApp
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
    override val keys: Map<String, String> = emptyMap(),
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

inline fun <C : AppConfig> AppSpecAware<C>.apiApp(block: AppUnderTest<C>.() -> Unit) =
    testApp(host = "api.funktor.local").block()

/**
 * The same API surface as [apiApp], mounted a second time with insights **recording**.
 *
 * Insights is not on the `api.*` host because recording is expensive — a FULL record serialises the
 * whole kontainer and app config, ~270 KB per request measured on real data — and every spec in this
 * module would pay it for coverage only one spec wants. The routes, auth chain and handlers are the
 * very same ones.
 *
 * **The record-writing half is what this isolates, not all of the cost.** `instrumentWithInsights`
 * calls `registerTracer()`, which walks to the application's root `Routing` and installs a `trace { }`
 * there — so every request on `api.*` builds a resolve trace too. That is application-global by
 * construction and a second host cannot contain it.
 */
inline fun <C : AppConfig> AppSpecAware<C>.insightsApp(block: AppUnderTest<C>.() -> Unit) =
    testApp(host = "insights.funktor.local").block()
