package de.peekandpoke.funktor.core.config.ktor

import com.fasterxml.jackson.annotation.JsonIgnore

data class KtorConfig(
    val deployment: Deployment = Deployment(),
    val application: Application = Application(),
    val security: Security? = null,
) {
    data class Deployment(
        val environment: String = "live",
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
        @get:JsonIgnore // prevent this key from being logged
        val keyStorePassword: String? = null,
        @get:JsonIgnore // prevent this key from being logged
        val privateKeyPassword: String? = null,
    )

    val isLocalDev: Boolean get() = deployment.environment.lowercase() == "dev"

    val isTest: Boolean get() = deployment.environment.lowercase() == "test"

    val isQa: Boolean get() = deployment.environment.lowercase().startsWith("qa")

    val isDevelopment: Boolean get() = isLocalDev || isTest || isQa

    val isProduction: Boolean get() = deployment.environment.lowercase() in listOf("live", "production")

    val isNotProduction: Boolean get() = !isProduction
}
