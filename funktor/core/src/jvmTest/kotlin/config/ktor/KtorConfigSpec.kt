package io.peekandpoke.funktor.core.config.ktor

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

// The environment classification decides whether error responses expose full stack traces
// (see ApiStatusPages.withCause), so a mis-classified environment leaks internals to clients.
class KtorConfigSpec : StringSpec({

    fun configFor(environment: String) = KtorConfig(
        deployment = KtorConfig.Deployment(environment = environment)
    )

    "production environments must be recognised, in any casing" {
        listOf("live", "prod", "production", "Prod", "PROD", "Production", "LIVE").forEach { env ->
            withClue("environment '$env' must be production") {
                configFor(env).isProduction shouldBe true
                configFor(env).isNotProduction shouldBe false
            }
        }
    }

    "the default environment must be treated as production" {
        // The default is "prod" — if that were not classified as production, a default-configured
        // app would serve full stack traces to clients
        KtorConfig().isProduction shouldBe true
    }

    "non-production environments must not be treated as production" {
        listOf("dev", "test", "qa", "qa-2", "staging", "").forEach { env ->
            withClue("environment '$env' must not be production") {
                configFor(env).isProduction shouldBe false
            }
        }
    }

    "environments must not be classified as production by prefix or substring" {
        listOf("production-mirror", "preprod", "prod-clone", "not-live").forEach { env ->
            withClue("environment '$env' is a separate environment, not production") {
                configFor(env).isProduction shouldBe false
            }
        }
    }

    "development environments must be recognised" {
        listOf("dev", "DEV", "test", "qa", "qa-eu").forEach { env ->
            withClue("environment '$env' must be development") {
                configFor(env).isDevelopment shouldBe true
            }
        }
    }

    "production must never also count as development" {
        listOf("live", "prod", "production").forEach { env ->
            withClue("environment '$env' must not be development") {
                val config = configFor(env)

                config.isDevelopment shouldBe false
                config.isLocalDev shouldBe false
                config.isTest shouldBe false
                config.isQa shouldBe false
            }
        }
    }
})
