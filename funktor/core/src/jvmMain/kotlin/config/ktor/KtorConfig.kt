package io.peekandpoke.funktor.core.config.ktor

import io.peekandpoke.ultra.common.model.Redacted


/** Typed representation of the ktor configuration block (deployment, application, security). */
data class KtorConfig(
    val deployment: Deployment = Deployment(),
    val application: Application = Application(),
    val security: Security? = null,
) {
    data class Deployment(
        val environment: String = "prod",
        val host: String = "0.0.0.0",
        val port: Int = 80,
        val sslPort: Int = 8443,
        val autoreload: Boolean = false,
        val watch: List<String> = listOf(),
    )

    data class Application(
        val id: String = "Application",
        val modules: List<String> = listOf(),
    )

    data class Security(
        val keyStore: String? = null,
        val keyAlias: String? = null,
        val keyStorePassword: Redacted<String>? = null,
        val privateKeyPassword: Redacted<String>? = null,
    )

    val isLocalDev: Boolean get() = deployment.environment.lowercase() == "dev"

    val isTest: Boolean get() = deployment.environment.lowercase() == "test"

    /** `qa`, `qa-<name>` or `qa<digits>`, any case. Not a bare prefix: `qatar` or `qaprod` are not QA. */
    val isQa: Boolean get() = qaEnvironment.matches(deployment.environment.lowercase())

    val isDevelopment: Boolean get() = isLocalDev || isTest || isQa

    val isProduction: Boolean get() = deployment.environment.lowercase() in listOf("live", "prod", "production")

    val isNotProduction: Boolean get() = !isProduction
}

/** Top level, not a companion: Slumber reflects over [KtorConfig] and cannot read a private companion. */
private val qaEnvironment = Regex("qa(-[a-z0-9-]+|[0-9]+)?")
