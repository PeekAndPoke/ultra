package io.peekandpoke.funktor.logging

import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.KontainerBuilder

class LogsStorageMonkoSpec : LogsStorageBaseSpec() {

    override fun KontainerBuilder.configureKontainer() {
        monko(MongoDbConfig.forUnitTests)
    }

    override fun FunktorLoggingBuilder.configureLogging() {
        useMonko()
    }
}
