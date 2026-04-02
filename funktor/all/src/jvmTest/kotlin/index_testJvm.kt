package io.peekandpoke.funktor

import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.testing.AppSpecAware
import io.peekandpoke.funktor.testing.AppUnderTest
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.kontainer

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
        auth = {
            useKarango()
        },
    )

    karango(config = ArangoDbConfig.forUnitTests)

    module(TestUserModule)
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
