package io.peekandpoke.funktor.insights

import impl.InsightsDisabled
import impl.InsightsFull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
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
                    routing { instrumentWithInsights(InsightsConfig(enabled = true), env("prod")) }
                }
                startApplication()
            }
        }.message shouldContain "'prod'"

        // The same app in a development environment boots
        testApplication {
            application {
                routing { instrumentWithInsights(InsightsConfig(enabled = true), env("dev")) }
            }
            startApplication()
        }
    }

    "the kontainer hands out InsightsDisabled for an enabled config in production, InsightsFull in development" {
        fun insightsIn(environment: String) = kontainer {
            funktorInsights()
            instance(AppConfig::class, AppConfig.of(ktor = env(environment)))
            // What funktor core and auth provide in a real app
            instance(QueryProfiler::class, NullQueryProfiler)
            instance(UserProvider::class, UserProvider.anonymous)
        }.create {
            with { InsightsConfig(enabled = true) }
        }.get(Insights::class)

        // The API asks `isActive` too, so this also decides its 404
        insightsIn("prod").shouldBeInstanceOf<InsightsDisabled>().isActive shouldBe false
        insightsIn("dev").shouldBeInstanceOf<InsightsFull>().isActive shouldBe true
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
