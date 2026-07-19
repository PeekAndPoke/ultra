package io.peekandpoke.funktor.rest

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig

// Guards the decision that controls whether a 500 response carries a full stack trace.
// Getting this wrong leaks internal paths, class names and query fragments to any caller.
class ApiStatusPagesSpec : StringSpec({

    fun configFor(environment: String) = AppConfig.of(
        ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = environment))
    )

    "an unknown environment must be treated as production" {
        // No kontainer or no AppConfig on the call. Failing open here would leak stack traces
        // exactly when the environment could not be determined.
        ApiStatusPages.exposesStackTraces(null) shouldBe false
    }

    "production environments must never expose stack traces" {
        listOf("live", "prod", "production", "PROD", "Live").forEach { env ->
            withClue("environment '$env' must not expose stack traces") {
                ApiStatusPages.exposesStackTraces(configFor(env)) shouldBe false
            }
        }
    }

    "the default configuration must not expose stack traces" {
        // KtorConfig.Deployment.environment defaults to "prod"
        ApiStatusPages.exposesStackTraces(AppConfig.of()) shouldBe false
    }

    "the empty configuration must not expose stack traces" {
        ApiStatusPages.exposesStackTraces(AppConfig.empty) shouldBe false
    }

    "non-production environments may expose stack traces" {
        listOf("dev", "test", "qa", "qa-eu").forEach { env ->
            withClue("environment '$env' may expose stack traces") {
                ApiStatusPages.exposesStackTraces(configFor(env)) shouldBe true
            }
        }
    }

    "environments that merely look like production must not be treated as production" {
        listOf("staging", "preprod", "prod-clone", "production-mirror").forEach { env ->
            withClue("environment '$env' is not production, so stack traces are allowed") {
                ApiStatusPages.exposesStackTraces(configFor(env)) shouldBe true
            }
        }
    }
})
