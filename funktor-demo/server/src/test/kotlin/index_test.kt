package io.peekandpoke.funktor.demo.server

import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.testing.AppSpecAware
import io.peekandpoke.funktor.testing.AppUnderTest

val testApp = funktorApp<FunktorDemoConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config)
        )
    },
)


inline fun <C : AppConfig> AppSpecAware<C>.apiApp(block: AppUnderTest<C>.() -> Unit) =
    testApp(host = "api.funktor-demo.local").block()
