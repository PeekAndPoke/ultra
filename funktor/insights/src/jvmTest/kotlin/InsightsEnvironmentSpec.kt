package io.peekandpoke.funktor.insights

import impl.InsightsDisabled
import impl.InsightsFull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain as shouldContainElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.funktor.insights.collectors.RuntimeCollector
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.security.user.UserProvider
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import io.peekandpoke.ultra.vault.profiling.QueryProfiler

/** Insights runs only in a development environment, and refuses to boot when a production config asks for it (ULTRA-00225). */
class InsightsEnvironmentSpec : StringSpec({

    fun env(name: String) = KtorConfig(deployment = KtorConfig.Deployment(environment = name))

    /** An app config with the one insights switch (ULTRA-00301) set to [enabled]. */
    fun app(environment: String, enabled: Boolean) = AppConfig.of(
        ktor = env(environment),
        funktor = FunktorConfig(insights = InsightsConfig(enabled = enabled)),
    )

    val development = listOf("dev", "test", "qa", "qa-eu", "QA2", "Dev")
    val notDevelopment = listOf("prod", "live", "production", "staging", "prd", "", "\${ENV}", "dev ", "testing", "qatar", "qaprod")

    "only dev, test and qa, qa-<name>, qa<digits> allow insights; every other name counts as production" {
        development.forEach { withClue(it) { env(it).allowsInsights shouldBe true } }
        notDevelopment.forEach { withClue(it) { env(it).allowsInsights shouldBe false } }
    }

    "insights is active only when enabled AND allowed" {
        InsightsConfig(enabled = true).isActiveIn(env("dev")) shouldBe true
        InsightsConfig(enabled = true).isActiveIn(env("prod")) shouldBe false
        InsightsConfig(enabled = false).isActiveIn(env("dev")) shouldBe false
        null.isActiveIn(env("dev")) shouldBe false
    }

    "an enabled config outside development refuses, naming the environment; a disabled one never does" {
        notDevelopment.forEach { name ->
            withClue(name) {
                shouldThrow<AppStartException> {
                    refuseInsightsOutsideDevelopment(InsightsConfig(enabled = true), env(name))
                }.message shouldContain "'$name'"

                refuseInsightsOutsideDevelopment(InsightsConfig(enabled = false), env(name))
                refuseInsightsOutsideDevelopment(null, env(name))
            }
        }

        development.forEach { refuseInsightsOutsideDevelopment(InsightsConfig(enabled = true), env(it)) }
    }

    "an app that instruments its routes with insights enabled does not boot in production" {
        shouldThrow<AppStartException> {
            testApplication {
                application {
                    routing { instrumentWithInsights(app("prod", enabled = true)) }
                }
                startApplication()
            }
        }.message shouldContain "'prod'"

        // The same app in a development environment boots
        testApplication {
            application {
                routing { instrumentWithInsights(app("dev", enabled = true)) }
            }
            startApplication()
        }
    }

    "the kontainer reads the app's one switch: InsightsFull only when it is on in development" {
        fun insightsIn(environment: String, enabled: Boolean) = kontainer {
            funktorInsights()
            instance(AppConfig::class, app(environment, enabled))
            // What funktor core and auth provide in a real app
            instance(QueryProfiler::class, NullQueryProfiler)
            instance(UserProvider::class, UserProvider.anonymous)
        }.create().get(Insights::class)

        // The API asks `isActive` too, so this also decides its 404
        insightsIn("dev", enabled = true).shouldBeInstanceOf<InsightsFull>().isActive shouldBe true
        insightsIn("dev", enabled = false).shouldBeInstanceOf<InsightsDisabled>().isActive shouldBe false
        insightsIn("prod", enabled = true).shouldBeInstanceOf<InsightsDisabled>().isActive shouldBe false
    }

    "a switch that is off instruments nothing and refuses nothing, in any environment" {
        testApplication {
            application {
                routing { instrumentWithInsights(app("prod", enabled = false)) }
            }
            startApplication()
        }
    }

    "the app-wide check refuses the start with the switch on outside development, instrumented routes or not" {
        shouldThrow<AppStartException> { InsightsEnvironmentCheck(app("prod", enabled = true)).check() }
            .message shouldContain "'prod'"

        InsightsEnvironmentCheck(app("prod", enabled = false)).check()
        InsightsEnvironmentCheck(app("dev", enabled = true)).check()
    }

    "the app-wide check runs at start: the insights module registers it" {
        kontainer {
            funktorInsights()
            instance(AppConfig::class, app("dev", enabled = false))
            instance(QueryProfiler::class, NullQueryProfiler)
            instance(UserProvider::class, UserProvider.anonymous)
        }.create().getAll(AppLifeCycleHooks.OnAppStarting::class).map { it::class } shouldContainElement InsightsEnvironmentCheck::class
    }

    "routes are instrumented only while the switch is on" {
        fun instrumented(config: AppConfig): Boolean {
            var root: Routing? = null

            testApplication {
                application {
                    routing {
                        root = this
                        route("/x") { instrumentWithInsights(config) }
                    }
                }
                startApplication()
            }

            return RoutingInstrumentation.Registry.contains(root)
        }

        instrumented(app("dev", enabled = true)) shouldBe true
        instrumented(app("dev", enabled = false)) shouldBe false
    }

    "system properties whose name or value looks secret are redacted; the rest stay" {
        val redacted = RuntimeCollector.redact(
            mapOf(
                "javax.net.ssl.keyStorePassword" to "s1",
                "aws.accessKeyId" to "s2",
                "aws.secretAccessKey" to "s3",
                "db.pwd" to "s4",
                "my.api_key" to "s5",
                "github.token" to "s6",
                "sun.java.command" to "app.MainKt --password=s7",
                "mongodb.connectionString" to "mongodb://root:s8@db:27017",
                "database.url" to "jdbc:postgresql://h/db?user=u&password=s9",
                "java.version" to "21.0.5",
                "user.dir" to "/srv/app",
                "service.url" to "https://api.example.com:8443/v1",
                "plain.mongo" to "mongodb://db:27017",
            )
        )

        redacted.filterValues { it != HeaderLogging.REDACTED }.keys shouldBe
                setOf("java.version", "user.dir", "service.url", "plain.mongo")
        redacted["java.version"] shouldBe "21.0.5"
    }
})
