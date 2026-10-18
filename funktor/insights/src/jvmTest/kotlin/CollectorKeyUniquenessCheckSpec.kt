package io.peekandpoke.funktor.insights

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.core.lifecycle.AppStartException
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
import io.peekandpoke.ultra.common.SimpleLookup

/**
 * The duplicate-key check must **abort** the boot, not merely complain about it.
 *
 * That distinction is the whole point. Until 2026-07-31 this check called `error()`, i.e. threw
 * `IllegalStateException` — and `AppLifeCycleBuilder.register` runs `OnAppStarting` hooks with
 * `rethrow = { it is AppStartException }`, catching everything else into a log line
 * (`AppLifeCycleBuilder.kt:39,91-97`, pinned by `AppLifeCycleSpec`). So the app started anyway, and the
 * task file recorded the finding as fixed. All three round-2 reviewers found it independently.
 *
 * The assertion is therefore on the exception TYPE. `shouldThrow<Exception>` would have passed against
 * the broken version and proved nothing.
 */
class CollectorKeyUniquenessCheckSpec : StringSpec({

    // Three DISTINCT classes on purpose: `SimpleLookup` keys by runtime class, so two instances of one
    // class silently collapse into a single entry and no duplicate would ever be seen.
    class Alpha(override val key: String) : InsightsCollector {
        override fun finish(call: ApplicationCall) = Slice
    }

    class Beta(override val key: String) : InsightsCollector {
        override fun finish(call: ApplicationCall) = Slice
    }

    class Gamma(override val key: String) : InsightsCollector {
        override fun finish(call: ApplicationCall) = Slice
    }

    fun checkOver(vararg collectors: InsightsCollector) =
        CollectorKeyUniquenessCheck(SimpleLookup { collectors.toList() })

    /** Hands back a real [Application]; the check ignores it, but the signature demands one. */
    suspend fun withApplication(block: suspend (Application) -> Unit) {
        var app: Application? = null

        testApplication {
            application { app = this }
            client.config { }
        }

        block(app ?: error("the test application never started"))
    }

    "a duplicate key aborts the boot with AppStartException" {
        withApplication { app ->
            val thrown = shouldThrow<AppStartException> {
                checkOver(Alpha("request"), Beta("response"), Gamma("request")).onAppStarting(app)
            }

            thrown.message shouldContain "'request'"
            // names both offenders, so the message points at the collision instead of merely reporting one
            thrown.message shouldContain "Alpha"
            thrown.message shouldContain "Gamma"
            // and does not drag in the innocent one
            (thrown.message?.contains("Beta") ?: true) shouldBe false
        }
    }

    "distinct keys pass" {
        withApplication { app ->
            checkOver(Alpha("request"), Beta("response"), Gamma("user")).onAppStarting(app)
        }
    }

    "the built-in collectors declare ten distinct keys" {
        // Read off the collectors themselves, not a list written here — a new built-in that reuses an
        // existing key fails this rather than someone's boot.
        val keys = listOf(
            RequestCollector.KEY, ResponseCollector.KEY, UserCollector.KEY, RoutingCollector.KEY,
            AppConfigCollector.KEY, KontainerCollector.KEY, RuntimeCollector.KEY, LogCollector.KEY,
            VaultCollector.KEY, TemplateInsightsCollector.KEY,
        )

        keys.distinct() shouldBe keys
        keys.size shouldBe 10
    }
}) {
    private object Slice : InsightsCollectorData
}
