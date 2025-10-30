package io.peekandpoke.funktor.demo.server

import AuthConfig
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.config.funktor.FunktorConfig
import de.peekandpoke.funktor.core.config.ktor.KtorConfig
import de.peekandpoke.funktor.core.model.InsightsConfig
import de.peekandpoke.funktor.messaging.senders.aws.AwsSesConfig
import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.monko.MongoDbConfig

data class FunktorDemoConfig(
    override val ktor: KtorConfig,
    override val funktor: FunktorConfig = FunktorConfig(),
    val auth: AuthConfig,
    val arangodb: ArangoDbConfig,
    val mongodb: MongoDbConfig,
    val api: ApiConfig,
    val aws: AwsConfig,
) : AppConfig {

    data class ApiConfig(
        val baseUrl: String,
        val insights: InsightsConfig = InsightsConfig(),
    )

    data class AwsConfig(
        val ses: AwsSesConfig,
    )
}
