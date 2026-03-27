package io.peekandpoke.funktor.logging

import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class LogsStorageKarangoSpec : LogsStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        karango(ArangoDbConfig.forUnitTests)
    }

    override fun FunktorLoggingBuilder.configureLogging() {
        useKarango()
    }
}
